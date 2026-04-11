package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

public class FinanceVendorMutationDomainSupport extends AbstractFinanceVendorArchiveSupport {

    public FinanceVendorMutationDomainSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        super(financeVendorMapper, userMapper, objectMapper);
    }

    public FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        validateSave(dto, null, paymentInfoRequired);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceVendor vendor = objectMapper.convertValue(dto, FinanceVendor.class);
        normalizePaymentInfoFields(vendor, dto);
        vendor.setCVenCode(trimToNull(dto.getCVenCode()) == null ? buildVendorCode() : dto.getCVenCode().trim());
        vendor.setCompanyId(normalizedCompanyId);
        vendor.setCCreatePerson(defaultOperator(operatorName));
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setCreatedAt(LocalDateTime.now());

        if (financeVendorMapper.selectById(vendor.getCVenCode()) != null) {
            throw new IllegalStateException("Supplier code already exists");
        }

        financeVendorMapper.insert(vendor);
        return toDetail(requireVendor(normalizedCompanyId, vendor.getCVenCode()));
    }

    public FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return createVendor(requireCurrentUserCompanyId(currentUserId), dto, operatorName, paymentInfoRequired);
    }

    public FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceVendor existing = requireVendor(normalizedCompanyId, vendorCode);
        validateSave(dto, existing, paymentInfoRequired);

        FinanceVendor next = objectMapper.convertValue(dto, FinanceVendor.class);
        String createdBy = existing.getCCreatePerson();
        LocalDateTime createdAt = existing.getCreatedAt();
        normalizePaymentInfoFields(next, dto);
        BeanUtils.copyProperties(next, existing, "cVenCode", "createdAt", "updatedAt", "cCreatePerson");
        existing.setCVenCode(vendorCode);
        existing.setCompanyId(normalizedCompanyId);
        existing.setCCreatePerson(createdBy);
        existing.setCreatedAt(createdAt);
        existing.setCModifyPerson(defaultOperator(operatorName));
        existing.setDModifyDate(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(existing);
        return toDetail(requireVendor(normalizedCompanyId, vendorCode));
    }

    public Boolean disableVendor(String companyId, String vendorCode, String operatorName) {
        FinanceVendor vendor = requireVendor(companyId, vendorCode);
        vendor.setDEndDate(LocalDateTime.now());
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(vendor);
        return Boolean.TRUE;
    }
}
