package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.SystemBankBranchCatalog;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemBankBranchCatalogMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.ExpenseAttachmentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

abstract class AbstractExpensePaymentSupport {

    protected static final String BANK_PROVIDER_CMB = "CMB";
    protected static final String BANK_CHANNEL_CMB_CLOUD = "CMB_CLOUD";
    protected static final String BANK_PROVIDER_MANUAL = "MANUAL";
    protected static final String BANK_CHANNEL_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    protected static final String SYSTEM_OPERATOR = "SYSTEM";
    protected static final String PAYEE_ACCOUNT_COMPONENT_CODE = "payee-account";
    protected static final String BANK_PUSH_SUMMARY_COMPONENT_CODE = "bank-push-summary";

    protected static final String NODE_TYPE_PAYMENT = "PAYMENT";
    protected static final String TASK_STATUS_PENDING = "PENDING";
    protected static final String TASK_STATUS_PAUSED = "PAUSED";

    protected static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    protected static final String DOCUMENT_STATUS_PAYING = "PAYING";
    protected static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    protected static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    protected static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    protected static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    protected static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    protected static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    protected static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";

    protected static final String RECEIPT_STATUS_PENDING = "PENDING";
    protected static final String RECEIPT_STATUS_RECEIVED = "RECEIVED";
    protected static final String RECEIPT_STATUS_FAILED = "FAILED";

    protected final ExpenseDocumentReadSupport expenseDocumentReadSupport;
    protected final ExpenseSummaryAssembler expenseSummaryAssembler;
    protected final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    protected final ExpenseRelationWriteOffService expenseRelationWriteOffService;
    protected final PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    protected final ProcessDocumentTaskMapper processDocumentTaskMapper;
    protected final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    protected final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    protected final SystemBankBranchCatalogMapper systemBankBranchCatalogMapper;
    protected final SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    protected final SystemCompanyMapper systemCompanyMapper;
    protected final FinanceVendorMapper financeVendorMapper;
    protected final UserBankAccountMapper userBankAccountMapper;
    protected final ExpenseAttachmentService expenseAttachmentService;
    protected final ObjectMapper objectMapper;

    protected AbstractExpensePaymentSupport(ExpensePaymentSupportContext context) {
        this.expenseDocumentReadSupport = context.expenseDocumentReadSupport;
        this.expenseSummaryAssembler = context.expenseSummaryAssembler;
        this.expenseWorkflowRuntimeSupport = context.expenseWorkflowRuntimeSupport;
        this.expenseRelationWriteOffService = context.expenseRelationWriteOffService;
        this.pmBankPaymentRecordMapper = context.pmBankPaymentRecordMapper;
        this.processDocumentTaskMapper = context.processDocumentTaskMapper;
        this.processDocumentExpenseDetailMapper = context.processDocumentExpenseDetailMapper;
        this.processDocumentInstanceMapper = context.processDocumentInstanceMapper;
        this.systemBankBranchCatalogMapper = context.systemBankBranchCatalogMapper;
        this.systemCompanyBankAccountMapper = context.systemCompanyBankAccountMapper;
        this.systemCompanyMapper = context.systemCompanyMapper;
        this.financeVendorMapper = context.financeVendorMapper;
        this.userBankAccountMapper = context.userBankAccountMapper;
        this.expenseAttachmentService = context.expenseAttachmentService;
        this.objectMapper = context.objectMapper;
    }

