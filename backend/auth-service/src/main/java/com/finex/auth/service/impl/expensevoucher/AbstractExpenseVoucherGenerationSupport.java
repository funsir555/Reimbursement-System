package com.finex.auth.service.impl.expensevoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseVoucherEntrySnapshotVO;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushResultVO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingSaveDTO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingVO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.entity.ExpVoucherPushBatch;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ExpVoucherPushEntry;
import com.finex.auth.entity.ExpVoucherSubjectMapping;
import com.finex.auth.entity.ExpVoucherTemplatePolicy;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ExpVoucherPushBatchMapper;
import com.finex.auth.mapper.ExpVoucherPushDocumentMapper;
import com.finex.auth.mapper.ExpVoucherPushEntryMapper;
import com.finex.auth.mapper.ExpVoucherSubjectMappingMapper;
import com.finex.auth.mapper.ExpVoucherTemplatePolicyMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVoucherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractExpenseVoucherGenerationSupport {

    protected static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    protected static final String PUSH_STATUS_SUCCESS = "SUCCESS";
    protected static final String PUSH_STATUS_FAILED = "FAILED";
    protected static final String PUSH_STATUS_UNPUSHED = "UNPUSHED";
    protected static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    protected static final String DEFAULT_VOUCHER_TYPE = "GENERAL";
    protected static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    protected static final DateTimeFormatter BATCH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    protected static final List<OptionSeed> VOUCHER_TYPE_SEEDS = List.of(
            new OptionSeed("GENERAL", "????"),
            new OptionSeed("RECEIPT", "????"),
            new OptionSeed("PAYMENT", "????"),
            new OptionSeed("TRANSFER", "????")
    );

    protected static final List<OptionSeed> ACCOUNT_SEEDS = List.of(
            new OptionSeed("1001", "1001 ????"),
            new OptionSeed("1002", "1002 ????"),
            new OptionSeed("1122", "1122 ?????"),
            new OptionSeed("2202", "2202 ?????"),
            new OptionSeed("2221", "2221 ????"),
            new OptionSeed("660100", "660100 ???"),
            new OptionSeed("660200", "660200 ???"),
            new OptionSeed("660300", "660300 ???"),
            new OptionSeed("660500", "660500 ???")
    );

    protected final ExpVoucherTemplatePolicyMapper templatePolicyMapper;
    protected final ExpVoucherSubjectMappingMapper subjectMappingMapper;
    protected final ExpVoucherPushBatchMapper pushBatchMapper;
    protected final ExpVoucherPushDocumentMapper pushDocumentMapper;
    protected final ExpVoucherPushEntryMapper pushEntryMapper;
    protected final ProcessDocumentInstanceMapper documentInstanceMapper;
    protected final ProcessDocumentExpenseDetailMapper expenseDetailMapper;
    protected final ProcessDocumentTemplateMapper documentTemplateMapper;
    protected final ProcessExpenseTypeMapper expenseTypeMapper;
    protected final SystemCompanyMapper systemCompanyMapper;
    protected final UserMapper userMapper;
    protected final GlAccvouchMapper glAccvouchMapper;
    protected final FinanceVoucherService financeVoucherService;
    protected final ObjectMapper objectMapper;
    public static final class Dependencies {
        final ExpVoucherTemplatePolicyMapper templatePolicyMapper;
        final ExpVoucherSubjectMappingMapper subjectMappingMapper;
        final ExpVoucherPushBatchMapper pushBatchMapper;
        final ExpVoucherPushDocumentMapper pushDocumentMapper;
        final ExpVoucherPushEntryMapper pushEntryMapper;
        final ProcessDocumentInstanceMapper documentInstanceMapper;
        final ProcessDocumentExpenseDetailMapper expenseDetailMapper;
        final ProcessDocumentTemplateMapper documentTemplateMapper;
        final ProcessExpenseTypeMapper expenseTypeMapper;
        final SystemCompanyMapper systemCompanyMapper;
        final UserMapper userMapper;
        final GlAccvouchMapper glAccvouchMapper;
        final FinanceVoucherService financeVoucherService;
        final ObjectMapper objectMapper;

        public Dependencies(
                ExpVoucherTemplatePolicyMapper templatePolicyMapper,
                ExpVoucherSubjectMappingMapper subjectMappingMapper,
                ExpVoucherPushBatchMapper pushBatchMapper,
                ExpVoucherPushDocumentMapper pushDocumentMapper,
                ExpVoucherPushEntryMapper pushEntryMapper,
                ProcessDocumentInstanceMapper documentInstanceMapper,
                ProcessDocumentExpenseDetailMapper expenseDetailMapper,
                ProcessDocumentTemplateMapper documentTemplateMapper,
                ProcessExpenseTypeMapper expenseTypeMapper,
                SystemCompanyMapper systemCompanyMapper,
                UserMapper userMapper,
                GlAccvouchMapper glAccvouchMapper,
                FinanceVoucherService financeVoucherService,
                ObjectMapper objectMapper
        ) {
            this.templatePolicyMapper = templatePolicyMapper;
            this.subjectMappingMapper = subjectMappingMapper;
            this.pushBatchMapper = pushBatchMapper;
            this.pushDocumentMapper = pushDocumentMapper;
            this.pushEntryMapper = pushEntryMapper;
            this.documentInstanceMapper = documentInstanceMapper;
            this.expenseDetailMapper = expenseDetailMapper;
            this.documentTemplateMapper = documentTemplateMapper;
            this.expenseTypeMapper = expenseTypeMapper;
            this.systemCompanyMapper = systemCompanyMapper;
            this.userMapper = userMapper;
            this.glAccvouchMapper = glAccvouchMapper;
            this.financeVoucherService = financeVoucherService;
            this.objectMapper = objectMapper;
        }
    }

    protected AbstractExpenseVoucherGenerationSupport(Dependencies dependencies) {
        this.templatePolicyMapper = dependencies.templatePolicyMapper;
        this.subjectMappingMapper = dependencies.subjectMappingMapper;
        this.pushBatchMapper = dependencies.pushBatchMapper;
        this.pushDocumentMapper = dependencies.pushDocumentMapper;
        this.pushEntryMapper = dependencies.pushEntryMapper;
        this.documentInstanceMapper = dependencies.documentInstanceMapper;
        this.expenseDetailMapper = dependencies.expenseDetailMapper;
        this.documentTemplateMapper = dependencies.documentTemplateMapper;
        this.expenseTypeMapper = dependencies.expenseTypeMapper;
        this.systemCompanyMapper = dependencies.systemCompanyMapper;
        this.userMapper = dependencies.userMapper;
        this.glAccvouchMapper = dependencies.glAccvouchMapper;
        this.financeVoucherService = dependencies.financeVoucherService;
        this.objectMapper = dependencies.objectMapper;
    }

    public static Dependencies dependencies(
            ExpVoucherTemplatePolicyMapper templatePolicyMapper,
            ExpVoucherSubjectMappingMapper subjectMappingMapper,
            ExpVoucherPushBatchMapper pushBatchMapper,
            ExpVoucherPushDocumentMapper pushDocumentMapper,
            ExpVoucherPushEntryMapper pushEntryMapper,
            ProcessDocumentInstanceMapper documentInstanceMapper,
            ProcessDocumentExpenseDetailMapper expenseDetailMapper,
            ProcessDocumentTemplateMapper documentTemplateMapper,
            ProcessExpenseTypeMapper expenseTypeMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            GlAccvouchMapper glAccvouchMapper,
            FinanceVoucherService financeVoucherService,
            ObjectMapper objectMapper
    ) {
        return new Dependencies(
                templatePolicyMapper,
                subjectMappingMapper,
                pushBatchMapper,
                pushDocumentMapper,
                pushEntryMapper,
                documentInstanceMapper,
                expenseDetailMapper,
                documentTemplateMapper,
                expenseTypeMapper,
                systemCompanyMapper,
                userMapper,
                glAccvouchMapper,
                financeVoucherService,
                objectMapper
        );
    }

    protected ExpVoucherTemplatePolicy requireEnabledTemplatePolicy(String companyId, String templateCode) {
        ExpVoucherTemplatePolicy policy = templatePolicyMapper.selectOne(
                Wrappers.<ExpVoucherTemplatePolicy>lambdaQuery()
                        .eq(ExpVoucherTemplatePolicy::getCompanyId, companyId)
                        .eq(ExpVoucherTemplatePolicy::getTemplateCode, templateCode)
                        .eq(ExpVoucherTemplatePolicy::getEnabled, 1)
                        .last("limit 1")
        );
        if (policy == null) {
            throw new IllegalStateException("褰撳墠鍏徃鍜屾姤閿€妯℃澘鏈厤缃粺涓€璐锋柟绉戠洰绛栫暐");
        }
        return policy;
    }

    protected List<ExpVoucherSubjectMapping> listEnabledSubjectMappings(String companyId, String templateCode) {
        return subjectMappingMapper.selectList(
                Wrappers.<ExpVoucherSubjectMapping>lambdaQuery()
                        .eq(ExpVoucherSubjectMapping::getCompanyId, companyId)
                        .eq(ExpVoucherSubjectMapping::getTemplateCode, templateCode)
                        .eq(ExpVoucherSubjectMapping::getEnabled, 1)
        );
    }

    protected LinkedHashMap<String, BigDecimal> aggregateExpenseAmounts(List<ProcessDocumentExpenseDetail> details) {
        LinkedHashMap<String, BigDecimal> result = new LinkedHashMap<>();
        for (ProcessDocumentExpenseDetail detail : details) {
            String expenseTypeCode = hasText(detail.getExpenseTypeCode()) ? trim(detail.getExpenseTypeCode()) : fallbackExpenseTypeCode(detail.getFormDataJson());
            if (expenseTypeCode == null) {
                continue;
            }
            BigDecimal amount = resolveDetailAmount(detail);
            if (amount.compareTo(ZERO) <= 0) {
                continue;
            }
            result.merge(expenseTypeCode, amount, BigDecimal::add);
        }
        return result;
    }

    protected BigDecimal resolveDetailAmount(ProcessDocumentExpenseDetail detail) {
        BigDecimal actualPaymentAmount = zero(detail.getActualPaymentAmount());
        if (actualPaymentAmount.compareTo(ZERO) > 0) {
            return actualPaymentAmount;
        }
        return zero(detail.getInvoiceAmount());
    }

    protected String fallbackExpenseTypeCode(String formDataJson) {
        Object value = readMap(formDataJson).get("expenseTypeCode");
        return value == null ? null : trim(String.valueOf(value));
    }

    protected String buildExpenseSummary(List<ProcessDocumentExpenseDetail> details, Map<String, String> expenseTypeMap) {
        LinkedHashMap<String, BigDecimal> summary = aggregateExpenseAmounts(details);
        if (summary.isEmpty()) {
            return "-";
        }
        return summary.entrySet().stream()
                .map(entry -> expenseTypeMap.getOrDefault(entry.getKey(), entry.getKey()) + " " + zero(entry.getValue()).toPlainString())
                .collect(Collectors.joining(" / "));
    }

    protected boolean canPush(ProcessDocumentInstance document, ExpVoucherPushDocument pushDocument, String companyId, List<ProcessDocumentExpenseDetail> details) {
        if (!DOCUMENT_STATUS_APPROVED.equals(trim(document.getStatus()))) {
            return false;
        }
        if (!hasText(companyId)) {
            return false;
        }
        if (pushDocument != null && PUSH_STATUS_SUCCESS.equals(pushDocument.getPushStatus())) {
            return false;
        }
        ExpVoucherTemplatePolicy policy = templatePolicyMapper.selectOne(
                Wrappers.<ExpVoucherTemplatePolicy>lambdaQuery()
                        .eq(ExpVoucherTemplatePolicy::getCompanyId, companyId)
                        .eq(ExpVoucherTemplatePolicy::getTemplateCode, document.getTemplateCode())
                        .eq(ExpVoucherTemplatePolicy::getEnabled, 1)
                        .last("limit 1")
        );
        if (policy == null) {
            return false;
        }
        Map<String, ExpVoucherSubjectMapping> subjectMap = listEnabledSubjectMappings(companyId, document.getTemplateCode()).stream()
                .collect(Collectors.toMap(ExpVoucherSubjectMapping::getExpenseTypeCode, Function.identity(), (left, right) -> left));
        for (String expenseTypeCode : aggregateExpenseAmounts(details).keySet()) {
            if (!subjectMap.containsKey(expenseTypeCode)) {
                return false;
            }
        }
        return true;
    }
    protected List<SystemCompany> listCompanies() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        );
    }

    protected List<ProcessDocumentTemplate> listTemplates() {
        return documentTemplateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getEnabled, 1)
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        );
    }

    protected List<ProcessExpenseType> listExpenseTypes() {
        return expenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    protected List<ProcessDocumentInstance> listApprovedDocuments() {
        return documentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getStatus, DOCUMENT_STATUS_APPROVED)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getId)
        );
    }

    protected List<ExpVoucherPushDocument> listPushDocuments() {
        return pushDocumentMapper.selectList(
                Wrappers.<ExpVoucherPushDocument>lambdaQuery()
                        .orderByDesc(ExpVoucherPushDocument::getPushedAt, ExpVoucherPushDocument::getId)
        );
    }

    protected List<ProcessDocumentExpenseDetail> listExpenseDetails(String documentCode) {
        return expenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        );
    }

    protected ExpVoucherPushDocument findPushDocument(String companyId, String documentCode) {
        return pushDocumentMapper.selectOne(
                Wrappers.<ExpVoucherPushDocument>lambdaQuery()
                        .eq(ExpVoucherPushDocument::getCompanyId, companyId)
                        .eq(ExpVoucherPushDocument::getDocumentCode, documentCode)
                        .last("limit 1")
        );
    }

    protected Map<String, String> companyNameMap() {
        return listCompanies().stream().collect(Collectors.toMap(SystemCompany::getCompanyId, SystemCompany::getCompanyName, (left, right) -> left, LinkedHashMap::new));
    }

    protected Map<String, String> expenseTypeNameMap() {
        return listExpenseTypes().stream().collect(Collectors.toMap(ProcessExpenseType::getExpenseCode, item -> defaultText(item.getExpenseName(), item.getExpenseCode()), (left, right) -> left, LinkedHashMap::new));
    }

    protected List<FinanceVoucherOptionVO> loadAccountOptions() {
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        for (OptionSeed seed : ACCOUNT_SEEDS) {
            labels.put(seed.value, seed.label);
        }
        List<Object> dbCodes = glAccvouchMapper.selectObjs(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .select(GlAccvouch::getCcode)
                        .isNotNull(GlAccvouch::getCcode)
                        .groupBy(GlAccvouch::getCcode)
                        .orderByAsc(GlAccvouch::getCcode)
                        .last("limit 200")
        );
        for (Object dbCode : dbCodes) {
            if (dbCode == null) {
                continue;
            }
            String code = trim(String.valueOf(dbCode));
            if (code != null && !labels.containsKey(code)) {
                labels.put(code, code + " 浼氳绉戠洰");
            }
        }
        return labels.entrySet().stream().map(entry -> option(entry.getKey(), entry.getValue())).toList();
    }

    protected String resolveDefaultCompanyId(Long currentUserId, List<SystemCompany> companies) {
        if (companies.isEmpty()) {
            return null;
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser != null && hasText(currentUser.getCompanyId())) {
            String companyId = trim(currentUser.getCompanyId());
            boolean exists = companies.stream().anyMatch(item -> Objects.equals(item.getCompanyId(), companyId));
            if (exists) {
                return companyId;
            }
        }
        return companies.get(0).getCompanyId();
    }

    protected String resolveLatestBatchNo() {
        ExpVoucherPushBatch latestBatch = pushBatchMapper.selectOne(
                Wrappers.<ExpVoucherPushBatch>lambdaQuery()
                        .orderByDesc(ExpVoucherPushBatch::getCreatedAt, ExpVoucherPushBatch::getId)
                        .last("limit 1")
        );
        return latestBatch == null ? null : latestBatch.getBatchNo();
    }

    protected String resolveDocumentCompanyId(ProcessDocumentInstance document) {
        Map<String, Object> formData = readMap(document.getFormDataJson());
        Map<String, Object> schema = readMap(document.getFormSchemaSnapshotJson());
        String fieldKey = findBusinessComponentFieldKey(schema, PAYMENT_COMPANY_COMPONENT_CODE);
        if (fieldKey != null && formData.get(fieldKey) != null) {
            return trim(String.valueOf(formData.get(fieldKey)));
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (entry.getKey() != null && entry.getKey().startsWith(PAYMENT_COMPANY_COMPONENT_CODE) && entry.getValue() != null) {
                return trim(String.valueOf(entry.getValue()));
            }
        }
        User submitter = document.getSubmitterUserId() == null ? null : userMapper.selectById(document.getSubmitterUserId());
        return submitter == null ? null : trim(submitter.getCompanyId());
    }

    protected String requireDocumentCompanyId(ProcessDocumentInstance document) {
        String companyId = resolveDocumentCompanyId(document);
        if (companyId == null) {
            throw new IllegalStateException("鍗曟嵁鏈瘑鍒埌浠樻鍏徃锛屾棤娉曟帹閫佸嚟璇?");
        }
        return companyId;
    }

    protected String findBusinessComponentFieldKey(Map<String, Object> schema, String componentCode) {
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return null;
        }
        for (Object blockItem : blocks) {
            if (!(blockItem instanceof Map<?, ?> block)) {
                continue;
            }
            if (!Objects.equals("BUSINESS_COMPONENT", block.get("kind"))) {
                continue;
            }
            Object props = block.get("props");
            if (!(props instanceof Map<?, ?> propMap)) {
                continue;
            }
            if (Objects.equals(componentCode, propMap.get("componentCode")) && block.get("fieldKey") != null) {
                return String.valueOf(block.get("fieldKey"));
            }
        }
        return null;
    }

    protected Map<String, Object> readMap(String json) {
        if (!hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    protected LocalDate resolveBusinessDate(ProcessDocumentInstance document) {
        if (document.getFinishedAt() != null) {
            return document.getFinishedAt().toLocalDate();
        }
        if (document.getUpdatedAt() != null) {
            return document.getUpdatedAt().toLocalDate();
        }
        if (document.getCreatedAt() != null) {
            return document.getCreatedAt().toLocalDate();
        }
        return LocalDate.now();
    }

    protected void validateTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentId) {
        if (!hasText(dto.getCompanyId()) || systemCompanyMapper.selectById(trim(dto.getCompanyId())) == null) {
            throw new IllegalArgumentException("鍏徃涓嶅瓨鍦紝鏃犳硶淇濆瓨妯℃澘绉戠洰绛栫暐");
        }
        if (!existsTemplate(dto.getTemplateCode())) {
            throw new IllegalArgumentException("鎶ラ攢妯℃澘涓嶅瓨鍦紝鏃犳硶淇濆瓨妯℃澘绉戠洰绛栫暐");
        }
        ExpVoucherTemplatePolicy duplicate = templatePolicyMapper.selectOne(
                Wrappers.<ExpVoucherTemplatePolicy>lambdaQuery()
                        .eq(ExpVoucherTemplatePolicy::getCompanyId, trim(dto.getCompanyId()))
                        .eq(ExpVoucherTemplatePolicy::getTemplateCode, trim(dto.getTemplateCode()))
                        .ne(currentId != null, ExpVoucherTemplatePolicy::getId, currentId)
                        .last("limit 1")
        );
        if (duplicate != null) {
            throw new IllegalArgumentException("鍚屼竴鍏徃鍜屾姤閿€妯℃澘鍙兘缁存姢涓€濂楃粺涓€璐锋柟绛栫暐");
        }
    }

    protected void validateSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentId) {
        if (!hasText(dto.getCompanyId()) || systemCompanyMapper.selectById(trim(dto.getCompanyId())) == null) {
            throw new IllegalArgumentException("鍏徃涓嶅瓨鍦紝鏃犳硶淇濆瓨璐圭敤绫诲瀷鏄犲皠");
        }
        if (!existsTemplate(dto.getTemplateCode())) {
            throw new IllegalArgumentException("鎶ラ攢妯℃澘涓嶅瓨鍦紝鏃犳硶淇濆瓨璐圭敤绫诲瀷鏄犲皠");
        }
        if (!existsExpenseType(dto.getExpenseTypeCode())) {
            throw new IllegalArgumentException("璐圭敤绫诲瀷涓嶅瓨鍦紝鏃犳硶淇濆瓨绉戠洰鏄犲皠");
        }
        ExpVoucherSubjectMapping duplicate = subjectMappingMapper.selectOne(
                Wrappers.<ExpVoucherSubjectMapping>lambdaQuery()
                        .eq(ExpVoucherSubjectMapping::getCompanyId, trim(dto.getCompanyId()))
                        .eq(ExpVoucherSubjectMapping::getTemplateCode, trim(dto.getTemplateCode()))
                        .eq(ExpVoucherSubjectMapping::getExpenseTypeCode, trim(dto.getExpenseTypeCode()))
                        .ne(currentId != null, ExpVoucherSubjectMapping::getId, currentId)
                        .last("limit 1")
        );
        if (duplicate != null) {
            throw new IllegalArgumentException("鍚屼竴鍏徃銆佹姤閿€妯℃澘鍜岃垂鐢ㄧ被鍨嬩笉鑳介噸澶嶇淮鎶ゅ€熸柟绉戠洰");
        }
    }

    protected void applyTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, ExpVoucherTemplatePolicy entity, String currentUsername) {
        entity.setCompanyId(trim(dto.getCompanyId()));
        entity.setTemplateCode(trim(dto.getTemplateCode()));
        entity.setTemplateName(resolveTemplateName(dto.getTemplateCode(), dto.getTemplateName()));
        entity.setCreditAccountCode(trim(dto.getCreditAccountCode()));
        entity.setCreditAccountName(defaultText(trim(dto.getCreditAccountName()), resolveAccountName(dto.getCreditAccountCode())));
        entity.setVoucherType(defaultText(trim(dto.getVoucherType()), DEFAULT_VOUCHER_TYPE));
        entity.setSummaryRule(trim(dto.getSummaryRule()));
        entity.setEnabled(dto.getEnabled() == null ? 1 : (dto.getEnabled() == 0 ? 0 : 1));
        entity.setUpdatedBy(currentUsername);
    }

    protected void applySubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, ExpVoucherSubjectMapping entity, String currentUsername) {
        entity.setCompanyId(trim(dto.getCompanyId()));
        entity.setTemplateCode(trim(dto.getTemplateCode()));
        entity.setTemplateName(resolveTemplateName(dto.getTemplateCode(), dto.getTemplateName()));
        entity.setExpenseTypeCode(trim(dto.getExpenseTypeCode()));
        entity.setExpenseTypeName(resolveExpenseTypeName(dto.getExpenseTypeCode(), dto.getExpenseTypeName()));
        entity.setDebitAccountCode(trim(dto.getDebitAccountCode()));
        entity.setDebitAccountName(defaultText(trim(dto.getDebitAccountName()), resolveAccountName(dto.getDebitAccountCode())));
        entity.setEnabled(dto.getEnabled() == null ? 1 : (dto.getEnabled() == 0 ? 0 : 1));
        entity.setUpdatedBy(currentUsername);
    }

    protected boolean existsTemplate(String templateCode) {
        return documentTemplateMapper.selectOne(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateCode, trim(templateCode))
                        .last("limit 1")
        ) != null;
    }

    protected boolean existsExpenseType(String expenseTypeCode) {
        return expenseTypeMapper.selectOne(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getExpenseCode, trim(expenseTypeCode))
                        .last("limit 1")
        ) != null;
    }

    protected String resolveTemplateName(String templateCode, String templateName) {
        if (hasText(templateName)) {
            return trim(templateName);
        }
        ProcessDocumentTemplate template = documentTemplateMapper.selectOne(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateCode, trim(templateCode))
                        .last("limit 1")
        );
        return template == null ? trim(templateCode) : defaultText(template.getTemplateName(), templateCode);
    }

    protected String resolveExpenseTypeName(String expenseTypeCode, String expenseTypeName) {
        if (hasText(expenseTypeName)) {
            return trim(expenseTypeName);
        }
        ProcessExpenseType expenseType = expenseTypeMapper.selectOne(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getExpenseCode, trim(expenseTypeCode))
                        .last("limit 1")
        );
        return expenseType == null ? trim(expenseTypeCode) : defaultText(expenseType.getExpenseName(), expenseTypeCode);
    }

    protected ExpVoucherTemplatePolicy requireTemplatePolicy(Long id) {
        ExpVoucherTemplatePolicy entity = templatePolicyMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("妯℃澘绉戠洰绛栫暐涓嶅瓨鍦?");
        }
        return entity;
    }

    protected ExpVoucherSubjectMapping requireSubjectMapping(Long id) {
        ExpVoucherSubjectMapping entity = subjectMappingMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("璐圭敤绫诲瀷绉戠洰鏄犲皠涓嶅瓨鍦?");
        }
        return entity;
    }

    protected ExpenseVoucherTemplatePolicyVO toTemplatePolicyVO(ExpVoucherTemplatePolicy entity, Map<String, String> companyMap) {
        ExpenseVoucherTemplatePolicyVO vo = new ExpenseVoucherTemplatePolicyVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyMap.getOrDefault(entity.getCompanyId(), entity.getCompanyId()));
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setCreditAccountCode(entity.getCreditAccountCode());
        vo.setCreditAccountName(defaultText(entity.getCreditAccountName(), resolveAccountName(entity.getCreditAccountCode())));
        vo.setVoucherType(entity.getVoucherType());
        vo.setVoucherTypeLabel(resolveVoucherTypeLabel(entity.getVoucherType()));
        vo.setSummaryRule(entity.getSummaryRule());
        vo.setEnabled(Objects.equals(entity.getEnabled(), 1));
        vo.setUpdatedAt(formatDateTime(entity.getUpdatedAt()));
        return vo;
    }

    protected ExpenseVoucherSubjectMappingVO toSubjectMappingVO(ExpVoucherSubjectMapping entity, Map<String, String> companyMap) {
        ExpenseVoucherSubjectMappingVO vo = new ExpenseVoucherSubjectMappingVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyMap.getOrDefault(entity.getCompanyId(), entity.getCompanyId()));
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setExpenseTypeCode(entity.getExpenseTypeCode());
        vo.setExpenseTypeName(entity.getExpenseTypeName());
        vo.setDebitAccountCode(entity.getDebitAccountCode());
        vo.setDebitAccountName(defaultText(entity.getDebitAccountName(), resolveAccountName(entity.getDebitAccountCode())));
        vo.setEnabled(Objects.equals(entity.getEnabled(), 1));
        vo.setUpdatedAt(formatDateTime(entity.getUpdatedAt()));
        return vo;
    }

    protected ExpenseVoucherGeneratedRecordVO toGeneratedRecordVO(ExpVoucherPushDocument entity, Map<String, String> companyMap) {
        ExpenseVoucherGeneratedRecordVO vo = new ExpenseVoucherGeneratedRecordVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyMap.getOrDefault(entity.getCompanyId(), entity.getCompanyId()));
        vo.setBatchNo(entity.getBatchNo());
        vo.setDocumentCode(entity.getDocumentCode());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setSubmitterName(entity.getSubmitterName());
        vo.setTotalAmount(zero(entity.getTotalAmount()));
        vo.setPushStatus(entity.getPushStatus());
        vo.setPushStatusLabel(resolvePushStatusLabel(entity.getPushStatus()));
        vo.setVoucherNo(entity.getVoucherNo());
        vo.setVoucherType(entity.getVoucherType());
        vo.setVoucherNumber(entity.getVoucherNumber());
        vo.setBillDate(formatDate(entity.getBillDate()));
        vo.setPushedAt(formatDateTime(entity.getPushedAt()));
        vo.setFailureReason(entity.getErrorMessage());
        return vo;
    }

    protected ExpenseVoucherEntrySnapshotVO toEntrySnapshotVO(ExpVoucherPushEntry entity) {
        ExpenseVoucherEntrySnapshotVO vo = new ExpenseVoucherEntrySnapshotVO();
        vo.setEntryNo(entity.getEntryNo());
        vo.setDirection(entity.getDirection());
        vo.setDigest(entity.getDigest());
        vo.setAccountCode(entity.getAccountCode());
        vo.setAccountName(entity.getAccountName());
        vo.setExpenseTypeCode(entity.getExpenseTypeCode());
        vo.setExpenseTypeName(entity.getExpenseTypeName());
        vo.setAmount(zero(entity.getAmount()));
        return vo;
    }

    protected FinanceVoucherOptionVO toCompanyOption(SystemCompany company) {
        return option(company.getCompanyId(), hasText(company.getCompanyCode()) ? company.getCompanyCode() + " - " + company.getCompanyName() : company.getCompanyName());
    }

    protected FinanceVoucherOptionVO toTemplateOption(ProcessDocumentTemplate template) {
        return option(template.getTemplateCode(), defaultText(template.getTemplateName(), template.getTemplateCode()));
    }

    protected FinanceVoucherOptionVO toExpenseTypeOption(ProcessExpenseType expenseType) {
        return option(expenseType.getExpenseCode(), defaultText(expenseType.getExpenseName(), expenseType.getExpenseCode()));
    }

    protected String resolveSummary(String templateRule, ProcessDocumentInstance document, String expenseTypeName) {
        String summary = hasText(templateRule) ? trim(templateRule) : "鎶ラ攢鍗?{documentCode}-${expenseTypeName}";
        return summary
                .replace("${documentCode}", defaultText(document.getDocumentCode(), ""))
                .replace("${templateName}", defaultText(document.getTemplateName(), ""))
                .replace("${submitterName}", defaultText(document.getSubmitterName(), ""))
                .replace("${expenseTypeName}", defaultText(expenseTypeName, ""));
    }

    protected ExpenseVoucherPushResultVO buildSuccessResult(ProcessDocumentInstance document, String companyId, FinanceVoucherSaveResultVO saveResult) {
        ExpenseVoucherPushResultVO result = new ExpenseVoucherPushResultVO();
        result.setDocumentCode(document.getDocumentCode());
        result.setCompanyId(companyId);
        result.setTemplateCode(document.getTemplateCode());
        result.setTemplateName(document.getTemplateName());
        result.setPushStatus(PUSH_STATUS_SUCCESS);
        result.setVoucherNo(saveResult.getVoucherNo());
        return result;
    }

    protected ExpenseVoucherPushResultVO buildFailureResult(String documentCode, String companyId, String templateCode, String templateName, String errorMessage) {
        ExpenseVoucherPushResultVO result = new ExpenseVoucherPushResultVO();
        result.setDocumentCode(documentCode);
        result.setCompanyId(companyId);
        result.setTemplateCode(templateCode);
        result.setTemplateName(templateName);
        result.setPushStatus(PUSH_STATUS_FAILED);
        result.setErrorMessage(defaultText(errorMessage, "鎺ㄩ€佸け璐?"));
        return result;
    }

    protected <T> ExpenseVoucherPageVO<T> buildPage(List<T> rows, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int start = Math.min((safePage - 1) * safePageSize, rows.size());
        int end = Math.min(start + safePageSize, rows.size());
        ExpenseVoucherPageVO<T> result = new ExpenseVoucherPageVO<>();
        result.setTotal(rows.size());
        result.setPage(safePage);
        result.setPageSize(safePageSize);
        result.setItems(new ArrayList<>(rows.subList(start, end)));
        return result;
    }

    protected FinanceVoucherOptionVO option(String value, String label) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    protected List<FinanceVoucherOptionVO> toOptions(List<OptionSeed> seeds) {
        return seeds.stream().map(item -> option(item.value, item.label)).toList();
    }

    protected boolean matchesCompany(String actualCompanyId, String filterCompanyId) {
        return !hasText(filterCompanyId) || Objects.equals(trim(actualCompanyId), trim(filterCompanyId));
    }

    protected boolean matchesKeyword(String keyword, String... texts) {
        if (!hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = trim(keyword).toLowerCase(Locale.ROOT);
        for (String text : texts) {
            if (containsIgnoreCase(text, normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsIgnoreCase(String source, String keyword) {
        return hasText(source) && hasText(keyword) && source.toLowerCase(Locale.ROOT).contains(trim(keyword).toLowerCase(Locale.ROOT));
    }

    protected boolean matchesDateRange(LocalDate date, String dateFrom, String dateTo) {
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        if (date == null) {
            return from == null && to == null;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    protected LocalDate parseDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(trim(value), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected String formatDate(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    protected String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString().replace('T', ' ');
    }

    protected String resolveAccountName(String accountCode) {
        String code = trim(accountCode);
        if (code == null) {
            return null;
        }
        for (OptionSeed seed : ACCOUNT_SEEDS) {
            if (Objects.equals(seed.value, code)) {
                return seed.label;
            }
        }
        return code + " 浼氳绉戠洰";
    }

    protected String resolveVoucherTypeLabel(String voucherType) {
        String value = trim(voucherType);
        if (value == null) {
            return DEFAULT_VOUCHER_TYPE;
        }
        for (OptionSeed seed : VOUCHER_TYPE_SEEDS) {
            if (Objects.equals(seed.value, value)) {
                return seed.label;
            }
        }
        return value;
    }

    protected String resolvePushStatusLabel(String pushStatus) {
        return switch (defaultText(pushStatus, PUSH_STATUS_UNPUSHED)) {
            case PUSH_STATUS_SUCCESS -> "鎺ㄩ€佹垚鍔?";
            case PUSH_STATUS_FAILED -> "鎺ㄩ€佸け璐?";
            default -> "寰呮帹閫?";
        };
    }

    protected BigDecimal zero(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    protected String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    protected boolean hasText(String value) {
        return trim(value) != null;
    }

    protected String defaultText(String value, String fallback) {
        return hasText(value) ? trim(value) : fallback;
    }

    protected static final class OptionSeed {
        protected final String value;
        protected final String label;

        protected OptionSeed(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    protected static final class CompanyBatchContext {
        protected final ExpVoucherPushBatch batch;
        protected int documentCount;
        protected int successCount;
        protected int failureCount;

        protected CompanyBatchContext(ExpVoucherPushBatch batch) {
            this.batch = batch;
        }
    }
}
