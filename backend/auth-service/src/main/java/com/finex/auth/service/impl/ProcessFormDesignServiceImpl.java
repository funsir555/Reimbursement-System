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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessFormDesignServiceImpl implements ProcessFormDesignService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PM_NAME_MAX_LENGTH = 64;
    private static final int PM_FIELD_KEY_MAX_LENGTH = 64;

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
            throw new IllegalStateException("\u5f53\u524d\u8868\u5355\u8bbe\u8ba1\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
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
                throw new IllegalArgumentException("\u5f53\u524d\u6a21\u677f\u7c7b\u578b\u4e0b\u6ca1\u6709\u53ef\u7528\u7684\u8868\u5355\u8bbe\u8ba1");
            }
            return options.get(0).getValue();
        }

        ProcessFormDesign formDesign = processFormDesignMapper.selectOne(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .eq(ProcessFormDesign::getFormCode, normalizedCode)
                        .last("limit 1")
        );
        if (formDesign == null) {
            throw new IllegalArgumentException("\u8868\u5355\u8bbe\u8ba1\u4e0d\u5b58\u5728");
        }
        if (!Objects.equals(normalizeTemplateType(templateType), normalizeTemplateType(formDesign.getTemplateType()))) {
            throw new IllegalArgumentException("\u6240\u9009\u8868\u5355\u8bbe\u8ba1\u4e0e\u5f53\u524d\u6a21\u677f\u7c7b\u578b\u4e0d\u5339\u914d");
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
            throw new IllegalArgumentException("\u8868\u5355\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        validatePmNameLength(dto.getFormName(), "\u8868\u5355\u540d\u79f0");
        if (trimToNull(dto.getTemplateType()) == null) {
            throw new IllegalArgumentException("\u8868\u5355\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }
        validateSchemaFieldKeys(dto.getSchema(), "\u8868\u5355");
        if (existing != null && isFormDesignReferenced(existing.getFormCode())
                && !Objects.equals(normalizeTemplateType(existing.getTemplateType()), normalizeTemplateType(dto.getTemplateType()))) {
            throw new IllegalStateException("\u5f53\u524d\u8868\u5355\u8bbe\u8ba1\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u4fee\u6539\u6a21\u677f\u7c7b\u578b");
        }
    }

    private void validateSchemaFieldKeys(Map<String, Object> schema, String subjectLabel) {
        Object rawBlocks = schema == null ? null : schema.get("blocks");
        if (!(rawBlocks instanceof Collection<?> blocks)) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        int index = 0;
        for (Object rawBlock : blocks) {
            index++;
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            String fieldKey = trimToNull(stringValue(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                throw new IllegalArgumentException(subjectLabel + "\u7b2c " + index + " \u4e2a\u5b57\u6bb5\u6807\u8bc6\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (fieldKey.length() > PM_FIELD_KEY_MAX_LENGTH) {
                throw new IllegalArgumentException("\u5b57\u6bb5\u6807\u8bc6 " + fieldKey + " \u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
            }
            if (!seen.add(fieldKey)) {
                throw new IllegalArgumentException(subjectLabel + "\u5b57\u6bb5\u6807\u8bc6 " + fieldKey + " \u4e0d\u80fd\u91cd\u590d");
            }
        }
    }

    private void validatePmNameLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
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
            throw new IllegalStateException("\u8868\u5355\u8bbe\u8ba1\u4e0d\u5b58\u5728");
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
            throw new IllegalStateException("\u8bfb\u53d6\u8868\u5355\u8bbe\u8ba1\u5931\u8d25", ex);
        }
    }

    private String writeSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema == null || schema.isEmpty() ? defaultSchema() : schema);
        } catch (Exception ex) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u8868\u5355\u8bbe\u8ba1\u5931\u8d25", ex);
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
            case "contract" -> "\u5408\u540c\u5355";
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }
}