    protected Map<String, List<ProcessDocumentExpenseDetail>> loadExpenseDetailMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    protected Map<String, SystemBankBranchCatalog> loadBranchCatalogMap(Set<String> branchCodes) {
        if (branchCodes == null || branchCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemBankBranchCatalogMapper.selectList(
                Wrappers.<SystemBankBranchCatalog>lambdaQuery()
                        .in(SystemBankBranchCatalog::getBranchCode, branchCodes)
        ).stream().collect(Collectors.toMap(
                SystemBankBranchCatalog::getBranchCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected Map<String, PmBankPaymentRecord> loadLatestBankRecordMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return pmBankPaymentRecordMapper.selectList(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .in(PmBankPaymentRecord::getDocumentCode, documentCodes)
                        .orderByDesc(PmBankPaymentRecord::getId)
        ).stream().collect(Collectors.toMap(
                PmBankPaymentRecord::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected Map<Long, PmBankPaymentRecord> loadLatestBankRecordByAccountId(Set<Long> companyBankAccountIds) {
        if (companyBankAccountIds == null || companyBankAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pmBankPaymentRecordMapper.selectList(
                Wrappers.<PmBankPaymentRecord>lambdaQuery()
                        .in(PmBankPaymentRecord::getCompanyBankAccountId, companyBankAccountIds)
                        .orderByDesc(PmBankPaymentRecord::getId)
        ).stream().collect(Collectors.toMap(
                PmBankPaymentRecord::getCompanyBankAccountId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected Map<Long, String> loadCompanyBankAccountNameMap(Set<Long> companyBankAccountIds) {
        if (companyBankAccountIds == null || companyBankAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemCompanyBankAccountMapper.selectBatchIds(companyBankAccountIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        SystemCompanyBankAccount::getId,
                        this::buildCompanyBankAccountName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected Map<String, String> buildCompanyNameMap(Set<String> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                item -> defaultText(trimToNull(item.getCompanyName()), item.getCompanyId()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    protected String findCompanyName(String companyId) {
        if (trimToNull(companyId) == null) {
            return "";
        }
        SystemCompany company = systemCompanyMapper.selectOne(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getCompanyId, companyId)
                        .last("limit 1")
        );
        return company == null ? companyId : defaultText(trimToNull(company.getCompanyName()), companyId);
    }

    protected Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

    protected Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
        }
    }

    protected String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize data", ex);
        }
    }

    protected Object extractFirstBusinessComponentRawValue(
            Map<String, Object> schema,
            Map<String, Object> formData,
            String componentCode
    ) {
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
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(props.get("componentCode")), componentCode)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            Object value = formData.get(fieldKey);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected String resolvePayeeBankProvince(
            PaymentReceiverInfo receiverInfo,
            Map<String, SystemBankBranchCatalog> branchCatalogMap
    ) {
        if (receiverInfo == null) {
            return null;
        }
        SystemBankBranchCatalog branch = resolveBranchCatalog(receiverInfo, branchCatalogMap);
        return firstNonBlank(branch == null ? null : branch.getProvince(), receiverInfo.province());
    }

    protected String resolvePayeeBankCity(
            PaymentReceiverInfo receiverInfo,
            Map<String, SystemBankBranchCatalog> branchCatalogMap
    ) {
        if (receiverInfo == null) {
            return null;
        }
        SystemBankBranchCatalog branch = resolveBranchCatalog(receiverInfo, branchCatalogMap);
        return firstNonBlank(branch == null ? null : branch.getCity(), receiverInfo.city());
    }

    protected SystemBankBranchCatalog resolveBranchCatalog(
            PaymentReceiverInfo receiverInfo,
            Map<String, SystemBankBranchCatalog> branchCatalogMap
    ) {
        if (receiverInfo == null || branchCatalogMap == null || branchCatalogMap.isEmpty()) {
            return null;
        }
        String branchCode = receiverInfo.branchCode();
        return branchCode == null ? null : branchCatalogMap.get(branchCode);
    }

    protected String resolveReceiptStatusLabel(PmBankPaymentRecord record) {
        if (record == null) {
            return "待回单";
        }
        if (isFlagEnabled(record.getManualPaid()) && trimToNull(record.getReceiptAttachmentId()) == null) {
            return "待回单";
        }
        return switch (defaultText(trimToNull(record.getReceiptStatus()), RECEIPT_STATUS_PENDING)) {
            case RECEIPT_STATUS_RECEIVED -> "已收回单";
            case RECEIPT_STATUS_FAILED -> "回单失败";
            default -> "待回单";
        };
    }

    protected String resolveBankLinkStatusLabel(SystemCompanyBankAccount account) {
        if (!isFlagEnabled(account.getDirectConnectEnabled())) {
            return "未启用";
        }
        if (!BANK_PROVIDER_CMB.equals(trimToNull(account.getDirectConnectProvider()))
                || !BANK_CHANNEL_CMB_CLOUD.equals(trimToNull(account.getDirectConnectChannel()))) {
            return "配置异常";
        }
        return "已启用";
    }

    protected String resolveBankLinkSyncStatus(SystemCompanyBankAccount account) {
        String status = trimToNull(account.getDirectConnectLastSyncStatus());
        return status == null ? "未推送" : status;
    }

    protected boolean isReceiptQueryEnabled(SystemCompanyBankAccount account) {
        if (account == null) {
            return false;
        }
        return Boolean.parseBoolean(readBankLinkExt(account).getOrDefault("receiptQueryEnabled", "false"));
    }

    protected Map<String, String> readBankLinkExt(SystemCompanyBankAccount account) {
        Map<String, Object> ext = readMap(account == null ? null : account.getDirectConnectExtJson());
        Map<String, String> result = new LinkedHashMap<>();
        ext.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    protected String buildCompanyBankAccountName(SystemCompanyBankAccount account) {
        if (account == null) {
            return null;
        }
        String tailNo = trimToNull(account.getAccountNo());
        String suffix = tailNo == null || tailNo.length() <= 4 ? tailNo : tailNo.substring(tailNo.length() - 4);
        return account.getAccountName() + (suffix == null ? "" : "（尾号 " + suffix + "）");
    }

    protected String buildBankPushRequestNo(String documentCode) {
        return defaultText(documentCode, "DOC")
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected LocalDateTime parseFlexibleDateTime(String rawValue, LocalDateTime defaultValue) {
        String normalized = trimToNull(rawValue);
        if (normalized == null) {
            return defaultValue;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
                // try next formatter
            }
        }
        return defaultValue;
    }

    protected String normalizePaymentOrderStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return DOCUMENT_STATUS_PENDING_PAYMENT;
        }
        return switch (normalized) {
            case DOCUMENT_STATUS_PENDING_PAYMENT,
                    DOCUMENT_STATUS_PAYING,
                    DOCUMENT_STATUS_PAYMENT_COMPLETED,
                    DOCUMENT_STATUS_PAYMENT_FINISHED,
                    DOCUMENT_STATUS_PAYMENT_EXCEPTION -> normalized;
            default -> DOCUMENT_STATUS_PENDING_PAYMENT;
        };
    }

    protected boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    protected String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "申请单";
            case "loan" -> "借款单";
            case "contract" -> "合同单";
            default -> "pending";
        };
    }

    protected String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "待支付";
            case DOCUMENT_STATUS_PAYING -> "支付中";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "已支付";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "已完成";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "支付异常";
            case DOCUMENT_STATUS_APPROVED, DOCUMENT_STATUS_COMPLETED -> "已完成";
            case DOCUMENT_STATUS_REJECTED -> "已驳回";
            case "DRAFT" -> "草稿";
            case DOCUMENT_STATUS_EXCEPTION -> "流程异常";
            default -> "pending";
        };
    }

    protected String formatTime(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    protected void requireNotBlank(String value, String message) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException(message);
        }
    }

    protected boolean isFlagEnabled(Integer value) {
        return value != null && value == 1;
    }

    protected String maskAccountNo(String accountNo) {
        String normalized = trimToNull(accountNo);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 8) {
            return normalized;
        }
        return normalized.substring(0, 4) + " **** " + normalized.substring(normalized.length() - 4);
    }

    protected PaymentReceiverInfo emptyPaymentReceiverInfo() {
        return new PaymentReceiverInfo(null, null, null, null, null, null, null);
    }

    protected String trimObjectToNull(Object value) {
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    protected Long toLong(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected String firstNonBlank(String... values) {
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

    protected String defaultText(String value, String fallback) {
        return trimToNull(value) == null ? fallback : value.trim();
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    protected static final class PaymentReceiverInfo {
        private final String receiverName;
        private final String accountNo;
        private final String bankName;
        private final String accountName;
        private final String branchCode;
        private final String province;
        private final String city;

        PaymentReceiverInfo(
                String receiverName,
                String accountNo,
                String bankName,
                String accountName,
                String branchCode,
                String province,
                String city
        ) {
            this.receiverName = receiverName;
            this.accountNo = accountNo;
            this.bankName = bankName;
            this.accountName = accountName;
            this.branchCode = branchCode;
            this.province = province;
            this.city = city;
        }

        boolean isEmpty() {
            return receiverName == null
                    && accountNo == null
                    && bankName == null
                    && accountName == null
                    && branchCode == null
                    && province == null
                    && city == null;
        }

        PaymentReceiverInfo merge(PaymentReceiverInfo fallback) {
            if (fallback == null) {
                return this;
            }
            return new PaymentReceiverInfo(
                    localFirstNonBlank(receiverName, fallback.receiverName),
                    localFirstNonBlank(accountNo, fallback.accountNo),
                    localFirstNonBlank(bankName, fallback.bankName),
                    localFirstNonBlank(accountName, fallback.accountName),
                    localFirstNonBlank(branchCode, fallback.branchCode),
                    localFirstNonBlank(province, fallback.province),
                    localFirstNonBlank(city, fallback.city)
            );
        }

        PaymentReceiverInfo withAccountNo(String nextAccountNo) {
            return new PaymentReceiverInfo(receiverName, nextAccountNo, bankName, accountName, branchCode, province, city);
        }

        String receiverName() {
            return receiverName;
        }

        String accountNo() {
            return accountNo;
        }

        String bankName() {
            return bankName;
        }

        String accountName() {
            return accountName;
        }

        String branchCode() {
            return branchCode;
        }

        String province() {
            return province;
        }

        String city() {
            return city;
        }

        private String localFirstNonBlank(String... values) {
            if (values == null) {
                return null;
            }
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
            return null;
        }
    }
}
