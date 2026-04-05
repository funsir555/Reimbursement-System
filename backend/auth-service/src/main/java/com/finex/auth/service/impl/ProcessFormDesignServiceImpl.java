package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessFormDesignMapper;
import com.finex.auth.service.ProcessFormDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcessFormDesignServiceImpl implements ProcessFormDesignService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProcessFormDesignMapper processFormDesignMapper;
    private final ProcessDocumentTemplateMapper processDocumentTemplateMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType) {
        return processFormDesignMapper.selectList(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .eq(trimToNull(templateType) != null, ProcessFormDesign::getTemplateType, trimToNull(templateType))
                        .orderByDesc(ProcessFormDesign::getUpdatedAt, ProcessFormDesign::getId)
        ).stream().map(this::toSummary).toList();
    }

    @Override
    public ProcessFormDesignDetailVO getFormDesignDetail(Long id) {
        return toDetail(requireFormDesign(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto) {
        validateSave(dto, null);

        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setFormCode(buildFormCode());
        applyBase(formDesign, dto);
        processFormDesignMapper.insert(formDesign);
        return toDetail(requireFormDesign(formDesign.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto) {
        ProcessFormDesign existing = requireFormDesign(id);
        validateSave(dto, existing);
        applyBase(existing, dto);
        processFormDesignMapper.updateById(existing);
        return toDetail(requireFormDesign(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFormDesign(Long id) {
        ProcessFormDesign formDesign = requireFormDesign(id);
        Long referencedCount = processDocumentTemplateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getFormDesignCode, formDesign.getFormCode())
        );
        if (referencedCount != null && referencedCount > 0) {
            throw new IllegalStateException("当前表单设计已被模板引用，不能删除");
        }
        processFormDesignMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    public List<ProcessFormOptionVO> listFormDesignOptions(String templateType) {
        return listFormDesigns(templateType).stream()
                .map(item -> option(item.getFormName(), item.getFormCode()))
                .toList();
    }

    @Override
    public Map<String, String> formDesignLabelMap(String templateType) {
        return listFormDesignOptions(templateType).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getValue(), item.getLabel()), Map::putAll);
    }

    @Override
    public String resolveFormDesignCode(String formCode, String templateType) {
        String normalizedCode = trimToNull(formCode);
        if (normalizedCode == null) {
            List<ProcessFormOptionVO> options = listFormDesignOptions(templateType);
            if (options.isEmpty()) {
                throw new IllegalArgumentException("璇峰厛閫夋嫨琛ㄥ崟璁捐");
            }
            return options.get(0).getValue();
        }

        ProcessFormDesign formDesign = processFormDesignMapper.selectOne(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .eq(ProcessFormDesign::getFormCode, normalizedCode)
                        .last("limit 1")
        );
        if (formDesign == null) {
            throw new IllegalArgumentException("鎵€閫夎〃鍗曡璁′笉瀛樺湪");
        }
        if (!Objects.equals(normalizeTemplateType(templateType), normalizeTemplateType(formDesign.getTemplateType()))) {
            throw new IllegalArgumentException("鎵€閫夎〃鍗曡璁′笉灞炰簬褰撳墠妯℃澘绫诲瀷");
        }
        return normalizedCode;
    }

    private ProcessFormDesignSummaryVO toSummary(ProcessFormDesign formDesign) {
        ProcessFormDesignSummaryVO summary = new ProcessFormDesignSummaryVO();
        summary.setId(formDesign.getId());
        summary.setFormCode(formDesign.getFormCode());
        summary.setFormName(formDesign.getFormName());
        summary.setTemplateType(formDesign.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(formDesign.getTemplateType()));
        summary.setFormDescription(formDesign.getFormDescription());
        summary.setUpdatedAt(formatTime(formDesign.getUpdatedAt()));
        return summary;
    }

    private ProcessFormDesignDetailVO toDetail(ProcessFormDesign formDesign) {
        ProcessFormDesignDetailVO detail = new ProcessFormDesignDetailVO();
        detail.setId(formDesign.getId());
        detail.setFormCode(formDesign.getFormCode());
        detail.setFormName(formDesign.getFormName());
        detail.setTemplateType(formDesign.getTemplateType());
        detail.setTemplateTypeLabel(resolveTemplateTypeLabel(formDesign.getTemplateType()));
        detail.setFormDescription(formDesign.getFormDescription());
        detail.setSchema(readSchema(formDesign.getSchemaJson()));
        detail.setUpdatedAt(formatTime(formDesign.getUpdatedAt()));
        return detail;
    }

    private void applyBase(ProcessFormDesign target, ProcessFormDesignSaveDTO dto) {
        target.setTemplateType(normalizeTemplateType(dto.getTemplateType()));
        target.setFormName(dto.getFormName().trim());
        target.setFormDescription(trimToNull(dto.getFormDescription()));
        target.setSchemaJson(writeSchema(dto.getSchema()));
    }

    private void validateSave(ProcessFormDesignSaveDTO dto, ProcessFormDesign existing) {
        if (trimToNull(dto.getFormName()) == null) {
            throw new IllegalArgumentException("琛ㄥ崟鍚嶇О涓嶈兘涓虹┖");
        }
        if (trimToNull(dto.getTemplateType()) == null) {
            throw new IllegalArgumentException("琛ㄥ崟绫诲瀷涓嶈兘涓虹┖");
        }
        if (existing != null && isFormDesignReferenced(existing.getFormCode())
                && !Objects.equals(normalizeTemplateType(existing.getTemplateType()), normalizeTemplateType(dto.getTemplateType()))) {
            throw new IllegalStateException("当前表单设计已被模板引用，不能修改模板类型");
        }
    }

    private boolean isFormDesignReferenced(String formCode) {
        Long count = processDocumentTemplateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getFormDesignCode, formCode)
        );
        return count != null && count > 0;
    }

    private ProcessFormDesign requireFormDesign(Long id) {
        ProcessFormDesign formDesign = processFormDesignMapper.selectById(id);
        if (formDesign == null) {
            throw new IllegalStateException("表单设计不存在");
        }
        return formDesign;
    }

    private String buildFormCode() {
        String prefix = "FD" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processFormDesignMapper.selectCount(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .likeRight(ProcessFormDesign::getFormCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (Exception ex) {
            throw new IllegalStateException("鍙嶅簭鍒楀寲琛ㄥ崟璁捐澶辫触", ex);
        }
    }

    private String writeSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema == null || schema.isEmpty() ? defaultSchema() : schema);
        } catch (Exception ex) {
            throw new IllegalStateException("序列化表单设计失败", ex);
        }
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    private String normalizeTemplateType(String templateType) {
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "application";
            case "loan" -> "loan";
            case "contract" -> "contract";
            default -> "report";
        };
    }

    private String resolveTemplateTypeLabel(String templateType) {
        return switch (normalizeTemplateType(templateType)) {
            case "application" -> "Application";
            case "loan" -> "Loan";
            case "contract" -> "合同单";
            default -> "Expense";
        };
    }

    private String formatTime(java.time.LocalDateTime value) {
        return value == null ? "" : value.format(TIME_FORMATTER);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }
}
