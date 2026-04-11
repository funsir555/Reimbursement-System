package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public abstract class AbstractFinanceVendorArchiveSupport {

    protected static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected final FinanceVendorMapper financeVendorMapper;
    protected final UserMapper userMapper;
    protected final ObjectMapper objectMapper;

    protected AbstractFinanceVendorArchiveSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        this.financeVendorMapper = financeVendorMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    protected void validateSave(FinanceVendorSaveDTO dto, FinanceVendor existing, boolean paymentInfoRequired) {
        if (dto == null) {
            throw new IllegalArgumentException("Supplier payload is required");
        }
        if (trimToNull(dto.getCVenName()) == null) {
            throw new IllegalArgumentException("Supplier name is required");
        }
        if (paymentInfoRequired) {
            if (effectiveReceiptAccountName(dto) == null) {
                throw new IllegalArgumentException("Receipt account name is required");
            }
            if (trimToNull(dto.getCVenAccount()) == null) {
                throw new IllegalArgumentException("Bank account number is required");
            }
            if (trimToNull(dto.getCVenBank()) == null) {
                throw new IllegalArgumentException("Bank name is required");
            }
            if (trimToNull(dto.getReceiptBankProvince()) == null || trimToNull(dto.getReceiptBankCity()) == null) {
                throw new IllegalArgumentException("Bank province and city are required");
            }
            if (trimToNull(dto.getReceiptBranchName()) == null) {
                throw new IllegalArgumentException("Branch name is required");
            }
        }
        if (existing == null && trimToNull(dto.getCVenCode()) != null) {
            QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
            query.eq("cVenCode", dto.getCVenCode().trim()).last("limit 1");
            if (financeVendorMapper.selectOne(query) != null) {
                throw new IllegalStateException("Supplier code already exists");
            }
        }
    }

    protected FinanceVendor requireVendor(String companyId, String vendorCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(vendorCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Supplier code is required");
        }
        FinanceVendor vendor = financeVendorMapper.selectById(normalizedCode);
        if (vendor == null) {
            throw new IllegalStateException("Supplier not found");
        }
        if (!normalizedCompanyId.equals(trimToNull(vendor.getCompanyId()))) {
            throw new SecurityException("Supplier does not belong to the current company");
        }
        return vendor;
    }

    protected FinanceVendorSummaryVO toSummary(FinanceVendor vendor) {
        FinanceVendorSummaryVO summary = new FinanceVendorSummaryVO();
        summary.setCVenCode(vendor.getCVenCode());
        summary.setCVenName(vendor.getCVenName());
        summary.setCVenAbbName(vendor.getCVenAbbName());
        summary.setCVCCode(vendor.getCVCCode());
        summary.setCVenPerson(vendor.getCVenPerson());
        summary.setCVenPhone(vendor.getCVenPhone());
        summary.setCVenBank(vendor.getCVenBank());
        summary.setCVenAccount(vendor.getCVenAccount());
        summary.setCompanyId(vendor.getCompanyId());
        summary.setDEndDate(vendor.getDEndDate());
        summary.setUpdatedAt(vendor.getUpdatedAt());
        summary.setActive(vendor.getDEndDate() == null);
        return summary;
    }

    protected FinanceVendorDetailVO toDetail(FinanceVendor vendor) {
        FinanceVendorDetailVO detail = objectMapper.convertValue(vendor, FinanceVendorDetailVO.class);
        detail.setActive(vendor.getDEndDate() == null);
        return detail;
    }

    protected ExpenseCreateVendorOptionVO toOption(FinanceVendor vendor) {
        ExpenseCreateVendorOptionVO option = new ExpenseCreateVendorOptionVO();
        option.setValue(vendor.getCVenCode());
        option.setLabel(vendor.getCVenName());
        option.setSecondaryLabel(buildVendorSecondaryLabel(vendor.getCVenCode(), vendor.getCVenAbbName()));
        option.setCVenCode(vendor.getCVenCode());
        option.setCVenName(vendor.getCVenName());
        option.setCVenAbbName(vendor.getCVenAbbName());
        return option;
    }

    protected void normalizePaymentInfoFields(FinanceVendor vendor, FinanceVendorSaveDTO dto) {
        vendor.setReceiptAccountName(effectiveReceiptAccountName(dto));
        vendor.setReceiptBankProvince(trimToNull(dto.getReceiptBankProvince()));
        vendor.setReceiptBankCity(trimToNull(dto.getReceiptBankCity()));
        vendor.setReceiptBranchCode(trimToNull(dto.getReceiptBranchCode()));
        vendor.setReceiptBranchName(trimToNull(dto.getReceiptBranchName()));
        vendor.setCVenBank(trimToNull(dto.getCVenBank()));
        vendor.setCVenBankCode(trimToNull(dto.getCVenBankCode()));
        vendor.setCVenBankNub(trimToNull(dto.getCVenBankNub()));
        vendor.setCVenAccount(trimToNull(dto.getCVenAccount()));
    }

    protected String effectiveReceiptAccountName(FinanceVendorSaveDTO dto) {
        String explicitName = trimToNull(dto.getReceiptAccountName());
        return explicitName != null ? explicitName : trimToNull(dto.getCVenName());
    }

    protected String buildVendorCode() {
        String prefix = "VEN" + LocalDate.now().format(CODE_DATE_FORMATTER);
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        query.likeRight("cVenCode", prefix);
        Long count = financeVendorMapper.selectCount(query);
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    protected String buildVendorSecondaryLabel(String vendorCode, String vendorAbbName) {
        String code = trimToNull(vendorCode);
        String abbName = trimToNull(vendorAbbName);
        if (code != null && abbName != null) {
            return code + " / " + abbName;
        }
        return code != null ? code : (abbName == null ? "" : abbName);
    }

    protected String defaultOperator(String operatorName) {
        String normalized = trimToNull(operatorName);
        return normalized == null ? "system" : normalized;
    }

    protected String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("Company id is required");
        }
        return normalized;
    }

    protected String requireCurrentUserCompanyId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Current user id is required");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new IllegalStateException("Current user not found");
        }
        String companyId = trimToNull(currentUser.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("Current user company is missing");
        }
        return companyId;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}