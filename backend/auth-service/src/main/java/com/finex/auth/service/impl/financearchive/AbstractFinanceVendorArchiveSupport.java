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

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public abstract class AbstractFinanceVendorArchiveSupport {

    protected static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BANK_DIRECTORY_REQUIRED_MESSAGE = "请选择开户银行、开户省、开户市与开户网点后再保存";

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
            throw new IllegalArgumentException("\u4f9b\u5e94\u5546\u4fdd\u5b58\u53c2\u6570\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (trimToNull(dto.getCVenName()) == null) {
            throw new IllegalArgumentException("\u4f9b\u5e94\u5546\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }

        validateLength(dto, "cVenCode", "\u4f9b\u5e94\u5546\u7f16\u7801", 64);
        validateLength(dto, "cVenName", "\u4f9b\u5e94\u5546\u540d\u79f0", 128);
        validateLength(dto, "cVenAbbName", "\u4f9b\u5e94\u5546\u7b80\u79f0", 64);
        validateLength(dto, "cVCCode", "\u5206\u7c7b\u7f16\u7801", 64);
        validateLength(dto, "cVenBank", "\u5f00\u6237\u94f6\u884c", 128);
        validateLength(dto, "cVenAccount", "\u94f6\u884c\u8d26\u53f7", 64);
        validateLength(dto, "cVenBankNub", "\u8054\u884c\u53f7", 64);
        validateLength(dto, "receiptAccountName", "\u8d26\u6237\u540d", 128);
        validateLength(dto, "receiptBranchName", "\u5f00\u6237\u7f51\u70b9", 128);
        validateLength(dto, "cVenPerson", "\u8054\u7cfb\u4eba", 64);
        validateLength(dto, "cVenPhone", "\u7535\u8bdd", 32);
        validateLength(dto, "cVenHand", "\u624b\u673a", 32);
        validateLength(dto, "cCreatePerson", "\u5efa\u6863\u4eba", 64);
        validateLength(dto, "cDCCode", "\u5730\u533a\u7f16\u7801", 64);
        validateLength(dto, "cModifyPerson", "\u53d8\u66f4\u4eba", 64);
        validateLength(dto, "cRelCustomer", "\u5173\u8054\u5ba2\u6237", 64);
        validateLength(dto, "cVenBankCode", "\u5f00\u6237\u94f6\u884c\u7f16\u7801", 64);
        validateLength(dto, "cVenBP", "\u547c\u673a", 32);
        validateLength(dto, "cVenFax", "\u4f20\u771f", 32);
        validateLength(dto, "cVenHeadCode", "\u4e0a\u7ea7\u4f9b\u5e94\u5546\u7f16\u7801", 64);
        validateLength(dto, "cVenLPerson", "\u6cd5\u4eba", 64);
        validateLength(dto, "cVenPayCond", "\u4ed8\u6b3e\u6761\u4ef6\u7f16\u7801", 64);
        validateLength(dto, "cVenPostCode", "\u90ae\u653f\u7f16\u7801", 16);
        validateLength(dto, "cVenPPerson", "\u4e13\u8425\u4e1a\u52a1\u5458", 64);
        validateLength(dto, "cVenTradeCCode", "\u884c\u4e1a\u5206\u7c7b", 64);
        validateLength(dto, "cVenWhCode", "\u4ed3\u5e93\u7f16\u7801", 64);

        if (paymentInfoRequired) {
            if (effectiveReceiptAccountName(dto) == null) {
                throw new IllegalArgumentException("\u8d26\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (trimToNull(dto.getCVenAccount()) == null) {
                throw new IllegalArgumentException("\u94f6\u884c\u8d26\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (
                    trimToNull(dto.getCVenBank()) == null
                            || trimToNull(dto.getCVenBankCode()) == null
                            || trimToNull(dto.getReceiptBankProvince()) == null
                            || trimToNull(dto.getReceiptBankCity()) == null
                            || trimToNull(dto.getReceiptBranchCode()) == null
                            || trimToNull(dto.getReceiptBranchName()) == null
            ) {
                throw new IllegalArgumentException(BANK_DIRECTORY_REQUIRED_MESSAGE);
            }
        }

        if (existing == null && trimToNull(dto.getCVenCode()) != null) {
            QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
            query.eq("cVenCode", dto.getCVenCode().trim()).last("limit 1");
            if (financeVendorMapper.selectOne(query) != null) {
                throw new IllegalStateException("\u4f9b\u5e94\u5546\u7f16\u7801\u5df2\u5b58\u5728");
            }
        }
    }

    protected FinanceVendor requireVendor(String companyId, String vendorCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(vendorCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("\u4f9b\u5e94\u5546\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        FinanceVendor vendor = financeVendorMapper.selectById(normalizedCode);
        if (vendor == null) {
            throw new IllegalStateException("\u4f9b\u5e94\u5546\u4e0d\u5b58\u5728");
        }
        if (!normalizedCompanyId.equals(trimToNull(vendor.getCompanyId()))) {
            throw new SecurityException("\u4f9b\u5e94\u5546\u4e0d\u5c5e\u4e8e\u5f53\u524d\u516c\u53f8");
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
        option.setLabel(firstNonBlank(vendor.getReceiptAccountName(), vendor.getCVenName(), vendor.getCVenCode()));
        option.setSecondaryLabel(buildVendorSecondaryLabel(vendor));
        option.setCVenCode(vendor.getCVenCode());
        option.setCVenName(vendor.getCVenName());
        option.setCVenAbbName(vendor.getCVenAbbName());
        return option;
    }

    protected void normalizePaymentInfoFields(FinanceVendor vendor, FinanceVendorSaveDTO dto) {
        String previousBankCode = trimToNull(vendor.getCVenBankCode());
        String previousProvince = trimToNull(vendor.getReceiptBankProvince());
        String previousCity = trimToNull(vendor.getReceiptBankCity());
        String previousBranchCode = trimToNull(vendor.getReceiptBranchCode());
        String previousBranchName = trimToNull(vendor.getReceiptBranchName());
        String previousCnapsCode = trimToNull(vendor.getCVenBankNub());

        String nextBankCode = trimToNull(dto.getCVenBankCode());
        String nextProvince = trimToNull(dto.getReceiptBankProvince());
        String nextCity = trimToNull(dto.getReceiptBankCity());
        String nextBranchCode = trimToNull(dto.getReceiptBranchCode());
        String nextBranchName = trimToNull(dto.getReceiptBranchName());
        boolean branchSelectionChanged = !Objects.equals(previousBankCode, nextBankCode)
                || !Objects.equals(previousProvince, nextProvince)
                || !Objects.equals(previousCity, nextCity)
                || !Objects.equals(previousBranchCode, nextBranchCode)
                || !Objects.equals(previousBranchName, nextBranchName);

        vendor.setReceiptAccountName(effectiveReceiptAccountName(dto));
        vendor.setReceiptBankProvince(nextProvince);
        vendor.setReceiptBankCity(nextCity);
        vendor.setReceiptBranchCode(nextBranchCode);
        vendor.setReceiptBranchName(nextBranchName);
        vendor.setCVenBank(trimToNull(dto.getCVenBank()));
        vendor.setCVenBankCode(nextBankCode);
        vendor.setCVenBankNub(resolveWeakCnapsCode(previousCnapsCode, trimToNull(dto.getCVenBankNub()), branchSelectionChanged));
        vendor.setCVenAccount(trimToNull(dto.getCVenAccount()));
    }

    protected String resolveWeakCnapsCode(String previousCnapsCode, String submittedCnapsCode, boolean branchSelectionChanged) {
        if (submittedCnapsCode != null) {
            return submittedCnapsCode;
        }
        if (branchSelectionChanged) {
            return null;
        }
        return previousCnapsCode;
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

    protected String buildVendorSecondaryLabel(FinanceVendor vendor) {
        String code = trimToNull(vendor.getCVenCode());
        String vendorName = trimToNull(vendor.getCVenName());
        String vendorAbbName = trimToNull(vendor.getCVenAbbName());
        String receiptAccountName = trimToNull(vendor.getReceiptAccountName());

        StringBuilder builder = new StringBuilder();
        appendSecondaryPart(builder, code);
        if (vendorName != null && !vendorName.equals(receiptAccountName)) {
            appendSecondaryPart(builder, vendorName);
        }
        if (vendorAbbName != null && !vendorAbbName.equals(vendorName) && !vendorAbbName.equals(receiptAccountName)) {
            appendSecondaryPart(builder, vendorAbbName);
        }
        return builder.toString();
    }

    protected String defaultOperator(String operatorName) {
        String normalized = trimToNull(operatorName);
        return normalized == null ? "system" : normalized;
    }

    protected String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u4e3b\u4f53\u4e0d\u80fd\u4e3a\u7a7a");
        }
        return normalized;
    }

    protected String requireCurrentUserCompanyId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("\u5f53\u524d\u7528\u6237ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new IllegalStateException("\u5f53\u524d\u767b\u5f55\u7528\u6237\u4e0d\u5b58\u5728");
        }
        String companyId = trimToNull(currentUser.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("\u5f53\u524d\u7528\u6237\u516c\u53f8\u4fe1\u606f\u7f3a\u5931");
        }
        return companyId;
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

    protected String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private void appendSecondaryPart(StringBuilder builder, String value) {
        if (value == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" / ");
        }
        builder.append(value);
    }
}
