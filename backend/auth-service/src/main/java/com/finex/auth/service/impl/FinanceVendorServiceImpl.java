package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVendorService;
import com.finex.auth.service.impl.financearchive.FinanceVendorMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceVendorOptionDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceVendorQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinanceVendorServiceImpl implements FinanceVendorService {

    private final FinanceVendorQueryDomainSupport financeVendorQueryDomainSupport;
    private final FinanceVendorMutationDomainSupport financeVendorMutationDomainSupport;
    private final FinanceVendorOptionDomainSupport financeVendorOptionDomainSupport;

    public FinanceVendorServiceImpl(FinanceVendorMapper financeVendorMapper, UserMapper userMapper, ObjectMapper objectMapper) {
        this.financeVendorQueryDomainSupport = new FinanceVendorQueryDomainSupport(financeVendorMapper, userMapper, objectMapper);
        this.financeVendorMutationDomainSupport = new FinanceVendorMutationDomainSupport(financeVendorMapper, userMapper, objectMapper);
        this.financeVendorOptionDomainSupport = new FinanceVendorOptionDomainSupport(financeVendorMapper, userMapper, objectMapper);
    }

    @Override
    public List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled) {
        return financeVendorQueryDomainSupport.listVendors(companyId, keyword, includeDisabled);
    }

    @Override
    public FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode) {
        return financeVendorQueryDomainSupport.getVendorDetail(companyId, vendorCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.createVendor(companyId, dto, operatorName, paymentInfoRequired);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.createVendor(currentUserId, dto, operatorName, paymentInfoRequired);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.updateVendor(companyId, vendorCode, dto, operatorName, paymentInfoRequired);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableVendor(String companyId, String vendorCode, String operatorName) {
        return financeVendorMutationDomainSupport.disableVendor(companyId, vendorCode, operatorName);
    }

    @Override
    public List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword, Boolean includeDisabled) {
        return financeVendorOptionDomainSupport.listActiveVendorOptions(companyId, keyword, includeDisabled);
    }
}