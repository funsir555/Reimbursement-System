package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseDetailDesignMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessExpenseDetailDesignServiceImpl implements ProcessExpenseDetailDesignService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PM_NAME_MAX_LENGTH = 64;
    private static final int PM_FIELD_KEY_MAX_LENGTH = 64;

    private final ProcessExpenseDetailDesignMapper processExpenseDetailDesignMapper;
    private final ProcessDocumentTemplateMapper processDocumentTemplateMapper;
    private final ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;

    @Override
    public List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns() {
        return processExpenseDetailDesignMapper.selectList(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .orderByDesc(ProcessExpenseDetailDesign::getUpdatedAt, ProcessExpenseDetailDesign::getId)
        ).stream().map(this::toSummary).toList();
    }

    @Override
    public ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id) {
        return toDetail(requireExpenseDetailDesign(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto) {
        validateSave(dto, null);

        ProcessExpenseDetailDesign detailDesign = new ProcessExpenseDetailDesign();
        detailDesign.setDetailCode(buildDetailCode());
        applyBase(detailDesign, dto);
        processExpenseDetailDesignMapper.insert(detailDesign);
        return toDetail(requireExpenseDetailDesign(detailDesign.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto) {
        ProcessExpenseDetailDesign existing = requireExpenseDetailDesign(id);
        validateSave(dto, existing);
        applyBase(existing, dto);
        processExpenseDetailDesignMapper.updateById(existing);
        return toDetail(requireExpenseDetailDesign(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseDetailDesign(Long id) {
        ProcessExpenseDetailDesign detailDesign = requireExpenseDetailDesign(id);
        Long referencedCount = processDocumentTemplateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getExpenseDetailDesignCode, detailDesign.getDetailCode())
        );
        if (referencedCount != null && referencedCount > 0) {
            throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u660e\u7ec6\u8868\u5355\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        processExpenseDetailDesignMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, String> detailDesignLabelMap() {
        return listExpenseDetailDesigns().stream().collect(
                LinkedHashMap::new,
                (map, item) -> map.put(item.getDetailCode(), item.getDetailName()),
                Map::putAll
        );
    }

    @Override
    public String resolveExpenseDetailDesignCode(String detailCode) {
        String normalizedCode = trimToNull(detailCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u8868\u5355\u4e0d\u80fd\u4e3a\u7a7a");
        }

        ProcessExpenseDetailDesign detailDesign = processExpenseDetailDesignMapper.selectOne(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .eq(ProcessExpenseDetailDesign::getDetailCode, normalizedCode)
                        .last("limit 1")
        );
        if (detailDesign == null) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u8868\u5355\u4e0d\u5b58\u5728");
        }
        return detailDesign.getDetailCode();
    }

    @Override
    public String resolveExpenseDetailType(String detailCode) {
        String normalizedCode = resolveExpenseDetailDesignCode(detailCode);
        ProcessExpenseDetailDesign detailDesign = processExpenseDetailDesignMapper.selectOne(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .eq(ProcessExpenseDetailDesign::getDetailCode, normalizedCode)
                        .last("limit 1")
        );
        return detailDesign == null
                ? ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL
                : normalizeDetailType(detailDesign.getDetailType());
    }

    private ProcessExpenseDetailDesignSummaryVO toSummary(ProcessExpenseDetailDesign detailDesign) {
        ProcessExpenseDetailDesignSummaryVO summary = new ProcessExpenseDetailDesignSummaryVO();
        summary.setId(detailDesign.getId());
        summary.setDetailCode(detailDesign.getDetailCode());
        summary.setDetailName(detailDesign.getDetailName());
        summary.setDetailType(normalizeDetailType(detailDesign.getDetailType()));
        summary.setDetailTypeLabel(resolveDetailTypeLabel(detailDesign.getDetailType()));
        summary.setDetailDescription(detailDesign.getDetailDescription());
        summary.setUpdatedAt(formatTime(detailDesign.getUpdatedAt()));
        return summary;
    }

    private ProcessExpenseDetailDesignDetailVO toDetail(ProcessExpenseDetailDesign detailDesign) {
        ProcessExpenseDetailDesignDetailVO detail = new ProcessExpenseDetailDesignDetailVO();
        detail.setId(detailDesign.getId());
        detail.setDetailCode(detailDesign.getDetailCode());
        detail.setDetailName(detailDesign.getDetailName());
        detail.setDetailType(normalizeDetailType(detailDesign.getDetailType()));
        detail.setDetailTypeLabel(resolveDetailTypeLabel(detailDesign.getDetailType()));
        detail.setDetailDescription(detailDesign.getDetailDescription());
        detail.setSchema(expenseDetailSystemFieldSupport.readSchema(detailDesign.getSchemaJson(), detailDesign.getDetailType()));
        detail.setUpdatedAt(formatTime(detailDesign.getUpdatedAt()));
        return detail;
    }

    private void applyBase(ProcessExpenseDetailDesign target, ProcessExpenseDetailDesignSaveDTO dto) {
        String normalizedDetailType = normalizeDetailType(dto.getDetailType());
        target.setDetailName(dto.getDetailName().trim());
        target.setDetailType(normalizedDetailType);
        target.setDetailDescription(trimToNull(dto.getDetailDescription()));
        target.setSchemaJson(expenseDetailSystemFieldSupport.writeSchema(dto.getSchema(), normalizedDetailType));
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(LocalDateTime.now());
        }
        target.setUpdatedAt(LocalDateTime.now());
    }

    private void validateSave(ProcessExpenseDetailDesignSaveDTO dto, ProcessExpenseDetailDesign existing) {
        if (trimToNull(dto.getDetailName()) == null) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u8868\u5355\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        validatePmNameLength(dto.getDetailName(), "\u8d39\u7528\u660e\u7ec6\u8868\u5355\u540d\u79f0");
        if (trimToNull(dto.getDetailType()) == null) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String normalizedType = normalizeDetailType(dto.getDetailType());
        if (!Objects.equals(normalizedType, ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL)
                && !Objects.equals(normalizedType, ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE)) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u7c7b\u578b\u4e0d\u5408\u6cd5");
        }
        validateSchemaFieldKeys(
                expenseDetailSystemFieldSupport.normalizeSchema(dto.getSchema(), normalizedType),
                "\u8d39\u7528\u660e\u7ec6\u8868\u5355"
        );
        if (existing != null
                && isDetailDesignReferenced(existing.getDetailCode())
                && !Objects.equals(normalizeDetailType(existing.getDetailType()), normalizedType)) {
            throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u660e\u7ec6\u8868\u5355\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u4fee\u6539\u660e\u7ec6\u7c7b\u578b");
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

    private boolean isDetailDesignReferenced(String detailCode) {
        Long count = processDocumentTemplateMapper.selectCount(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getExpenseDetailDesignCode, detailCode)
        );
        return count != null && count > 0;
    }

    private ProcessExpenseDetailDesign requireExpenseDetailDesign(Long id) {
        ProcessExpenseDetailDesign detailDesign = processExpenseDetailDesignMapper.selectById(id);
        if (detailDesign == null) {
            throw new IllegalStateException("\u8d39\u7528\u660e\u7ec6\u8868\u5355\u4e0d\u5b58\u5728");
        }
        return detailDesign;
    }

    private String buildDetailCode() {
        String prefix = "EDD" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processExpenseDetailDesignMapper.selectCount(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .likeRight(ProcessExpenseDetailDesign::getDetailCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String normalizeDetailType(String detailType) {
        return expenseDetailSystemFieldSupport.normalizeDetailType(detailType);
    }

    private String resolveDetailTypeLabel(String detailType) {
        return Objects.equals(normalizeDetailType(detailType), ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE)
                ? "\u4f01\u4e1a\u5f80\u6765"
                : "\u666e\u901a\u62a5\u9500";
    }

    private String formatTime(LocalDateTime value) {
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
}
