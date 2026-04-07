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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcessExpenseDetailDesignServiceImpl implements ProcessExpenseDetailDesignService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
            throw new IllegalStateException("褰撳墠璐圭敤鏄庣粏琛ㄥ崟宸茶妯℃澘寮曠敤锛屼笉鑳藉垹闄?");
        }
        processExpenseDetailDesignMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, String> detailDesignLabelMap() {
        return listExpenseDetailDesigns().stream().collect(
                java.util.LinkedHashMap::new,
                (map, item) -> map.put(item.getDetailCode(), item.getDetailName()),
                Map::putAll
        );
    }

    @Override
    public String resolveExpenseDetailDesignCode(String detailCode) {
        String normalizedCode = trimToNull(detailCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("璐圭敤鏄庣粏琛ㄥ崟涓嶈兘涓虹┖");
        }

        ProcessExpenseDetailDesign detailDesign = processExpenseDetailDesignMapper.selectOne(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .eq(ProcessExpenseDetailDesign::getDetailCode, normalizedCode)
                        .last("limit 1")
        );
        if (detailDesign == null) {
            throw new IllegalArgumentException("璐圭敤鏄庣粏琛ㄥ崟涓嶅瓨鍦?");
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
            throw new IllegalArgumentException("璐圭敤鏄庣粏琛ㄥ崟鍚嶇О涓嶈兘涓虹┖");
        }
        if (trimToNull(dto.getDetailType()) == null) {
            throw new IllegalArgumentException("璐圭敤鏄庣粏琛ㄥ崟绫诲瀷涓嶈兘涓虹┖");
        }
        String normalizedType = normalizeDetailType(dto.getDetailType());
        if (!Objects.equals(normalizedType, ExpenseDetailSystemFieldSupport.DETAIL_TYPE_NORMAL)
                && !Objects.equals(normalizedType, ExpenseDetailSystemFieldSupport.DETAIL_TYPE_ENTERPRISE)) {
            throw new IllegalArgumentException("璐圭敤鏄庣粏琛ㄥ崟绫诲瀷涓嶅悎娉?");
        }
        if (existing != null && isDetailDesignReferenced(existing.getDetailCode())
                && !Objects.equals(normalizeDetailType(existing.getDetailType()), normalizedType)) {
            throw new IllegalStateException("褰撳墠璐圭敤鏄庣粏琛ㄥ崟宸茶妯℃澘寮曠敤锛屼笉鑳戒慨鏀圭被鍨?");
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
            throw new IllegalStateException("璐圭敤鏄庣粏琛ㄥ崟涓嶅瓨鍦?");
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
                ? "浼佷笟寰€鏉?"
                : "鏅€氭姤閿€";
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
}
