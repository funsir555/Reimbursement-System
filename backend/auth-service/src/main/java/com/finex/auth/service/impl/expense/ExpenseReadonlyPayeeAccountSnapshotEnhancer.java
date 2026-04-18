package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExpenseReadonlyPayeeAccountSnapshotEnhancer {

    private static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    private static final String PAYEE_ACCOUNT_COMPONENT_CODE = "payee-account";
    private static final String USER_ACCOUNT_PREFIX = "USER_ACCOUNT:";
    private static final String VENDOR_PREFIX = "VENDOR:";

    private final FinanceVendorMapper financeVendorMapper;
    private final UserBankAccountMapper userBankAccountMapper;

    Map<String, Object> enhanceFormData(
            Map<String, Object> schema,
            Map<String, Object> formData,
            String fallbackPaymentCompanyId
    ) {
        if (schema == null || formData == null || formData.isEmpty()) {
            return formData;
        }
        List<String> fieldKeys = findBusinessFieldKeys(schema, PAYEE_ACCOUNT_COMPONENT_CODE);
        if (fieldKeys.isEmpty()) {
            return formData;
        }
        String paymentCompanyId = firstNonBlank(
                extractFirstBusinessComponentValue(schema, formData, PAYMENT_COMPANY_COMPONENT_CODE),
                fallbackPaymentCompanyId
        );
        for (String fieldKey : fieldKeys) {
            Object rawValue = formData.get(fieldKey);
            Object enhancedValue = enhancePayeeAccountValue(rawValue, paymentCompanyId);
            if (enhancedValue != null) {
                formData.put(fieldKey, enhancedValue);
            }
        }
        return formData;
    }

    private Object enhancePayeeAccountValue(Object rawValue, String paymentCompanyId) {
        String lookupValue = firstLookupValue(rawValue);
        if (lookupValue == null) {
            return null;
        }
        Map<String, Object> snapshot = rawValue instanceof Map<?, ?> rawMap
                ? copyMap(rawMap)
                : new LinkedHashMap<>();
        snapshot.putIfAbsent("value", lookupValue);
        if (lookupValue.startsWith(USER_ACCOUNT_PREFIX)) {
            return enhanceUserAccountSnapshot(snapshot, lookupValue);
        }
        if (lookupValue.startsWith(VENDOR_PREFIX)) {
            return enhanceVendorAccountSnapshot(snapshot, lookupValue, paymentCompanyId);
        }
        return null;
    }

    private Map<String, Object> enhanceUserAccountSnapshot(Map<String, Object> snapshot, String lookupValue) {
        Long accountId = parseLong(trimToNull(lookupValue.substring(USER_ACCOUNT_PREFIX.length())));
        if (accountId == null) {
            return null;
        }
        UserBankAccount account = userBankAccountMapper.selectById(accountId);
        if (account == null) {
            return null;
        }
        String accountName = firstNonBlank(account.getAccountName(), snapshot.get("accountName"), snapshot.get("ownerName"));
        String bankDisplayName = firstNonBlank(account.getBranchName(), account.getBankName(), snapshot.get("bankName"), snapshot.get("bankBranchName"));
        snapshot.put("sourceType", firstNonBlank(snapshot.get("sourceType"), "USER"));
        snapshot.put("ownerName", accountName);
        snapshot.put("accountName", accountName);
        snapshot.put("accountNo", firstNonBlank(account.getAccountNo(), snapshot.get("accountNo")));
        snapshot.put("bankName", bankDisplayName);
        snapshot.put("bankBranchName", bankDisplayName);
        return snapshot;
    }

    private Map<String, Object> enhanceVendorAccountSnapshot(
            Map<String, Object> snapshot,
            String lookupValue,
            String paymentCompanyId
    ) {
        String companyId = trimToNull(paymentCompanyId);
        String vendorCode = trimToNull(lookupValue.substring(VENDOR_PREFIX.length()));
        if (companyId == null || vendorCode == null) {
            return null;
        }
        FinanceVendor vendor = financeVendorMapper.selectOne(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .eq(FinanceVendor::getCompanyId, companyId)
                        .eq(FinanceVendor::getCVenCode, vendorCode)
                        .last("limit 1")
        );
        if (vendor == null) {
            return null;
        }
        String accountName = firstNonBlank(vendor.getReceiptAccountName(), vendor.getCVenName(), snapshot.get("accountName"), snapshot.get("ownerName"));
        String bankDisplayName = firstNonBlank(vendor.getReceiptBranchName(), vendor.getCVenBank(), snapshot.get("bankName"), snapshot.get("bankBranchName"));
        snapshot.put("sourceType", firstNonBlank(snapshot.get("sourceType"), "VENDOR"));
        snapshot.put("ownerCode", firstNonBlank(snapshot.get("ownerCode"), vendor.getCVenCode()));
        snapshot.put("ownerName", accountName);
        snapshot.put("accountName", accountName);
        snapshot.put("accountNo", firstNonBlank(vendor.getCVenAccount(), snapshot.get("accountNo")));
        snapshot.put("bankName", bankDisplayName);
        snapshot.put("bankBranchName", bankDisplayName);
        return snapshot;
    }

    private List<String> findBusinessFieldKeys(Map<String, Object> schema, String componentCode) {
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks) || blocks.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Map<String, Object> props = asMap(blockMap.get("props"));
            if (!Objects.equals(trimToNull(stringValue(props.get("componentCode"))), componentCode)) {
                continue;
            }
            String fieldKey = trimToNull(stringValue(blockMap.get("fieldKey")));
            if (fieldKey != null) {
                result.add(fieldKey);
            }
        }
        return result;
    }

    private String extractFirstBusinessComponentValue(Map<String, Object> schema, Map<String, Object> formData, String componentCode) {
        if (schema == null || formData == null || trimToNull(componentCode) == null) {
            return null;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return null;
        }
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Map<String, Object> props = asMap(blockMap.get("props"));
            if (!Objects.equals(trimToNull(stringValue(props.get("componentCode"))), componentCode)) {
                continue;
            }
            String fieldKey = trimToNull(stringValue(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            String value = firstLookupValue(formData.get(fieldKey));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstLookupValue(Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = extractLookupValue(item);
                if (normalized != null) {
                    return normalized;
                }
            }
            return null;
        }
        return extractLookupValue(value);
    }

    private String extractLookupValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            String normalized = firstNonBlank(
                    map.get("value"),
                    map.get("code"),
                    map.get("id"),
                    map.get("sourceCode")
            );
            if (normalized != null) {
                return normalized;
            }
            String sourceType = trimToNull(stringValue(map.get("sourceType")));
            String ownerCode = trimToNull(stringValue(map.get("ownerCode")));
            if (ownerCode != null) {
                if (Objects.equals(sourceType, "USER")) {
                    return USER_ACCOUNT_PREFIX + ownerCode;
                }
                if (Objects.equals(sourceType, "VENDOR")) {
                    return VENDOR_PREFIX + ownerCode;
                }
            }
            return null;
        }
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private Map<String, Object> copyMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Map.of();
        }
        return copyMap(rawMap);
    }

    private Long parseLong(String value) {
        try {
            String normalized = trimToNull(value);
            return normalized == null ? null : Long.valueOf(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = trimToNull(stringValue(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
