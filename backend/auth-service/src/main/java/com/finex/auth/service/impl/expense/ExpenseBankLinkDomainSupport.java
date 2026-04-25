package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.SystemCompanyBankAccount;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ExpenseBankLinkDomainSupport extends AbstractExpensePaymentSupport {

    private final ExpensePaymentRecordSupport recordSupport;

    ExpenseBankLinkDomainSupport(
            ExpensePaymentSupportContext context,
            ExpensePaymentRecordSupport recordSupport
    ) {
        super(context);
        this.recordSupport = recordSupport;
    }

    List<ExpenseBankLinkSummaryVO> listBankLinks() {
        List<SystemCompanyBankAccount> accounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .orderByAsc(SystemCompanyBankAccount::getCompanyId)
                        .orderByDesc(SystemCompanyBankAccount::getDirectConnectEnabled)
                        .orderByAsc(SystemCompanyBankAccount::getId)
        );
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> companyNameMap = buildCompanyNameMap(
                accounts.stream()
                        .map(SystemCompanyBankAccount::getCompanyId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        Map<Long, PmBankPaymentRecord> latestRecordByAccountId = loadLatestBankRecordByAccountId(
                accounts.stream().map(SystemCompanyBankAccount::getId).collect(Collectors.toSet())
        );
        return accounts.stream()
                .map(account -> toBankLinkSummary(
                        account,
                        companyNameMap.get(account.getCompanyId()),
                        latestRecordByAccountId.get(account.getId())
                ))
                .toList();
    }

    ExpenseBankLinkConfigVO getBankLink(Long companyBankAccountId) {
        SystemCompanyBankAccount account = recordSupport.requireCompanyBankAccount(companyBankAccountId);
        return toBankLinkConfig(account, findCompanyName(account.getCompanyId()));
    }

    ExpenseBankLinkConfigVO updateBankLink(Long companyBankAccountId, ExpenseBankLinkSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("invalid-config");
        }
        SystemCompanyBankAccount account = recordSupport.requireCompanyBankAccount(companyBankAccountId);
        String provider = trimToNull(dto.getDirectConnectProvider());
        String channel = trimToNull(dto.getDirectConnectChannel());
        if (!BANK_PROVIDER_CMB.equals(provider) || !BANK_CHANNEL_CMB_CLOUD.equals(channel)) {
            throw new IllegalArgumentException("unsupported-bank-link");
        }
        boolean enabled = Boolean.TRUE.equals(dto.getEnabled());
        if (enabled) {
            requireNotBlank(dto.getOperatorKey(), "operatorKey-required");
            requireNotBlank(dto.getCallbackSecret(), "callbackSecret-required");
        }

        account.setDirectConnectEnabled(enabled ? 1 : 0);
        account.setDirectConnectProvider(provider);
        account.setDirectConnectChannel(channel);
        account.setDirectConnectProtocol(trimToNull(dto.getDirectConnectProtocol()));
        account.setDirectConnectCustomerNo(trimToNull(dto.getDirectConnectCustomerNo()));
        account.setDirectConnectAppId(trimToNull(dto.getDirectConnectAppId()));
        account.setDirectConnectAccountAlias(trimToNull(dto.getDirectConnectAccountAlias()));
        account.setDirectConnectAuthMode(trimToNull(dto.getDirectConnectAuthMode()));
        account.setDirectConnectApiBaseUrl(trimToNull(dto.getDirectConnectApiBaseUrl()));
        account.setDirectConnectCertRef(trimToNull(dto.getDirectConnectCertRef()));
        account.setDirectConnectSecretRef(trimToNull(dto.getDirectConnectSecretRef()));
        account.setDirectConnectSignType(trimToNull(dto.getDirectConnectSignType()));
        account.setDirectConnectEncryptType(trimToNull(dto.getDirectConnectEncryptType()));
        account.setDirectConnectLastSyncAt(LocalDateTime.now());
        account.setDirectConnectLastSyncStatus(enabled ? "ENABLED" : "DISABLED");
        account.setDirectConnectLastErrorMsg(null);
        account.setDirectConnectExtJson(writeJson(Map.of(
                "operatorKey", defaultText(trimToNull(dto.getOperatorKey()), ""),
                "callbackSecret", defaultText(trimToNull(dto.getCallbackSecret()), ""),
                "publicKeyRef", defaultText(trimToNull(dto.getPublicKeyRef()), ""),
                "receiptQueryEnabled", Boolean.TRUE.equals(dto.getReceiptQueryEnabled())
        )));
        systemCompanyBankAccountMapper.updateById(account);

        if (enabled) {
            recordSupport.disableOtherEnabledBankLinks(account);
        }
        return toBankLinkConfig(
                recordSupport.requireCompanyBankAccount(companyBankAccountId),
                findCompanyName(account.getCompanyId())
        );
    }

    private ExpenseBankLinkSummaryVO toBankLinkSummary(
            SystemCompanyBankAccount account,
            String companyName,
            PmBankPaymentRecord latestRecord
    ) {
        ExpenseBankLinkSummaryVO item = new ExpenseBankLinkSummaryVO();
        item.setCompanyBankAccountId(account.getId());
        item.setCompanyId(account.getCompanyId());
        item.setCompanyName(companyName);
        item.setAccountName(account.getAccountName());
        item.setAccountNo(maskAccountNo(account.getAccountNo()));
        item.setBankName(account.getBankName());
        item.setAccountStatus(account.getStatus());
        item.setDirectConnectEnabled(isFlagEnabled(account.getDirectConnectEnabled()));
        item.setDirectConnectProvider(account.getDirectConnectProvider());
        item.setDirectConnectChannel(account.getDirectConnectChannel());
        item.setDirectConnectStatusLabel(resolveBankLinkStatusLabel(account));
        item.setLastDirectConnectStatus(resolveBankLinkSyncStatus(account));
        item.setLastReceiptStatus(resolveReceiptStatusLabel(latestRecord));
        return item;
    }

    private ExpenseBankLinkConfigVO toBankLinkConfig(SystemCompanyBankAccount account, String companyName) {
        Map<String, String> ext = readBankLinkExt(account);
        ExpenseBankLinkConfigVO item = new ExpenseBankLinkConfigVO();
        item.setCompanyBankAccountId(account.getId());
        item.setCompanyId(account.getCompanyId());
        item.setCompanyName(companyName);
        item.setAccountName(account.getAccountName());
        item.setAccountNo(account.getAccountNo());
        item.setBankName(account.getBankName());
        item.setAccountStatus(account.getStatus());
        item.setDirectConnectEnabled(isFlagEnabled(account.getDirectConnectEnabled()));
        item.setDirectConnectProvider(account.getDirectConnectProvider());
        item.setDirectConnectChannel(account.getDirectConnectChannel());
        item.setDirectConnectProtocol(account.getDirectConnectProtocol());
        item.setDirectConnectCustomerNo(account.getDirectConnectCustomerNo());
        item.setDirectConnectAppId(account.getDirectConnectAppId());
        item.setDirectConnectAccountAlias(account.getDirectConnectAccountAlias());
        item.setDirectConnectAuthMode(account.getDirectConnectAuthMode());
        item.setDirectConnectApiBaseUrl(account.getDirectConnectApiBaseUrl());
        item.setDirectConnectCertRef(account.getDirectConnectCertRef());
        item.setDirectConnectSecretRef(account.getDirectConnectSecretRef());
        item.setDirectConnectSignType(account.getDirectConnectSignType());
        item.setDirectConnectEncryptType(account.getDirectConnectEncryptType());
        item.setOperatorKey(ext.getOrDefault("operatorKey", ""));
        item.setCallbackSecret(ext.getOrDefault("callbackSecret", ""));
        item.setPublicKeyRef(ext.getOrDefault("publicKeyRef", ""));
        item.setReceiptQueryEnabled(Boolean.parseBoolean(ext.getOrDefault("receiptQueryEnabled", "false")));
        item.setLastDirectConnectStatus(resolveBankLinkSyncStatus(account));
        item.setLastDirectConnectError(account.getDirectConnectLastErrorMsg());
        return item;
    }
}
