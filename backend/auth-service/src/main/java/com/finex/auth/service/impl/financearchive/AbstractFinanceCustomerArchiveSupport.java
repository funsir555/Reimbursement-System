package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class AbstractFinanceCustomerArchiveSupport {

    protected static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected final FinanceCustomerMapper financeCustomerMapper;
    protected final ObjectMapper objectMapper;

    protected AbstractFinanceCustomerArchiveSupport(
            FinanceCustomerMapper financeCustomerMapper,
            ObjectMapper objectMapper
    ) {
        this.financeCustomerMapper = financeCustomerMapper;
        this.objectMapper = objectMapper;
    }

    protected void validateSave(FinanceCustomerSaveDTO dto, FinanceCustomer existing) {
        if (dto == null) {
            throw new IllegalArgumentException("\u5ba2\u6237\u4fdd\u5b58\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (trimToNull(dto.getCCusName()) == null) {
            throw new IllegalArgumentException("\u5ba2\u6237\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }

        validateLength(dto, "cCusCode", "\u5ba2\u6237\u7f16\u7801", 64);
        validateLength(dto, "cCusName", "\u5ba2\u6237\u540d\u79f0", 128);
        validateLength(dto, "cCusAbbName", "\u5ba2\u6237\u7b80\u79f0", 64);
        validateLength(dto, "cCCCode", "\u5206\u7c7b\u7f16\u7801", 64);
        validateLength(dto, "cDCCode", "\u5730\u533a\u7f16\u7801", 64);
        validateLength(dto, "cCusTradeCCode", "\u884c\u4e1a\u7f16\u7801", 64);
        validateLength(dto, "cCusPostCode", "\u90ae\u653f\u7f16\u7801", 16);
        validateLength(dto, "cCusBank", "\u5f00\u6237\u94f6\u884c", 128);
        validateLength(dto, "cCusAccount", "\u94f6\u884c\u8d26\u53f7", 64);
        validateLength(dto, "cCusLPerson", "\u6cd5\u4eba", 64);
        validateLength(dto, "cCusPerson", "\u8054\u7cfb\u4eba", 64);
        validateLength(dto, "cCusHand", "\u624b\u673a", 32);
        validateLength(dto, "cCusHeadCode", "\u5ba2\u6237\u603b\u516c\u53f8\u7f16\u7801", 64);
        validateLength(dto, "cCusWhCode", "\u53d1\u8d27\u4ed3\u5e93", 64);
        validateLength(dto, "cCusDepart", "\u5206\u7ba1\u90e8\u95e8", 64);
        validateLength(dto, "cCusBankCode", "\u6240\u5c5e\u94f6\u884c\u7f16\u7801", 64);
        validateLength(dto, "customerKCode", "\u5ba2\u6237\u7ea7\u522b\u7f16\u7801", 64);

        if (existing == null && trimToNull(dto.getCCusCode()) != null) {
            QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
            query.eq("cCusCode", dto.getCCusCode().trim()).last("limit 1");
            if (financeCustomerMapper.selectOne(query) != null) {
                throw new IllegalStateException("\u5ba2\u6237\u7f16\u7801\u5df2\u5b58\u5728");
            }
        }
    }

    protected FinanceCustomer requireCustomer(String companyId, String customerCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(customerCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("\u5ba2\u6237\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        FinanceCustomer customer = financeCustomerMapper.selectById(normalizedCode);
        if (customer == null) {
            throw new IllegalStateException("\u5ba2\u6237\u4e0d\u5b58\u5728");
        }
        if (!normalizedCompanyId.equals(trimToNull(customer.getCompanyId()))) {
            throw new SecurityException("\u5ba2\u6237\u4e0d\u5c5e\u4e8e\u5f53\u524d\u516c\u53f8");
        }
        return customer;
    }

    protected FinanceCustomerSummaryVO toSummary(FinanceCustomer customer) {
        FinanceCustomerSummaryVO summary = new FinanceCustomerSummaryVO();
        summary.setCCusCode(customer.getCCusCode());
        summary.setCCusName(customer.getCCusName());
        summary.setCCusAbbName(customer.getCCusAbbName());
        summary.setCCusPerson(customer.getCCusPerson());
        summary.setCCusHand(customer.getCCusHand());
        summary.setCCusBank(customer.getCCusBank());
        summary.setCCusAccount(customer.getCCusAccount());
        summary.setIARMoney(customer.getIARMoney());
        summary.setCompanyId(customer.getCompanyId());
        summary.setDEndDate(customer.getDEndDate());
        summary.setUpdatedAt(customer.getUpdatedAt());
        summary.setActive(customer.getDEndDate() == null);
        return summary;
    }

    protected FinanceCustomerDetailVO toDetail(FinanceCustomer customer) {
        FinanceCustomerDetailVO detail = objectMapper.convertValue(customer, FinanceCustomerDetailVO.class);
        detail.setActive(customer.getDEndDate() == null);
        return detail;
    }

    protected String buildCustomerCode() {
        String prefix = "CUS" + LocalDate.now().format(CODE_DATE_FORMATTER);
        QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
        query.likeRight("cCusCode", prefix);
        Long count = financeCustomerMapper.selectCount(query);
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    protected String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u4e3b\u4f53\u4e0d\u80fd\u4e3a\u7a7a");
        }
        return normalized;
    }

    protected void validateLength(Object target, String fieldName, String label, int maxLength) {
        String value = trimToNull(readStringField(target, fieldName));
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 " + maxLength + " \u4e2a\u5b57\u7b26");
        }
    }

    private String readStringField(Object target, String fieldName) {
        try {
            String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = target.getClass().getMethod("get" + suffix);
            Object value = method.invoke(target);
            return value instanceof String stringValue ? stringValue : null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("\u65e0\u6cd5\u8bfb\u53d6\u5b57\u6bb5\uff1a" + fieldName, exception);
        }
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
