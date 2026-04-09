package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentDetailAssembler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DETAIL_TYPE_ENTERPRISE = "ENTERPRISE_TRANSACTION";
    private static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    private static final String ENTERPRISE_MODE_INVOICE_FULL_PAYMENT = "INVOICE_FULL_PAYMENT";

    private final ObjectMapper objectMapper;

    public ExpenseDetailInstanceDetailVO toExpenseDetailDetailVO(ProcessDocumentExpenseDetail detail) {
        ExpenseDetailInstanceDetailVO vo = new ExpenseDetailInstanceDetailVO();
        vo.setDocumentCode(detail.getDocumentCode());
        vo.setDetailNo(detail.getDetailNo());
        vo.setDetailDesignCode(detail.getDetailDesignCode());
        vo.setDetailType(detail.getDetailType());
        vo.setDetailTypeLabel(resolveExpenseDetailTypeLabel(detail.getDetailType()));
        vo.setEnterpriseMode(detail.getEnterpriseMode());
        vo.setEnterpriseModeLabel(resolveEnterpriseModeLabel(detail.getEnterpriseMode()));
        vo.setExpenseTypeCode(detail.getExpenseTypeCode());
        vo.setBusinessSceneMode(detail.getBusinessSceneMode());
        vo.setDetailTitle(detail.getDetailTitle());
        vo.setSortOrder(detail.getSortOrder());
        vo.setSchemaSnapshot(readMap(detail.getSchemaSnapshotJson()));
        vo.setFormData(readMap(detail.getFormDataJson()));
        vo.setCreatedAt(formatTime(detail.getCreatedAt()));
        vo.setUpdatedAt(formatTime(detail.getUpdatedAt()));
        return vo;
    }

    private String resolveExpenseDetailTypeLabel(String detailType) {
        return Objects.equals(trimToNull(detailType), DETAIL_TYPE_ENTERPRISE) ? "\u4f01\u4e1a\u5f80\u6765" : "\u666e\u901a\u62a5\u9500";
    }

    private String resolveEnterpriseModeLabel(String enterpriseMode) {
        if (Objects.equals(trimToNull(enterpriseMode), ENTERPRISE_MODE_PREPAY_UNBILLED)) {
            return "\u9884\u4ed8\u672a\u5230\u7968";
        }
        if (Objects.equals(trimToNull(enterpriseMode), ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return "\u5230\u7968\u5168\u989d\u652f\u4ed8";
        }
        return "";
    }

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
