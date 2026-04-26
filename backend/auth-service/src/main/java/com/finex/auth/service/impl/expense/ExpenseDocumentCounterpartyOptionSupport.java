package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ExpenseDocumentCounterpartyOptionSupport {

    private static final String PERSONAL_PAYEE_VALUE_PREFIX = "PERSONAL_PAYEE:";
    private static final String PAYEE_SOURCE_PERSONAL = "PERSONAL_PRIVATE_PAYEE";

    private final FinanceVendorMapper financeVendorMapper;
    private final UserMapper userMapper;
    private final UserBankAccountMapper userBankAccountMapper;
    private final FinanceVendorService financeVendorService;

    List<ExpenseCreateVendorOptionVO> listVendorOptions(
            Long userId,
            String keyword,
            Boolean includeDisabled,
            String paymentCompanyId
    ) {
        return financeVendorService.listActiveVendorOptions(
                resolveExpenseCreateCompanyId(userId, paymentCompanyId),
                keyword,
                includeDisabled
        );
    }

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        String normalizedKeyword = trimToNull(keyword);
        if (Boolean.TRUE.equals(personalOnly)) {
            return listPersonalPayeeOptions(userId, normalizedKeyword);
        }

        String currentCompanyId = requireCurrentUserCompanyId(userId);
        List<ExpenseCreatePayeeOptionVO> options = new ArrayList<>();
        financeVendorService.listActiveVendorOptions(currentCompanyId, normalizedKeyword, false).forEach(item -> {
            ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
            option.setValue("VENDOR:" + item.getCVenCode());
            option.setLabel(item.getCVenName());
            option.setSourceType("VENDOR");
            option.setSourceCode(item.getCVenCode());
            option.setSecondaryLabel(item.getSecondaryLabel());
            options.add(option);
        });

        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        );
        users.stream()
                .filter(user -> matchesKeyword(normalizedKeyword, user.getName(), user.getUsername(), user.getPhone(), user.getEmail()))
                .forEach(user -> {
                    ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
                    option.setValue("USER:" + user.getId());
                    option.setLabel(user.getName());
                    option.setSourceType("USER");
                    option.setSourceCode(String.valueOf(user.getId()));
                    option.setSecondaryLabel(trimToNull(user.getPhone()) != null ? user.getPhone() : user.getUsername());
                    options.add(option);
                });
        return options;
    }

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode,
            String paymentCompanyId
    ) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedLinkageMode = trimToNull(linkageMode);
        if ("EMPLOYEE".equalsIgnoreCase(normalizedLinkageMode)) {
            return listPersonalPayeeAccountOptions(userId, normalizedKeyword, trimToNull(payeeName));
        }
        if ("ENTERPRISE".equalsIgnoreCase(normalizedLinkageMode)) {
            return listCounterpartyPayeeAccountOptions(
                    userId,
                    normalizedKeyword,
                    trimToNull(counterpartyCode),
                    paymentCompanyId
            );
        }

        String currentCompanyId = resolveExpenseCreateCompanyId(userId, paymentCompanyId);
        List<ExpenseCreatePayeeAccountOptionVO> options = new ArrayList<>();

        QueryWrapper<FinanceVendor> vendorQuery = new QueryWrapper<>();
        vendorQuery.eq("company_id", currentCompanyId)
                .isNull("dEndDate")
                .isNotNull("cVenAccount")
                .orderByAsc("cVenName", "cVenCode");
        financeVendorMapper.selectList(vendorQuery).stream()
                .filter(item -> currentCompanyId.equals(trimToNull(item.getCompanyId())))
                .filter(item -> trimToNull(item.getCVenAccount()) != null)
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getReceiptAccountName(),
                        item.getCVenName(),
                        item.getCVenAbbName(),
                        item.getCVenBank(),
                        item.getCVenAccount(),
                        item.getCVenBankNub()
                ))
                .forEach(item -> {
                    String accountName = firstNonBlank(item.getReceiptAccountName(), item.getCVenName());
                    ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                    option.setValue("VENDOR:" + item.getCVenCode());
                    option.setLabel(buildAccountLabel(accountName, item.getCVenBank()));
                    option.setSourceType("VENDOR");
                    option.setOwnerCode(item.getCVenCode());
                    option.setOwnerName(accountName);
                    option.setBankName(item.getCVenBank());
                    option.setAccountName(accountName);
                    option.setAccountNoMasked(maskAccountNo(item.getCVenAccount()));
                    option.setSecondaryLabel(buildVendorAccountSecondary(item));
                    options.add(option);
                });

        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        );
        if (!users.isEmpty()) {
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
            List<UserBankAccount> accounts = userBankAccountMapper.selectList(
                    Wrappers.<UserBankAccount>lambdaQuery()
                            .eq(UserBankAccount::getStatus, 1)
                            .in(UserBankAccount::getUserId, userMap.keySet())
                            .orderByDesc(UserBankAccount::getDefaultAccount)
                            .orderByAsc(UserBankAccount::getId)
            );
            accounts.stream()
                    .filter(account -> {
                        User user = userMap.get(account.getUserId());
                        return user != null && matchesKeyword(
                                normalizedKeyword,
                                user.getName(),
                                user.getUsername(),
                                account.getBankName(),
                                account.getAccountName(),
                                account.getAccountNo()
                        );
                    })
                    .forEach(account -> {
                        User user = userMap.get(account.getUserId());
                        ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                        option.setValue("USER_ACCOUNT:" + account.getId());
                        option.setLabel(buildAccountLabel(account.getAccountName(), account.getBankName()));
                        option.setSourceType("USER");
                        option.setOwnerCode(String.valueOf(user.getId()));
                        option.setOwnerName(user.getName());
                        option.setBankName(account.getBankName());
                        option.setAccountName(account.getAccountName());
                        option.setAccountNoMasked(maskAccountNo(account.getAccountNo()));
                        option.setSecondaryLabel(trimToNull(account.getBranchName()) != null ? account.getBranchName() : user.getUsername());
                        options.add(option);
                    });
        }

        return options;
    }

    private List<ExpenseCreatePayeeOptionVO> listPersonalPayeeOptions(Long userId, String normalizedKeyword) {
        List<UserBankAccount> accounts = userBankAccountMapper.selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .eq(UserBankAccount::getStatus, 1)
                        .orderByDesc(UserBankAccount::getDefaultAccount)
                        .orderByAsc(UserBankAccount::getId)
        );
        LinkedHashMap<String, ExpenseCreatePayeeOptionVO> options = new LinkedHashMap<>();
        for (UserBankAccount account : accounts) {
            String accountName = trimToNull(account.getAccountName());
            if (accountName == null) {
                continue;
            }
            if (!matchesKeyword(normalizedKeyword, accountName, account.getAccountNo(), account.getBankName(), account.getBranchName())) {
                continue;
            }
            options.computeIfAbsent(accountName, key -> {
                ExpenseCreatePayeeOptionVO option = new ExpenseCreatePayeeOptionVO();
                option.setValue(PERSONAL_PAYEE_VALUE_PREFIX + key);
                option.setLabel(key);
                option.setSourceType(PAYEE_SOURCE_PERSONAL);
                option.setSourceCode(key);
                option.setSecondaryLabel("未配置开户行");
                return option;
            });
        }
        return new ArrayList<>(options.values());
    }

    private List<ExpenseCreatePayeeAccountOptionVO> listPersonalPayeeAccountOptions(
            Long userId,
            String normalizedKeyword,
            String payeeName
    ) {
        String normalizedPayeeName = normalizePayeeName(payeeName);
        List<UserBankAccount> accounts = userBankAccountMapper.selectList(
                Wrappers.<UserBankAccount>lambdaQuery()
                        .eq(UserBankAccount::getUserId, userId)
                        .eq(UserBankAccount::getStatus, 1)
                        .orderByDesc(UserBankAccount::getDefaultAccount)
                        .orderByAsc(UserBankAccount::getId)
        );
        return accounts.stream()
                .filter(account -> normalizedPayeeName == null || Objects.equals(trimToNull(account.getAccountName()), normalizedPayeeName))
                .filter(account -> matchesKeyword(
                        normalizedKeyword,
                        account.getAccountName(),
                        account.getAccountNo(),
                        account.getBankName(),
                        account.getBranchName()
                ))
                .map(account -> {
                    ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                    option.setValue("USER_ACCOUNT:" + account.getId());
                    option.setLabel(buildAccountLabel(account.getAccountName(), account.getBankName()));
                    option.setSourceType("USER");
                    option.setOwnerCode(String.valueOf(userId));
                    option.setOwnerName(account.getAccountName());
                    option.setBankName(account.getBankName());
                    option.setAccountName(account.getAccountName());
                    option.setAccountNoMasked(maskAccountNo(account.getAccountNo()));
                    option.setSecondaryLabel(trimToNull(account.getBranchName()) != null ? account.getBranchName() : "未配置开户行");
                    return option;
                })
                .toList();
    }

    private List<ExpenseCreatePayeeAccountOptionVO> listCounterpartyPayeeAccountOptions(
            Long userId,
            String normalizedKeyword,
            String counterpartyCode,
            String paymentCompanyId
    ) {
        String normalizedVendorCode = trimToNull(counterpartyCode);
        if (normalizedVendorCode == null) {
            return List.of();
        }
        String currentCompanyId = resolveExpenseCreateCompanyId(userId, paymentCompanyId);
        FinanceVendor vendor = financeVendorMapper.selectOne(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .eq(FinanceVendor::getCompanyId, currentCompanyId)
                        .eq(FinanceVendor::getCVenCode, normalizedVendorCode)
                        .last("limit 1")
        );
        if (vendor == null || trimToNull(vendor.getCVenAccount()) == null) {
            return List.of();
        }
        if (!matchesKeyword(
                normalizedKeyword,
                vendor.getReceiptAccountName(),
                vendor.getCVenName(),
                vendor.getCVenAbbName(),
                vendor.getCVenBank(),
                vendor.getCVenAccount(),
                vendor.getCVenBankNub()
        )) {
            return List.of();
        }
        String accountName = firstNonBlank(vendor.getReceiptAccountName(), vendor.getCVenName());
        ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
        option.setValue("VENDOR:" + vendor.getCVenCode());
        option.setLabel(buildAccountLabel(accountName, vendor.getCVenBank()));
        option.setSourceType("VENDOR");
        option.setOwnerCode(vendor.getCVenCode());
        option.setOwnerName(accountName);
        option.setBankName(vendor.getCVenBank());
        option.setAccountName(accountName);
        option.setAccountNoMasked(maskAccountNo(vendor.getCVenAccount()));
        option.setSecondaryLabel(buildVendorAccountSecondary(vendor));
        return List.of(option);
    }

    private User requireActiveUser(Long userId) {
        User user = loadActiveUser(userId);
        if (user == null) {
            throw new IllegalStateException("当前用户不存在或已被禁用，无法继续处理");
        }
        return user;
    }

    private String requireCurrentUserCompanyId(Long userId) {
        User user = requireActiveUser(userId);
        String companyId = trimToNull(user.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("当前用户未配置所属公司，无法继续处理");
        }
        return companyId;
    }

    private String resolveExpenseCreateCompanyId(Long userId, String paymentCompanyId) {
        String explicitCompanyId = trimToNull(paymentCompanyId);
        return explicitCompanyId != null ? explicitCompanyId : requireCurrentUserCompanyId(userId);
    }

    private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

    private String normalizePayeeName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return trimToNull(normalized.substring(PERSONAL_PAYEE_VALUE_PREFIX.length()));
        }
        return normalized;
    }

    private String buildAccountLabel(String accountName, String bankName) {
        String left = firstNonBlank(accountName, bankName);
        String right = left != null && Objects.equals(left, trimToNull(bankName)) ? null : trimToNull(bankName);
        return right == null ? (left == null ? "未命名账户" : left) : left + " / " + right;
    }

    private String buildVendorAccountSecondary(FinanceVendor vendor) {
        List<String> parts = new ArrayList<>();
        if (trimToNull(vendor.getReceiptBranchName()) != null) {
            parts.add(vendor.getReceiptBranchName().trim());
        } else if (trimToNull(vendor.getCVenBank()) != null) {
            parts.add(vendor.getCVenBank().trim());
        }
        if (trimToNull(vendor.getCVenAccount()) != null) {
            parts.add(maskAccountNo(vendor.getCVenAccount()));
        }
        return String.join(" / ", parts);
    }

    private String maskAccountNo(String accountNo) {
        String normalized = trimToNull(accountNo);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized;
        }
        return normalized.substring(0, 4) + " **** " + normalized.substring(normalized.length() - 4);
    }

    private boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
