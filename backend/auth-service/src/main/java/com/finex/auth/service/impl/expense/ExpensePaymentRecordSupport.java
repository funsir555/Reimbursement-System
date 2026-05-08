package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemCompanyBankAccount;

import java.util.List;
import java.util.Objects;

class ExpensePaymentRecordSupport extends AbstractExpensePaymentSupport {

    ExpensePaymentRecordSupport(ExpensePaymentSupportContext context) {
        super(context);
    }

    PmBankPaymentRecord requireBankPaymentRecordForCallback(ExpenseBankCallbackDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("银行回调参数不能为空");
        }
        PmBankPaymentRecord record = null;
        if (trimToNull(dto.getPushRequestNo()) != null) {
            record = pmBankPaymentRecordMapper.selectOne(
                    Wrappers.<PmBankPaymentRecord>lambdaQuery()
                            .eq(PmBankPaymentRecord::getPushRequestNo, dto.getPushRequestNo())
                            .orderByDesc(PmBankPaymentRecord::getId)
                            .last("limit 1")
            );
        }
        if (record == null && dto.getTaskId() != null) {
            record = pmBankPaymentRecordMapper.selectOne(
                    Wrappers.<PmBankPaymentRecord>lambdaQuery()
                            .eq(PmBankPaymentRecord::getTaskId, dto.getTaskId())
                            .orderByDesc(PmBankPaymentRecord::getId)
                            .last("limit 1")
            );
        }
        if (record == null && trimToNull(dto.getDocumentCode()) != null) {
            record = findLatestBankPaymentRecord(dto.getDocumentCode());
        }
        if (record == null) {
            throw new IllegalArgumentException("未匹配到银行付款记录");
        }
        return record;
    }

    PmBankPaymentRecord findLatestBankPaymentRecord(String documentCode) {
        if (trimToNull(documentCode) == null) {
            return null;
        }
        return pmBankPaymentRecordMapper.selectOne(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .eq(PmBankPaymentRecord::getDocumentCode, documentCode)
                        .orderByDesc(PmBankPaymentRecord::getId)
                        .last("limit 1")
        );
    }

    PmBankPaymentRecord findOrCreateBankPaymentRecord(
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            SystemCompanyBankAccount account
    ) {
        PmBankPaymentRecord record = findLatestBankPaymentRecord(instance.getDocumentCode());
        if (record == null) {
            record = new PmBankPaymentRecord();
            record.setTaskId(task.getId());
            record.setDocumentCode(instance.getDocumentCode());
            record.setReceiptQueryCount(0);
        }
        if (account != null) {
            record.setCompanyBankAccountId(account.getId());
            record.setBankProvider(BANK_PROVIDER_CMB);
            record.setBankChannel(BANK_CHANNEL_CMB_CLOUD);
        }
        return record;
    }

    void saveBankPaymentRecord(PmBankPaymentRecord record) {
        if (record.getId() == null) {
            pmBankPaymentRecordMapper.insert(record);
            return;
        }
        pmBankPaymentRecordMapper.updateById(record);
    }

    SystemCompanyBankAccount findActiveBankAccountForDocument(ProcessDocumentInstance instance) {
        return findActiveBankAccountForDocument(instance, true);
    }

    SystemCompanyBankAccount findActiveBankAccountForDocument(ProcessDocumentInstance instance, boolean required) {
        ExpenseSummaryAssembler.SummaryMetadata metadata = expenseSummaryAssembler
                .buildSummaryEnrichmentData(List.of(instance))
                .metadata(instance.getDocumentCode());
        String paymentCompanyId = trimToNull(metadata == null ? null : metadata.paymentCompanyId());
        if (paymentCompanyId == null) {
            if (required) {
                throw new IllegalStateException("单据未配置付款公司，无法推送银行");
            }
            return null;
        }
        List<SystemCompanyBankAccount> accounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .eq(SystemCompanyBankAccount::getCompanyId, paymentCompanyId)
                        .eq(SystemCompanyBankAccount::getStatus, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectEnabled, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectProvider, BANK_PROVIDER_CMB)
                        .eq(SystemCompanyBankAccount::getDirectConnectChannel, BANK_CHANNEL_CMB_CLOUD)
                        .orderByAsc(SystemCompanyBankAccount::getId)
        );
        if (accounts.isEmpty()) {
            if (required) {
                throw new IllegalStateException("付款公司未启用招商银行云直连账户");
            }
            return null;
        }
        if (accounts.size() > 1) {
            throw new IllegalStateException("同一公司只能启用一个招商银行云直连账户");
        }
        return accounts.get(0);
    }

    SystemCompanyBankAccount requireCompanyBankAccount(Long companyBankAccountId) {
        SystemCompanyBankAccount account = systemCompanyBankAccountMapper.selectById(companyBankAccountId);
        if (account == null) {
            throw new IllegalArgumentException("公司银行账户不存在");
        }
        return account;
    }

    void disableOtherEnabledBankLinks(SystemCompanyBankAccount currentAccount) {
        List<SystemCompanyBankAccount> companyAccounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .eq(SystemCompanyBankAccount::getCompanyId, currentAccount.getCompanyId())
                        .eq(SystemCompanyBankAccount::getDirectConnectEnabled, 1)
                        .eq(SystemCompanyBankAccount::getDirectConnectProvider, BANK_PROVIDER_CMB)
                        .eq(SystemCompanyBankAccount::getDirectConnectChannel, BANK_CHANNEL_CMB_CLOUD)
        );
        for (SystemCompanyBankAccount account : companyAccounts) {
            if (Objects.equals(account.getId(), currentAccount.getId())) {
                continue;
            }
            account.setDirectConnectEnabled(0);
            account.setDirectConnectLastSyncStatus("DISABLED");
            systemCompanyBankAccountMapper.updateById(account);
        }
    }
}
