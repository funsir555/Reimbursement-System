package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;

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
            throw new IllegalArgumentException("??????????????????");
        }
        if (trimToNull(dto.getCCusName()) == null) {
            throw new IllegalArgumentException("??????????????????");
        }
        if (existing == null && trimToNull(dto.getCCusCode()) != null) {
            QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
            query.eq("cCusCode", dto.getCCusCode().trim()).last("limit 1");
            if (financeCustomerMapper.selectOne(query) != null) {
                throw new IllegalStateException("????????????????");
            }
        }
    }

    protected FinanceCustomer requireCustomer(String companyId, String customerCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(customerCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("??????????????????");
        }
        FinanceCustomer customer = financeCustomerMapper.selectById(normalizedCode);
        if (customer == null) {
            throw new IllegalStateException("???????????");
        }
        if (!normalizedCompanyId.equals(trimToNull(customer.getCompanyId()))) {
            throw new SecurityException("???????????????????");
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
            throw new IllegalArgumentException("??????????????????");
        }
        return normalized;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
