// 业务域：报销单录入、流转与查询
// 文件角色：数据组装类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

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

/**
 * ExpenseDocumentDetailAssembler：数据组装类。
 * 把多组数据拼成 报销单单据明细需要的输出结果。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseDocumentDetailAssembler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String DETAIL_TYPE_ENTERPRISE = "ENTERPRISE_TRANSACTION";
    private static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    private static final String ENTERPRISE_MODE_INVOICE_FULL_PAYMENT = "INVOICE_FULL_PAYMENT";

    private final ObjectMapper objectMapper;

    /**
     * 处理报销单单据明细中的这一步。
     */
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

    /**
     * 解析报销单明细类型Label。
     */
    private String resolveExpenseDetailTypeLabel(String detailType) {
        return Objects.equals(trimToNull(detailType), DETAIL_TYPE_ENTERPRISE) ? "\u4f01\u4e1a\u5f80\u6765" : "\u666e\u901a\u62a5\u9500";
    }

    /**
     * 解析EnterpriseModeLabel。
     */
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
