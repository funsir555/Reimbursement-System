package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.service.FinanceCustomerService;
import com.finex.auth.service.impl.financearchive.FinanceCustomerMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceCustomerQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinanceCustomerServiceImpl implements FinanceCustomerService {

    private final FinanceCustomerQueryDomainSupport financeCustomerQueryDomainSupport;
    private final FinanceCustomerMutationDomainSupport financeCustomerMutationDomainSupport;

    public FinanceCustomerServiceImpl(FinanceCustomerMapper financeCustomerMapper, ObjectMapper objectMapper) {
        this.financeCustomerQueryDomainSupport = new FinanceCustomerQueryDomainSupport(financeCustomerMapper, objectMapper);
        this.financeCustomerMutationDomainSupport = new FinanceCustomerMutationDomainSupport(financeCustomerMapper, objectMapper);
    }

    @Override
    public List<FinanceCustomerSummaryVO> listCustomers(String companyId, String keyword, Boolean includeDisabled) {
        return financeCustomerQueryDomainSupport.listCustomers(companyId, keyword, includeDisabled);
    }

    @Override
    public FinanceCustomerDetailVO getCustomerDetail(String companyId, String customerCode) {
        return financeCustomerQueryDomainSupport.getCustomerDetail(companyId, customerCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCustomerDetailVO createCustomer(String companyId, FinanceCustomerSaveDTO dto, String operatorName) {
        return financeCustomerMutationDomainSupport.createCustomer(companyId, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCustomerDetailVO updateCustomer(String companyId, String customerCode, FinanceCustomerSaveDTO dto, String operatorName) {
        return financeCustomerMutationDomainSupport.updateCustomer(companyId, customerCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableCustomer(String companyId, String customerCode, String operatorName) {
        return financeCustomerMutationDomainSupport.disableCustomer(companyId, customerCode, operatorName);
    }
}
