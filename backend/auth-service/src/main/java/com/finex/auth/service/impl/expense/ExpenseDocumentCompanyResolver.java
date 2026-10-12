package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemCompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves the company owning a document from its saved form, then its submitter. */
@Service
@RequiredArgsConstructor
class ExpenseDocumentCompanyResolver {
    private static final String PAYMENT_COMPANY = "payment-company";

    private final SystemCompanyMapper companyMapper;

    String resolve(Map<String, Object> schema, Map<String, Object> formData, User submitter) {
        String paymentCompany = null;
        Object rawBlocks = schema == null ? null : schema.get("blocks");
        if (rawBlocks instanceof List<?> blocks) {
            for (Object rawBlock : blocks) {
                if (!(rawBlock instanceof Map<?, ?> block)
                        || !Objects.equals("BUSINESS_COMPONENT", block.get("kind"))
                        || !(block.get("props") instanceof Map<?, ?> props)
                        || !Objects.equals(PAYMENT_COMPANY, props.get("componentCode"))) {
                    continue;
                }
                String fieldKey = text(block.get("fieldKey"));
                String candidate = fieldKey == null || formData == null
                        ? null : companyValue(formData.get(fieldKey));
                if (candidate == null) continue;
                if (paymentCompany != null) {
                    throw new IllegalArgumentException("付款公司配置不唯一");
                }
                paymentCompany = candidate;
            }
        }
        String companyId = paymentCompany != null ? paymentCompany
                : text(submitter == null ? null : submitter.getCompanyId());
        if (companyId == null) {
            throw new IllegalArgumentException("单据缺少有效公司归属");
        }
        SystemCompany company = companyMapper.selectById(companyId);
        if (company == null || !Integer.valueOf(1).equals(company.getStatus())) {
            throw new IllegalArgumentException("单据公司不存在或已停用");
        }
        return companyId;
    }

    private String companyValue(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Map<?, ?> value) {
            String normalized = text(value.get("value"));
            if (normalized == null) normalized = text(value.get("code"));
            if (normalized == null) normalized = text(value.get("id"));
            if (normalized == null && !value.isEmpty()) {
                throw new IllegalArgumentException("付款公司值缺少公司 ID");
            }
            return normalized;
        }
        if (raw instanceof List<?> values) {
            String result = null;
            for (Object value : values) {
                String candidate = companyValue(value);
                if (candidate == null) continue;
                if (result != null) throw new IllegalArgumentException("付款公司值不唯一");
                result = candidate;
            }
            return result;
        }
        if (!(raw instanceof String)) throw new IllegalArgumentException("付款公司值无效");
        return text(raw);
    }

    private String text(Object value) {
        if (value == null) return null;
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
