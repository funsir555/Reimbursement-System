package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

public class FinanceCustomerMutationDomainSupport extends AbstractFinanceCustomerArchiveSupport {

    public FinanceCustomerMutationDomainSupport(
            FinanceCustomerMapper financeCustomerMapper,
            ObjectMapper objectMapper
    ) {
        super(financeCustomerMapper, objectMapper);
    }

    public FinanceCustomerDetailVO createCustomer(String companyId, FinanceCustomerSaveDTO dto, String operatorName) {
        validateSave(dto, null);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceCustomer customer = objectMapper.convertValue(dto, FinanceCustomer.class);
        customer.setCCusCode(trimToNull(dto.getCCusCode()) == null ? buildCustomerCode() : dto.getCCusCode().trim());
        customer.setCompanyId(normalizedCompanyId);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setCreatedAt(LocalDateTime.now());

        if (financeCustomerMapper.selectById(customer.getCCusCode()) != null) {
            throw new IllegalStateException("????????????????");
        }

        financeCustomerMapper.insert(customer);
        return toDetail(requireCustomer(normalizedCompanyId, customer.getCCusCode()));
    }

    public FinanceCustomerDetailVO updateCustomer(String companyId, String customerCode, FinanceCustomerSaveDTO dto, String operatorName) {
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceCustomer existing = requireCustomer(normalizedCompanyId, customerCode);
        validateSave(dto, existing);

        FinanceCustomer next = objectMapper.convertValue(dto, FinanceCustomer.class);
        BeanUtils.copyProperties(next, existing, "cCusCode", "createdAt", "updatedAt");
        existing.setCCusCode(customerCode);
        existing.setCompanyId(normalizedCompanyId);
        existing.setUpdatedAt(LocalDateTime.now());
        financeCustomerMapper.updateById(existing);
        return toDetail(requireCustomer(normalizedCompanyId, customerCode));
    }

    public Boolean disableCustomer(String companyId, String customerCode, String operatorName) {
        FinanceCustomer customer = requireCustomer(companyId, customerCode);
        customer.setDEndDate(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        financeCustomerMapper.updateById(customer);
        return Boolean.TRUE;
    }
}
