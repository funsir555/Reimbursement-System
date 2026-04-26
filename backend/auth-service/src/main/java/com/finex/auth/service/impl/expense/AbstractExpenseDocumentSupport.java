// 涓氬姟鍩燂細鎶ラ攢鍗曞綍鍏ャ€佹祦杞笌鏌ヨ
// 鏂囦欢瑙掕壊锛氬崟鎹敓鍛藉懆鏈熷鐢ㄦ敮鎾戠被
// 椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷鍗曟嵁鐘舵€佹祦杞€佸鎵硅矾鐢便€侀噾棰濇眹鎬诲拰閲嶅鎻愪氦淇濇姢銆?


package com.finex.auth.service.impl.expense;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseBankCallbackDTO;
import com.finex.auth.dto.ExpenseBankLinkConfigVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseBankLinkSummaryVO;
import com.finex.auth.dto.ExpenseApprovalLogVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDetailInstanceSummaryVO;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentBankPaymentVO;
import com.finex.auth.dto.ExpenseDocumentBankReceiptVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessFlowConditionDTO;
import com.finex.auth.dto.ProcessFlowConditionGroupDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemUserRole;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
import com.finex.auth.mapper.ProcessExpenseDetailDesignMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.ProcessFormDesignMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.FinanceVendorService;
import com.finex.auth.service.NotificationService;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 鎶ラ攢鍗曠敓鍛藉懆鏈熼€氱敤鏀拺绫汇€?
 * 灏佽鎶ラ攢鍗曞綍鍏ャ€佺紪杈戙€佹彁浜ゆ祦杞€佸叧鑱旀牳閿€鍜岃鎯呮煡璇㈢瓑鍙鐢ㄨ兘鍔涖€?
 * 淇敼杩欓噷鏃讹紝瑕佺壒鍒叧娉ㄥ崟鎹姸鎬佹祦杞€佸鎵归摼璺€侀噾棰濆彛寰勫拰閲嶅鎻愪氦淇濇姢銆?
 */
@Service
@Slf4j
@RequiredArgsConstructor
class AbstractExpenseDocumentSupport {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PM_NAME_MAX_LENGTH = 64;
    private static final int PM_TITLE_MAX_LENGTH = 128;
    private static final int PM_FIELD_KEY_MAX_LENGTH = 64;

    private static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";
    private static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    private static final String PAYEE_COMPONENT_CODE = "payee";
    private static final String COUNTERPARTY_COMPONENT_CODE = "counterparty";
    private static final String PERSONAL_PAYEE_VALUE_PREFIX = "PERSONAL_PAYEE:";
    private static final String PAYEE_SOURCE_PERSONAL = "PERSONAL_PRIVATE_PAYEE";
    private static final String CONTROL_TYPE_DATE = "DATE";
    private static final Set<String> PAYMENT_DATE_LABELS = Set.of("\u4ed8\u6b3e\u65e5\u671f", "\u652f\u4ed8\u65e5\u671f");
    private static final String DETAIL_TYPE_NORMAL = "NORMAL_REIMBURSEMENT";
    private static final String DETAIL_TYPE_ENTERPRISE = "ENTERPRISE_TRANSACTION";
    private static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    private static final String ENTERPRISE_MODE_INVOICE_FULL_PAYMENT = "INVOICE_FULL_PAYMENT";
    private static final String INVOICE_FREE_MODE_REQUIRED = "NOT_FREE";
    private static final String FIELD_EXPENSE_TYPE_CODE = ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE;
    private static final String FIELD_BUSINESS_SCENARIO = ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO;
    private static final String FIELD_DETAIL_AMOUNT = ExpenseAmountResolver.FIELD_DETAIL_AMOUNT;
    private static final String FIELD_INVOICE_AMOUNT = ExpenseDetailSystemFieldSupport.FIELD_INVOICE_AMOUNT;
    private static final String FIELD_ACTUAL_PAYMENT_AMOUNT = ExpenseDetailSystemFieldSupport.FIELD_ACTUAL_PAYMENT_AMOUNT;
    private static final String FIELD_INVOICE_ATTACHMENTS = ExpenseDetailSystemFieldSupport.FIELD_INVOICE_ATTACHMENTS;
    private static final String FIELD_PENDING_WRITE_OFF_AMOUNT = ExpenseDetailSystemFieldSupport.FIELD_PENDING_WRITE_OFF_AMOUNT;
    private static final String RELATED_DOCUMENT_COMPONENT_CODE = "related-document";
    private static final String WRITEOFF_DOCUMENT_COMPONENT_CODE = "writeoff-document";
    private static final String RELATION_TYPE_RELATED = "RELATED";
    private static final String RELATION_TYPE_WRITEOFF = "WRITEOFF";
    private static final String RELATION_STATUS_ACTIVE = "ACTIVE";
    private static final String RELATION_STATUS_VOID = "VOID";
    private static final String WRITEOFF_STATUS_PENDING = "PENDING_EFFECTIVE";
    private static final String WRITEOFF_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String WRITEOFF_STATUS_VOID = "VOID";
    private static final String WRITEOFF_SOURCE_LOAN = "LOAN";
    private static final String WRITEOFF_SOURCE_PREPAY_REPORT = "PREPAY_REPORT";
    private static final String DASHBOARD_WRITEOFF_SOURCE_FIELD_KEY = "dashboard-writeoff";
    private static final String MESSAGE_RELATED_DOCUMENT_SCOPE_RESTRICTED = "\u4ec5\u53ef\u5173\u8054\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e";
    private static final String MESSAGE_WRITEOFF_DOCUMENT_SCOPE_RESTRICTED = "\u4ec5\u53ef\u9009\u62e9\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e\u8fdb\u884c\u6838\u9500";

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String APPROVER_TYPE_MANAGER = "MANAGER";
    private static final String APPROVER_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";
    private static final String PAYMENT_EXECUTOR_TYPE_DESIGNATED_MEMBER = "DESIGNATED_MEMBER";
    private static final String PAYMENT_EXECUTOR_TYPE_FINANCE_ROLE = "FINANCE_ROLE";

    private static final String DEPT_SOURCE_UNDERTAKE = "UNDERTAKE_DEPT";
    private static final String DEPT_SOURCE_SUBMITTER = "SUBMITTER_DEPT";
    private static final String MISSING_HANDLER_AUTO_SKIP = "AUTO_SKIP";
    private static final String APPROVAL_MODE_OR_SIGN = "OR_SIGN";
    private static final String APPROVAL_MODE_AND_SIGN = "AND_SIGN";
    private static final String PAYMENT_SPECIAL_ALLOW_RETRY = "ALLOW_RETRY";
    private static final String PAYMENT_EXECUTE_PERMISSION = "expense:payment:payment_order:execute";

    private static final String DOCUMENT_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_DRAFT = "DRAFT";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String MESSAGE_DOCUMENT_VIEW_FORBIDDEN = "\u4f60\u65e0\u6743\u67e5\u770b\u8be5\u5355\u636e";

    private static final String BANK_PROVIDER_CMB = "CMB";
    private static final String BANK_CHANNEL_CMB_CLOUD = "CMB_CLOUD";
    private static final String RECEIPT_STATUS_PENDING = "PENDING";
    private static final String RECEIPT_STATUS_RECEIVED = "RECEIVED";
    private static final String RECEIPT_STATUS_FAILED = "FAILED";
    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PAUSED = "PAUSED";
    private static final String TASK_STATUS_APPROVED = "APPROVED";
    private static final String TASK_STATUS_REJECTED = "REJECTED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String TASK_KIND_NORMAL = "NORMAL";
    private static final String TASK_KIND_ADD_SIGN = "ADD_SIGN";

    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_RECALL = "RECALL";
    private static final String LOG_RESUBMIT = "RESUBMIT";
    private static final String LOG_ROUTE_HIT = "ROUTE_HIT";
    private static final String LOG_APPROVAL_PENDING = "APPROVAL_PENDING";
    private static final String LOG_APPROVE = "APPROVE";
    private static final String LOG_REJECT = "REJECT";
    private static final String LOG_MODIFY = "MODIFY";
    private static final String LOG_COMMENT = "COMMENT";
    private static final String LOG_REMIND = "REMIND";
    private static final String LOG_TRANSFER = "TRANSFER";
    private static final String LOG_ADD_SIGN = "ADD_SIGN";
    private static final String LOG_AUTO_SKIP = "AUTO_SKIP";
    private static final String LOG_CC_REACHED = "CC_REACHED";
    private static final String LOG_PAYMENT_REACHED = "PAYMENT_REACHED";
    private static final String LOG_PAYMENT_PENDING = "PAYMENT_PENDING";
    private static final String LOG_PAYMENT_START = "PAYMENT_START";
    private static final String LOG_PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
    private static final String LOG_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String LOG_FINISH = "FINISH";
    private static final String LOG_EXCEPTION = "EXCEPTION";
    private static final String FLOW_FINISH_COMMENT = "瀹℃壒娴佺▼缁撴潫";
    private static final String ROOT_CONTAINER_KEY = "__ROOT__";
    private static final int NAVIGATION_HISTORY_LIMIT = 200;

    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessFormDesignMapper processFormDesignMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    private final ProcessFlowMapper processFlowMapper;
    private final ProcessFlowVersionMapper processFlowVersionMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final SystemPermissionMapper systemPermissionMapper;
    private final SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final UserMapper userMapper;
    private final UserBankAccountMapper userBankAccountMapper;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentTaskMapper processDocumentTaskMapper;
    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final ProcessDocumentRelationMapper processDocumentRelationMapper;
    private final ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    private final PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    private final ExpenseAttachmentService expenseAttachmentService;
    private final FinanceVendorService financeVendorService;
    private final ProcessExpenseDetailDesignMapper processExpenseDetailDesignMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ExpenseReadonlyPayeeAccountSnapshotEnhancer readonlyPayeeAccountSnapshotEnhancer;
    private final ExpenseDocumentMetadataSupport expenseDocumentMetadataSupport;
    private final ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    private final ExpenseDocumentTaskRuntimeSupport expenseDocumentTaskRuntimeSupport;


    /**
     * 鏌ヨ姹囨€汇€?
     */
    ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        ProcessDocumentInstance instance = requireDocument(documentCode);
        assertCanViewDocument(instance, userId, allowCrossView);
        ProcessDocumentExpenseDetail detail = requireExpenseDetail(documentCode, detailNo);
        Map<String, Object> parentSchema = readMap(instance.getFormSchemaSnapshotJson());
        Map<String, Object> parentFormData = readFormData(instance.getFormDataJson());
        String paymentCompanyId = extractFirstBusinessComponentValue(parentSchema, parentFormData, PAYMENT_COMPANY_COMPONENT_CODE);
        return toExpenseDetailDetailVO(detail, paymentCompanyId);
    }

    /**
     * 鑾峰彇閫夋嫨鍣ㄣ€?
     */
    private Map<String, BigDecimal> loadPrepayReportAmountMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .eq(ProcessDocumentExpenseDetail::getBusinessSceneMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        detail -> defaultDecimal(ExpenseAmountResolver.resolvePrepayWriteOffAmount(
                           readMap(detail.getFormDataJson()),
                           detail.getActualPaymentAmount()
                   )),
                        BigDecimal::add
                )
        ));
    }

    /**
     * 鍔犺浇鏄犲皠銆?
     */
    private Map<String, BigDecimal> loadEffectiveWriteOffAmountMap(List<String> targetDocumentCodes) {
        if (targetDocumentCodes == null || targetDocumentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .in(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCodes)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentWriteOff::getTargetDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        item -> defaultDecimal(item.getEffectiveAmount()),
                        BigDecimal::add
                )
        ));
    }

    /**
     * 鍔犺浇鏄犲皠銆?
     */
    private BigDecimal resolveOutstandingAmount(
            ProcessDocumentInstance instance,
            String kind,
            Map<String, BigDecimal> prepayAmountMap,
            Map<String, BigDecimal> effectiveAmountMap
    ) {
        BigDecimal baseAmount = Objects.equals(kind, WRITEOFF_SOURCE_LOAN)
                ? defaultDecimal(instance.getTotalAmount())
                : defaultDecimal(prepayAmountMap.get(instance.getDocumentCode()));
        BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
        BigDecimal outstandingAmount = baseAmount.subtract(effectiveAmount);
        return outstandingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstandingAmount;
    }

    /**
     * 瑙ｆ瀽閲戦銆?
     */
    private void ensureDashboardWriteOffTargetSupported(ProcessDocumentInstance target) {
        if (!isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u672a\u5ba1\u6279\u901a\u8fc7\uff0c\u4e0d\u80fd\u53d1\u8d77\u6838\u9500");
        }
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(target.getDocumentCode()));
        resolveWriteOffSourceKind(target, prepayAmountMap);
    }

    private void ensureApprovedReportSource(ProcessDocumentInstance sourceReport) {
        if (!isEffectiveApprovedStatus(sourceReport.getStatus())) {
            throw new IllegalStateException("\u6e90\u62a5\u9500\u5355\u672a\u5ba1\u6279\u901a\u8fc7\uff0c\u4e0d\u80fd\u4f5c\u4e3a\u6838\u9500\u6765\u6e90");
        }
        if (!Objects.equals(normalizeTemplateType(sourceReport.getTemplateType()), "report")) {
            throw new IllegalStateException("\u4ec5\u652f\u6301\u5df2\u5ba1\u6279\u901a\u8fc7\u7684\u62a5\u9500\u5355\u4f5c\u4e3a\u6838\u9500\u6765\u6e90");
        }
    }

    private String normalizeRelationType(String relationType) {
        return Objects.equals(trimToNull(relationType), RELATION_TYPE_WRITEOFF) ? RELATION_TYPE_WRITEOFF : RELATION_TYPE_RELATED;
    }

    private List<String> normalizePickerTemplateTypes(String relationType, List<String> templateTypes) {
        if (Objects.equals(relationType, RELATION_TYPE_WRITEOFF)) {
            if (templateTypes == null || templateTypes.isEmpty()) {
                return List.of("report", "loan");
            }
            return templateTypes.stream()
                    .map(this::normalizeTemplateType)
                    .filter(item -> Objects.equals(item, "report") || Objects.equals(item, "loan"))
                    .distinct()
                    .toList();
        }
        if (templateTypes == null || templateTypes.isEmpty()) {
            return List.of("report", "application", "contract", "loan");
        }
        return templateTypes.stream()
                .map(this::normalizeTemplateType)
                .distinct()
                .toList();
    }

    private String normalizeTemplateType(String templateType) {
        String value = trimToNull(templateType);
        if (Objects.equals(value, "application") || Objects.equals(value, "loan") || Objects.equals(value, "contract")) {
            return value;
        }
        return "report";
    }

    /**
     * 鍒ゆ柇鐩稿叧淇℃伅銆?
     */
    boolean isTemplateAvailableForCreate(ProcessDocumentTemplate template) {
        if (!Objects.equals(trimToNull(template.getTemplateType()), "report")) {
            return true;
        }
        return trimToNull(template.getExpenseDetailDesignCode()) != null;
    }

    List<ExpenseDetailInstanceDTO> normalizeExpenseDetails(List<ExpenseDetailInstanceDTO> expenseDetails) {
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExpenseDetailInstanceDTO> normalized = new ArrayList<>();
        for (ExpenseDetailInstanceDTO item : expenseDetails) {
            if (item == null) {
                continue;
            }
            ExpenseDetailInstanceDTO next = new ExpenseDetailInstanceDTO();
            next.setDetailNo(trimToNull(item.getDetailNo()));
            next.setDetailDesignCode(trimToNull(item.getDetailDesignCode()));
            next.setDetailType(trimToNull(item.getDetailType()));
            next.setEnterpriseMode(trimToNull(item.getEnterpriseMode()));
            next.setExpenseTypeCode(trimToNull(item.getExpenseTypeCode()));
            next.setBusinessSceneMode(trimToNull(item.getBusinessSceneMode()));
            next.setDetailTitle(trimToNull(item.getDetailTitle()));
            next.setSortOrder(item.getSortOrder());
            next.setFormData(item.getFormData() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getFormData()));
            normalized.add(next);
        }
        return normalized;
    }

    /**
     * 鏍￠獙鏄庣粏銆?
     */
    void validateExpenseDetailSubmission(
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        String templateType = trimToNull(template.getTemplateType());
        if (!Objects.equals(templateType, "report")) {
            if (!expenseDetails.isEmpty()) {
                throw new IllegalArgumentException("\u5f53\u524d\u6a21\u677f\u4e0d\u652f\u6301\u8d39\u7528\u660e\u7ec6\uff0c\u4e0d\u80fd\u63d0\u4ea4\u8d39\u7528\u660e\u7ec6\u6570\u636e");
            }
            return;
        }
        if (expenseDetailDesign == null) {
            throw new IllegalStateException("\u5f53\u524d\u62a5\u9500\u6a21\u677f\u672a\u7ed1\u5b9a\u8d39\u7528\u660e\u7ec6\u8868\u5355");
        }
        if (expenseDetails.isEmpty()) {
            throw new IllegalArgumentException("\u62a5\u9500\u5355\u63d0\u4ea4\u524d\u81f3\u5c11\u9700\u8981 1 \u4efd\u8d39\u7528\u660e\u7ec6");
        }
        if (expenseDetails.size() > 10) {
            throw new IllegalArgumentException("\u8d39\u7528\u660e\u7ec6\u6700\u591a\u53ea\u80fd\u6dfb\u52a0 10 \u4efd");
        }
    }

    /**
     * 鏍￠獙涓婁笅鏂囥€?
     */
    String validateSubmitContext(
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            ProcessExpenseDetailDesign expenseDetailDesign,
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        if (trimToNull(template.getFormDesignCode()) == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u672a\u7ed1\u5b9a\u4e3b\u8868\u5355\uff0c\u65e0\u6cd5\u63d0\u4ea4\u5ba1\u6279\u5355");
        }
        if (formDesign == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u4e3b\u8868\u5355\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u8865\u9f50\u914d\u7f6e\u540e\u518d\u63d0\u4ea4");
        }
        validateExpenseDetailSubmission(template, expenseDetailDesign, expenseDetails);
        validateRuntimeRequiredFields(
                readSchema(formDesign.getSchemaJson()),
                formData == null ? Collections.emptyMap() : formData,
                null,
                null
        );
        if (Objects.equals(trimToNull(template.getTemplateType()), "report")
                && trimToNull(template.getExpenseDetailDesignCode()) != null
                && expenseDetailDesign == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u8d39\u7528\u660e\u7ec6\u8868\u5355\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u8865\u9f50\u914d\u7f6e\u540e\u518d\u63d0\u4ea4");
        }
        validateExpenseDetailRequiredFields(template, expenseDetailDesign, expenseDetails);
        return validateFlowSnapshotForSubmit(template);
    }

    private void validateExpenseDetailRequiredFields(
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        if (!Objects.equals(trimToNull(template.getTemplateType()), "report") || expenseDetailDesign == null) {
            return;
        }
        Map<String, Object> detailSchema = expenseDetailSystemFieldSupport.readSchema(
                expenseDetailDesign.getSchemaJson(),
                expenseDetailDesign.getDetailType()
        );
        String detailType = resolveExpenseDetailType(template, expenseDetailDesign);
        String defaultBusinessSceneMode = trimToNull(template.getExpenseDetailModeDefault());
        for (int index = 0; index < expenseDetails.size(); index++) {
            ExpenseDetailInstanceDTO detail = expenseDetails.get(index);
            Map<String, Object> detailFormData = normalizeExpenseDetailFormData(
                    detail == null ? null : detail.getFormData(),
                    detailType,
                    defaultBusinessSceneMode
            );
            String businessSceneMode = resolveBusinessSceneMode(
                    detailType,
                    firstNonBlank(
                            detail == null ? null : trimToNull(detail.getBusinessSceneMode()),
                            asText(detailFormData.get(FIELD_BUSINESS_SCENARIO))
                    ),
                    defaultBusinessSceneMode
            );
            String detailName = firstNonBlank(
                    detail == null ? null : trimToNull(detail.getDetailTitle()),
                    detail == null ? null : trimToNull(detail.getDetailNo()),
                    "\u8d39\u7528\u660e\u7ec6 " + (index + 1)
            );
            validateRuntimeRequiredFields(
                    detailSchema,
                    detailFormData,
                    detailType,
                    businessSceneMode,
                    "\u8bf7\u5148\u5b8c\u5584\u8d39\u7528\u660e\u7ec6\u201c" + detailName + "\u201d"
            );
        }
    }

    private void validateRuntimeRequiredFields(
            Map<String, Object> schema,
            Map<String, Object> formData,
            String detailType,
            String businessSceneMode
    ) {
        validateRuntimeRequiredFields(schema, formData, detailType, businessSceneMode, null);
    }

    private void validateRuntimeRequiredFields(
            Map<String, Object> schema,
            Map<String, Object> formData,
            String detailType,
            String businessSceneMode,
            String issuePrefix
    ) {
        Map<String, Object> safeSchema = schema == null ? defaultSchema() : schema;
        Map<String, Object> safeFormData = formData == null ? Collections.emptyMap() : formData;
        for (Object rawBlock : toObjectList(safeSchema.get("blocks"))) {
            Map<String, Object> block = toObjectMap(rawBlock);
            if (!isRuntimeRequiredBlock(block) || !isRuntimeBlockVisible(block, businessSceneMode)) {
                continue;
            }
            String fieldKey = asText(block.get("fieldKey"));
            if (fieldKey == null) {
                continue;
            }
            if (isRuntimeBlockFilled(block, safeFormData.get(fieldKey), detailType, businessSceneMode)) {
                continue;
            }
            String label = firstNonBlank(
                    asText(block.get("label")),
                    fieldKey,
                    "\u672a\u547d\u540d\u5b57\u6bb5"
            );
            String requiredMessage = "\u8bf7\u5148\u586b\u5199\u3010" + label + "\u3011";
            if (trimToNull(issuePrefix) != null) {
                throw new IllegalArgumentException(issuePrefix + "\uff1a" + requiredMessage);
            }
            throw new IllegalArgumentException(requiredMessage);
        }
    }

    private boolean isRuntimeRequiredBlock(Map<String, Object> block) {
        if (!asBoolean(block.get("required"), false)) {
            return false;
        }
        Map<String, Object> props = toObjectMap(block.get("props"));
        if (asBoolean(props.get("readOnly"), false)) {
            return false;
        }
        String kind = asText(block.get("kind"));
        if (Objects.equals(kind, "CONTROL")) {
            return !Objects.equals(asText(props.get("controlType")), "SECTION");
        }
        return true;
    }

    private boolean isRuntimeBlockVisible(Map<String, Object> block, String businessSceneMode) {
        Set<String> visibleSceneModes = toStringSet(toObjectMap(block.get("props")).get("visibleSceneModes"));
        if (visibleSceneModes.isEmpty()) {
            return true;
        }
        String normalizedSceneMode = trimToNull(businessSceneMode);
        return normalizedSceneMode != null && visibleSceneModes.contains(normalizedSceneMode);
    }

    private boolean isRuntimeBlockFilled(
            Map<String, Object> block,
            Object value,
            String detailType,
            String businessSceneMode
    ) {
        String kind = asText(block.get("kind"));
        Map<String, Object> props = toObjectMap(block.get("props"));
        if (Objects.equals(kind, "CONTROL")) {
            String controlType = asText(props.get("controlType"));
            if (Objects.equals(controlType, "SWITCH")) {
                String normalizedValue = asText(value);
                return value instanceof Boolean
                        || Objects.equals(normalizedValue, "true")
                        || Objects.equals(normalizedValue, "false");
            }
            if (Set.of("MULTI_SELECT", "CHECKBOX", "DATE_RANGE", "ATTACHMENT", "IMAGE").contains(controlType)) {
                return hasCollectionValue(value);
            }
            if (Set.of("NUMBER", "AMOUNT").contains(controlType)) {
                return hasNumericValue(value);
            }
            if (Set.of("SELECT", "RADIO").contains(controlType)) {
                return hasLookupValue(value);
            }
            return hasAnyRuntimeValue(value);
        }
        if (Objects.equals(kind, "BUSINESS_COMPONENT")) {
            String componentCode = asText(props.get("componentCode"));
            if (Objects.equals(componentCode, RELATED_DOCUMENT_COMPONENT_CODE)
                    || Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                return hasCollectionValue(value);
            }
            if (Set.of(
                    PAYMENT_COMPANY_COMPONENT_CODE,
                    COUNTERPARTY_COMPONENT_CODE,
                    PAYEE_COMPONENT_CODE,
                    "payee-account",
                    UNDERTAKE_DEPARTMENT_COMPONENT_CODE
            ).contains(componentCode)) {
                return hasLookupValue(value);
            }
            if (Objects.equals(componentCode, "bank-push-summary")) {
                return hasAnyRuntimeValue(value);
            }
        }
        return hasAnyRuntimeValue(value);
    }

    private boolean hasCollectionValue(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().anyMatch(this::hasAnyRuntimeValue);
        }
        return false;
    }

    private boolean hasNumericValue(Object value) {
        if (value instanceof Number number) {
            if (number instanceof Double doubleValue) {
                return Double.isFinite(doubleValue);
            }
            if (number instanceof Float floatValue) {
                return Float.isFinite(floatValue);
            }
            return true;
        }
        return trimToNull(asText(value)) != null;
    }

    private boolean hasLookupValue(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().anyMatch(this::hasLookupValue);
        }
        if (value instanceof Boolean || value instanceof Number) {
            return true;
        }
        if (value instanceof Map<?, ?> map) {
            return trimToNull(resolveLookupIdentifier(toObjectMap(map))) != null;
        }
        return trimToNull(asText(value)) != null;
    }

    private boolean hasAnyRuntimeValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return true;
        }
        if (value instanceof Number) {
            return hasNumericValue(value);
        }
        if (value instanceof CharSequence) {
            return trimToNull(String.valueOf(value)) != null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().anyMatch(this::hasAnyRuntimeValue);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = toObjectMap(map);
            if (trimToNull(resolveLookupIdentifier(normalized)) != null) {
                return true;
            }
            return normalized.values().stream().anyMatch(this::hasAnyRuntimeValue);
        }
        return trimToNull(String.valueOf(value)) != null;
    }

    private String resolveLookupIdentifier(Map<String, Object> value) {
        return firstNonBlank(
                asText(value.get("value")),
                asText(value.get("code")),
                asText(value.get("id")),
                asText(value.get("documentCode")),
                asText(value.get("attachmentId"))
        );
    }

    void syncDocumentBusinessRelations(
            String documentCode,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        if (trimToNull(documentCode) == null || formDesign == null) {
            return;
        }
        ProcessDocumentInstance source = requireDocument(documentCode);
        Long sourceSubmitterUserId = source.getSubmitterUserId();

        List<DocumentBusinessBinding> bindings = collectDocumentBusinessBindings(formDesign);
        List<RelatedDocumentSelection> relatedSelections = new ArrayList<>();
        List<WriteOffSelection> writeOffSelections = new ArrayList<>();
        for (DocumentBusinessBinding binding : bindings) {
            if (Objects.equals(binding.componentCode(), RELATED_DOCUMENT_COMPONENT_CODE)) {
                relatedSelections.addAll(normalizeRelatedDocumentSelections(documentCode, binding, formData));
            } else if (Objects.equals(binding.componentCode(), WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                writeOffSelections.addAll(normalizeWriteOffSelections(documentCode, binding, formData));
            }
        }
        Map<DocumentBusinessTargetKey, ProcessDocumentRelation> existingRelationMap = loadExistingRelationMap(documentCode);
        Map<DocumentBusinessTargetKey, ProcessDocumentWriteOff> existingWriteOffMap = loadExistingWriteOffMap(documentCode);
        if (bindings.isEmpty()) {
            voidUnselectedRelations(existingRelationMap.values(), Collections.emptySet());
            voidUnselectedWriteOffs(existingWriteOffMap.values(), Collections.emptySet());
            return;
        }

        Set<String> targetDocumentCodes = new LinkedHashSet<>();
        relatedSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        writeOffSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        if (targetDocumentCodes.isEmpty()) {
            voidUnselectedRelations(existingRelationMap.values(), Collections.emptySet());
            voidUnselectedWriteOffs(existingWriteOffMap.values(), Collections.emptySet());
            return;
        }

        Map<String, ProcessDocumentInstance> targetDocumentMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, targetDocumentCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();
        Set<DocumentBusinessTargetKey> selectedRelationKeys = new LinkedHashSet<>();
        Set<DocumentBusinessTargetKey> selectedWriteOffKeys = new LinkedHashSet<>();

        for (RelatedDocumentSelection selection : relatedSelections) {
            ProcessDocumentInstance target = requireRelationSelectableTargetDocument(
                    targetDocumentMap,
                    selection.documentCode(),
                    sourceSubmitterUserId,
                    MESSAGE_RELATED_DOCUMENT_SCOPE_RESTRICTED
            );
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException("\u5173\u8054\u5355\u636e\u7c7b\u578b\u4e0e\u5f53\u524d\u7ec4\u4ef6\u914d\u7f6e\u4e0d\u5339\u914d\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9");
            }
            DocumentBusinessTargetKey key = new DocumentBusinessTargetKey(selection.fieldKey(), selection.documentCode());
            selectedRelationKeys.add(key);
            ProcessDocumentRelation relation = existingRelationMap.get(key);
            if (relation == null) {
                relation = new ProcessDocumentRelation();
                relation.setSourceDocumentCode(documentCode);
                validatePmFieldKeyLength(selection.fieldKey(), "\u5173\u8054\u5b57\u6bb5\u6807\u8bc6");
                relation.setSourceFieldKey(selection.fieldKey());
                relation.setTargetDocumentCode(selection.documentCode());
                relation.setTargetTemplateType(normalizedTemplateType);
                relation.setSortOrder(selection.sortOrder());
                relation.setStatus(RELATION_STATUS_ACTIVE);
                relation.setCreatedAt(now);
                relation.setUpdatedAt(now);
                processDocumentRelationMapper.insert(relation);
                continue;
            }
            updateExistingRelation(relation, normalizedTemplateType, selection.sortOrder(), now);
        }

        for (WriteOffSelection selection : writeOffSelections) {
            ProcessDocumentInstance target = requireRelationSelectableTargetDocument(
                    targetDocumentMap,
                    selection.documentCode(),
                    sourceSubmitterUserId,
                    MESSAGE_WRITEOFF_DOCUMENT_SCOPE_RESTRICTED
            );
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException("\u6838\u9500\u5355\u636e\u7c7b\u578b\u4e0e\u5f53\u524d\u7ec4\u4ef6\u914d\u7f6e\u4e0d\u5339\u914d\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9");
            }
            String writeOffSourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, writeOffSourceKind, prepayAmountMap, effectiveAmountMap);
            if (selection.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("\u6838\u9500\u91d1\u989d\u5fc5\u987b\u5927\u4e8e 0");
            }
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("\u5f53\u524d\u6838\u9500\u5355\u636e\u5df2\u65e0\u53ef\u7528\u6838\u9500\u4f59\u989d");
            }
            if (selection.requestedAmount().compareTo(availableAmount) > 0) {
                throw new IllegalStateException("\u6838\u9500\u91d1\u989d\u4e0d\u80fd\u8d85\u8fc7\u5f53\u524d\u53ef\u7528\u6838\u9500\u4f59\u989d");
            }
            DocumentBusinessTargetKey key = new DocumentBusinessTargetKey(selection.fieldKey(), selection.documentCode());
            selectedWriteOffKeys.add(key);
            ProcessDocumentWriteOff writeOff = existingWriteOffMap.get(key);
            if (writeOff == null) {
                writeOff = new ProcessDocumentWriteOff();
                writeOff.setSourceDocumentCode(documentCode);
                validatePmFieldKeyLength(selection.fieldKey(), "\u6838\u9500\u5b57\u6bb5\u6807\u8bc6");
                writeOff.setSourceFieldKey(selection.fieldKey());
                writeOff.setTargetDocumentCode(selection.documentCode());
                writeOff.setTargetTemplateType(normalizedTemplateType);
                writeOff.setWriteoffSourceKind(writeOffSourceKind);
                writeOff.setRequestedAmount(selection.requestedAmount());
                writeOff.setEffectiveAmount(null);
                writeOff.setAvailableSnapshotAmount(availableAmount);
                writeOff.setRemainingSnapshotAmount(availableAmount.subtract(selection.requestedAmount()));
                writeOff.setSortOrder(selection.sortOrder());
                writeOff.setStatus(WRITEOFF_STATUS_PENDING);
                writeOff.setEffectiveAt(null);
                writeOff.setCreatedAt(now);
                writeOff.setUpdatedAt(now);
                processDocumentWriteOffMapper.insert(writeOff);
                continue;
            }
            updateExistingWriteOff(
                    writeOff,
                    normalizedTemplateType,
                    writeOffSourceKind,
                    selection.requestedAmount(),
                    availableAmount,
                    selection.sortOrder(),
                    now
            );
        }
        voidUnselectedRelations(existingRelationMap.values(), selectedRelationKeys);
        voidUnselectedWriteOffs(existingWriteOffMap.values(), selectedWriteOffKeys);
    }

    private Map<DocumentBusinessTargetKey, ProcessDocumentRelation> loadExistingRelationMap(String documentCode) {
        return processDocumentRelationMapper.selectList(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .eq(ProcessDocumentRelation::getSourceDocumentCode, documentCode)
        ).stream().collect(Collectors.toMap(
                item -> new DocumentBusinessTargetKey(item.getSourceFieldKey(), item.getTargetDocumentCode()),
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<DocumentBusinessTargetKey, ProcessDocumentWriteOff> loadExistingWriteOffMap(String documentCode) {
        return processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, documentCode)
        ).stream().collect(Collectors.toMap(
                item -> new DocumentBusinessTargetKey(item.getSourceFieldKey(), item.getTargetDocumentCode()),
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private void updateExistingRelation(
            ProcessDocumentRelation relation,
            String targetTemplateType,
            int sortOrder,
            LocalDateTime now
    ) {
        relation.setTargetTemplateType(targetTemplateType);
        relation.setSortOrder(sortOrder);
        relation.setStatus(RELATION_STATUS_ACTIVE);
        relation.setUpdatedAt(now);
        processDocumentRelationMapper.updateById(relation);
    }

    private void updateExistingWriteOff(
            ProcessDocumentWriteOff writeOff,
            String targetTemplateType,
            String writeOffSourceKind,
            BigDecimal requestedAmount,
            BigDecimal availableAmount,
            int sortOrder,
            LocalDateTime now
    ) {
        writeOff.setTargetTemplateType(targetTemplateType);
        writeOff.setWriteoffSourceKind(writeOffSourceKind);
        writeOff.setRequestedAmount(requestedAmount);
        writeOff.setEffectiveAmount(null);
        writeOff.setAvailableSnapshotAmount(availableAmount);
        writeOff.setRemainingSnapshotAmount(availableAmount.subtract(requestedAmount));
        writeOff.setSortOrder(sortOrder);
        writeOff.setStatus(WRITEOFF_STATUS_PENDING);
        writeOff.setEffectiveAt(null);
        writeOff.setUpdatedAt(now);
        processDocumentWriteOffMapper.updateById(writeOff);
    }

    private void voidUnselectedRelations(
            Iterable<ProcessDocumentRelation> relations,
            Set<DocumentBusinessTargetKey> selectedKeys
    ) {
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentRelation relation : relations) {
            DocumentBusinessTargetKey key = new DocumentBusinessTargetKey(
                    relation.getSourceFieldKey(),
                    relation.getTargetDocumentCode()
            );
            if (selectedKeys.contains(key) || Objects.equals(relation.getStatus(), RELATION_STATUS_VOID)) {
                continue;
            }
            relation.setStatus(RELATION_STATUS_VOID);
            relation.setUpdatedAt(now);
            processDocumentRelationMapper.updateById(relation);
        }
    }

    private void voidUnselectedWriteOffs(
            Iterable<ProcessDocumentWriteOff> writeOffs,
            Set<DocumentBusinessTargetKey> selectedKeys
    ) {
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentWriteOff writeOff : writeOffs) {
            DocumentBusinessTargetKey key = new DocumentBusinessTargetKey(
                    writeOff.getSourceFieldKey(),
                    writeOff.getTargetDocumentCode()
            );
            if (selectedKeys.contains(key) || Objects.equals(writeOff.getStatus(), WRITEOFF_STATUS_VOID)) {
                continue;
            }
            writeOff.setStatus(WRITEOFF_STATUS_VOID);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.updateById(writeOff);
        }
    }

    void finalizeEffectiveWriteOffs(String documentCode) {
        List<ProcessDocumentWriteOff> pendingWriteOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentWriteOff::getSortOrder, ProcessDocumentWriteOff::getId)
        );
        if (pendingWriteOffs.isEmpty()) {
            return;
        }

        Map<String, ProcessDocumentInstance> targetDocumentMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).toList())
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();

        for (ProcessDocumentWriteOff writeOff : pendingWriteOffs) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, writeOff.getTargetDocumentCode(), "\u6838\u9500\u76ee\u6807\u5355\u636e\u4e0d\u5b58\u5728\u6216\u672a\u5ba1\u6279\u901a\u8fc7");
            String sourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, sourceKind, prepayAmountMap, effectiveAmountMap);
            BigDecimal requestedAmount = defaultDecimal(writeOff.getRequestedAmount());
            if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("\u672c\u6b21\u6838\u9500\u91d1\u989d\u5fc5\u987b\u5927\u4e8e 0");
            }
            if (requestedAmount.compareTo(availableAmount) > 0) {
                throw new IllegalStateException("\u6838\u9500\u5355\u636e " + writeOff.getTargetDocumentCode() + " \u7684\u53ef\u6838\u9500\u4f59\u989d\u4e0d\u8db3\uff0c\u8bf7\u5237\u65b0\u540e\u91cd\u8bd5");
            }
            writeOff.setWriteoffSourceKind(sourceKind);
            writeOff.setEffectiveAmount(requestedAmount);
            writeOff.setAvailableSnapshotAmount(availableAmount);
            writeOff.setRemainingSnapshotAmount(availableAmount.subtract(requestedAmount));
            writeOff.setStatus(WRITEOFF_STATUS_EFFECTIVE);
            writeOff.setEffectiveAt(now);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.updateById(writeOff);
            effectiveAmountMap.put(
                    writeOff.getTargetDocumentCode(),
                    defaultDecimal(effectiveAmountMap.get(writeOff.getTargetDocumentCode())).add(requestedAmount)
            );
        }
    }

    private void voidActiveRelations(String documentCode) {
        List<ProcessDocumentRelation> relations = processDocumentRelationMapper.selectList(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .eq(ProcessDocumentRelation::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentRelation::getStatus, RELATION_STATUS_ACTIVE)
        );
        if (relations.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentRelation relation : relations) {
            relation.setStatus(RELATION_STATUS_VOID);
            relation.setUpdatedAt(now);
            processDocumentRelationMapper.updateById(relation);
        }
    }

    private void voidPendingWriteOffs(String documentCode) {
        List<ProcessDocumentWriteOff> writeOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_PENDING)
        );
        if (writeOffs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentWriteOff writeOff : writeOffs) {
            writeOff.setStatus(WRITEOFF_STATUS_VOID);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.updateById(writeOff);
        }
    }

    private List<DocumentBusinessBinding> collectDocumentBusinessBindings(ProcessFormDesign formDesign) {
        Map<String, Object> schema = readSchema(formDesign.getSchemaJson());
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks) || blocks.isEmpty()) {
            return Collections.emptyList();
        }
        List<DocumentBusinessBinding> bindings = new ArrayList<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawFieldKey = blockMap.get("fieldKey");
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> propsMap) || rawFieldKey == null) {
                continue;
            }
            String componentCode = asText(propsMap.get("componentCode"));
            String fieldKey = asText(rawFieldKey);
            if (fieldKey == null || componentCode == null) {
                continue;
            }
            if (!Objects.equals(componentCode, RELATED_DOCUMENT_COMPONENT_CODE)
                    && !Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                continue;
            }
            bindings.add(new DocumentBusinessBinding(fieldKey, componentCode, normalizeAllowedTemplateTypes(componentCode, propsMap.get("allowedTemplateTypes"))));
        }
        return bindings;
    }

    private List<String> normalizeAllowedTemplateTypes(String componentCode, Object rawValue) {
        boolean writeOffComponent = Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE);
        if (!(rawValue instanceof List<?> values) || values.isEmpty()) {
            return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
        }
        List<String> normalized = values.stream()
                .map(item -> normalizeTemplateType(item == null ? null : String.valueOf(item)))
                .filter(item -> !writeOffComponent || Objects.equals(item, "report") || Objects.equals(item, "loan"))
                .distinct()
                .toList();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
    }

    private List<RelatedDocumentSelection> normalizeRelatedDocumentSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<RelatedDocumentSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u80fd\u5173\u8054\u81ea\u5df1");
            }
            validatePmFieldKeyLength(binding.fieldKey(), "\u5173\u8054\u5b57\u6bb5\u6807\u8bc6");
            selections.add(new RelatedDocumentSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), sortOrder++));
        }
        return selections;
    }

    private List<WriteOffSelection> normalizeWriteOffSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<WriteOffSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u80fd\u6838\u9500\u81ea\u5df1");
            }
            BigDecimal requestedAmount = toBigDecimal(record.get("writeOffAmount"));
            if (requestedAmount == null) {
                throw new IllegalStateException("\u6838\u9500\u5355\u636e\u7f3a\u5c11\u6838\u9500\u91d1\u989d");
            }
            validatePmFieldKeyLength(binding.fieldKey(), "\u6838\u9500\u5b57\u6bb5\u6807\u8bc6");
            selections.add(new WriteOffSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), requestedAmount, sortOrder++));
        }
        return selections;
    }

    private List<Map<String, Object>> normalizeDocumentRecords(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof List<?> values) {
            List<Map<String, Object>> records = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    records.add(toObjectMap(map));
                }
            }
            return records;
        }
        if (rawValue instanceof Map<?, ?> map) {
            return List.of(toObjectMap(map));
        }
        return Collections.emptyList();
    }

    private Map<String, Object> toObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private ProcessDocumentInstance requireRelationSelectableTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            Long submitterUserId,
            String invalidMessage
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null
                || !Objects.equals(target.getSubmitterUserId(), submitterUserId)
                || !isRelationSelectableStatus(target.getStatus())) {
            throw new IllegalStateException(invalidMessage);
        }
        return target;
    }

    private ProcessDocumentInstance requireApprovedTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            String actionName
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null || !isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException(actionName + "\u76ee\u6807\u4e0d\u5b58\u5728\u6216\u672a\u901a\u8fc7\u5ba1\u6279");
        }
        return target;
    }

    /**
     * 瑙ｆ瀽鐩稿叧淇℃伅銆?
     */
    private String resolveWriteOffSourceKind(
            ProcessDocumentInstance target,
            Map<String, BigDecimal> prepayAmountMap
    ) {
        String templateType = normalizeTemplateType(target.getTemplateType());
        if (Objects.equals(templateType, "loan")) {
            return WRITEOFF_SOURCE_LOAN;
        }
        if (Objects.equals(templateType, "report")
                && defaultDecimal(prepayAmountMap.get(target.getDocumentCode())).compareTo(BigDecimal.ZERO) > 0) {
            return WRITEOFF_SOURCE_PREPAY_REPORT;
        }
        throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u652f\u6301\u6838\u9500\u5199\u5165");
    }

    /**
     * 瑙ｆ瀽閲戦銆?
     */
    private BigDecimal resolveCurrentAvailableWriteOffAmount(
            ProcessDocumentInstance target,
            String writeOffSourceKind,
            Map<String, BigDecimal> prepayAmountMap,
            Map<String, BigDecimal> effectiveAmountMap
    ) {
        BigDecimal baseAmount = Objects.equals(writeOffSourceKind, WRITEOFF_SOURCE_LOAN)
                ? defaultDecimal(target.getTotalAmount())
                : defaultDecimal(prepayAmountMap.get(target.getDocumentCode()));
        BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(target.getDocumentCode()));
        BigDecimal availableAmount = baseAmount.subtract(effectiveAmount);
        return availableAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableAmount;
    }

    /**
     * 鏍￠獙蹇収銆?
     */
    private String validateFlowSnapshotForSubmit(ProcessDocumentTemplate template) {
        String flowCode = trimToNull(template.getApprovalFlow());
        if (flowCode == null) {
            return null;
        }
        ProcessFlow flow = processFlowMapper.selectOne(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getFlowCode, flowCode)
                        .last("limit 1")
        );
        if (flow == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u5ba1\u6279\u6d41\u7a0b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u8865\u9f50\u914d\u7f6e\u540e\u518d\u63d0\u4ea4");
        }
        Long versionId = flow.getCurrentPublishedVersionId() != null
                ? flow.getCurrentPublishedVersionId()
                : flow.getCurrentDraftVersionId();
        if (versionId == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u5ba1\u6279\u6d41\u7a0b\u8fd8\u6ca1\u6709\u53ef\u7528\u7248\u672c\uff0c\u8bf7\u5148\u53d1\u5e03\u540e\u518d\u63d0\u4ea4");
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(versionId);
        String snapshotJson = version == null ? null : trimToNull(version.getSnapshotJson());
        if (snapshotJson == null) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u5ba1\u6279\u6d41\u7a0b\u5feb\u7167\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u91cd\u65b0\u53d1\u5e03\u540e\u518d\u63d0\u4ea4");
        }
        try {
            expenseWorkflowRuntimeSupport.validateFlowSnapshot(snapshotJson);
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("\u5f53\u524d\u6a21\u677f\u7ed1\u5b9a\u7684\u5ba1\u6279\u6d41\u7a0b\u5feb\u7167\u5df2\u635f\u574f\uff0c\u8bf7\u5148\u91cd\u65b0\u53d1\u5e03\u540e\u518d\u63d0\u4ea4", ex);
        }
        return snapshotJson;
    }

    /**
     * 鏋勫缓鎻愪氦鏁版嵁銆?
     */
    Map<String, Object> buildSubmitPayload(ProcessDocumentTemplate template) {
        return expenseDocumentMetadataSupport.buildSubmitPayload(template);
    }

    /**
     * 淇濆瓨瀹炰緥銆?
     */
    void saveExpenseDetailInstances(
            String documentCode,
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        if (!Objects.equals(trimToNull(template.getTemplateType()), "report") || expenseDetailDesign == null) {
            return;
        }
        log.info(
                "Expense submit stage=persist-expense-details documentCode={} templateCode={} detailCount={}",
                documentCode,
                template.getTemplateCode(),
                expenseDetails.size()
        );
        try {
            for (int index = 0; index < expenseDetails.size(); index++) {
                ExpenseDetailInstanceDTO expenseDetail = expenseDetails.get(index);
                String detailType = resolveExpenseDetailType(template, expenseDetailDesign);
                Map<String, Object> detailFormData = normalizeExpenseDetailFormData(
                        expenseDetail.getFormData(),
                        detailType,
                        template.getExpenseDetailModeDefault()
                );
                String businessSceneMode = resolveBusinessSceneModeForInstance(detailType, template, expenseDetail, detailFormData);
                if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE) && businessSceneMode == null) {
                    String detailName = firstNonBlank(expenseDetail.getDetailTitle(), expenseDetail.getDetailNo(), "\u8d39\u7528\u660e\u7ec6 " + (index + 1));
                    throw new IllegalArgumentException(detailName + "\u672a\u9009\u62e9\u4e1a\u52a1\u573a\u666f\uff0c\u8bf7\u5148\u8865\u5145\u540e\u518d\u63d0\u4ea4");
                }
                String detailName = firstNonBlank(expenseDetail.getDetailTitle(), expenseDetail.getDetailNo(), "\u8d39\u7528\u660e\u7ec6 " + (index + 1));
                String amountRuleMessage = ExpenseAmountResolver.validateResolvedExpenseDetailAmountRule(
                        detailFormData,
                        detailType,
                        businessSceneMode
                );
                if (amountRuleMessage != null) {
                    throw new IllegalArgumentException("\u8bf7\u5148\u5b8c\u5584\u8d39\u7528\u660e\u7ec6\u201c" + detailName + "\u201d\uff1a" + amountRuleMessage);
                }
                ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
                detail.setDocumentCode(documentCode);
                detail.setDetailNo(firstNonBlank(expenseDetail.getDetailNo(), buildExpenseDetailNo(documentCode, index + 1)));
                detail.setDetailDesignCode(expenseDetailDesign.getDetailCode());
                detail.setDetailType(detailType);
                detail.setEnterpriseMode(resolveEnterpriseModeForInstance(template, expenseDetailDesign, businessSceneMode));
                detail.setExpenseTypeCode(firstNonBlank(expenseDetail.getExpenseTypeCode(), stringValue(detailFormData.get(FIELD_EXPENSE_TYPE_CODE))));
                detail.setBusinessSceneMode(businessSceneMode);
                detail.setDetailTitle(firstNonBlank(expenseDetail.getDetailTitle(), "\u8d39\u7528\u660e\u7ec6 " + (index + 1)));
                detail.setSortOrder(expenseDetail.getSortOrder() == null ? index + 1 : expenseDetail.getSortOrder());
                BigDecimal invoiceAmount = readInvoiceAmountForStorage(detailType, businessSceneMode, detailFormData);
                BigDecimal actualPaymentAmount = toBigDecimal(detailFormData.get(FIELD_ACTUAL_PAYMENT_AMOUNT));
                detail.setInvoiceAmount(invoiceAmount);
                detail.setActualPaymentAmount(actualPaymentAmount);
                detail.setPendingWriteOffAmount(readPendingWriteOffAmountForStorage(detailType, businessSceneMode, detailFormData, actualPaymentAmount));
                detail.setSchemaSnapshotJson(expenseDetailDesign.getSchemaJson() == null ? writeJson(defaultSchema()) : expenseDetailDesign.getSchemaJson());
                detail.setFormDataJson(writeJson(detailFormData));
                detail.setCreatedAt(LocalDateTime.now());
                detail.setUpdatedAt(LocalDateTime.now());
                processDocumentExpenseDetailMapper.insert(detail);
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Expense submit detail persistence failed documentCode={} templateCode={} detailCount={} detailDesignCode={}",
                    documentCode,
                    template.getTemplateCode(),
                    expenseDetails.size(),
                    expenseDetailDesign.getDetailCode(),
                    ex
            );
            throw ex;
        }
    }

    /**
     * 鍔犺浇鏄庣粏銆?
     */
    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        );
    }

    List<ExpenseDetailInstanceSummaryVO> safeLoadExpenseDetailSummaries(String documentCode) {
        List<ExpenseDetailInstanceSummaryVO> summaries = new ArrayList<>();
        for (ProcessDocumentExpenseDetail expenseDetail : loadExpenseDetails(documentCode)) {
            if (expenseDetail == null) {
                log.error("Skip empty expense detail while building document detail documentCode={}", documentCode);
                continue;
            }
            try {
                summaries.add(toExpenseDetailSummaryVO(expenseDetail));
            } catch (Exception exception) {
                log.error(
                        "Skip broken expense detail while building document detail documentCode={} detailNo={}",
                        documentCode,
                        expenseDetail.getDetailNo(),
                        exception
                );
            }
        }
        return summaries;
    }

    private ExpenseDetailInstanceSummaryVO toExpenseDetailSummaryVO(ProcessDocumentExpenseDetail detail) {
        ExpenseDetailInstanceSummaryVO summary = new ExpenseDetailInstanceSummaryVO();
        summary.setDetailNo(detail.getDetailNo());
        summary.setDetailDesignCode(detail.getDetailDesignCode());
        summary.setDetailType(detail.getDetailType());
        summary.setDetailTypeLabel(resolveExpenseDetailTypeLabel(detail.getDetailType()));
        summary.setEnterpriseMode(detail.getEnterpriseMode());
        summary.setEnterpriseModeLabel(resolveEnterpriseModeLabel(detail.getEnterpriseMode()));
        summary.setExpenseTypeCode(detail.getExpenseTypeCode());
        summary.setBusinessSceneMode(detail.getBusinessSceneMode());
        summary.setDetailTitle(detail.getDetailTitle());
        summary.setSortOrder(detail.getSortOrder());
        summary.setCreatedAt(formatTime(detail.getCreatedAt()));
        return summary;
    }

    private ExpenseDetailInstanceDetailVO toExpenseDetailDetailVO(ProcessDocumentExpenseDetail detail, String fallbackPaymentCompanyId) {
        ExpenseDetailInstanceDetailVO vo = new ExpenseDetailInstanceDetailVO();
        vo.setDocumentCode(detail.getDocumentCode());
        vo.setDetailNo(detail.getDetailNo());
        vo.setDetailDesignCode(detail.getDetailDesignCode());
        vo.setDetailType(detail.getDetailType());
        vo.setDetailTypeLabel(resolveExpenseDetailTypeLabel(detail.getDetailType()));
        vo.setEnterpriseMode(detail.getEnterpriseMode());
        vo.setEnterpriseModeLabel(resolveEnterpriseModeLabel(detail.getEnterpriseMode()));
        vo.setExpenseTypeCode(detail.getExpenseTypeCode());
        vo.setBusinessSceneMode(detail.getBusinessSceneMode());
        vo.setDetailTitle(detail.getDetailTitle());
        vo.setSortOrder(detail.getSortOrder());
        Map<String, Object> schemaSnapshot = readMap(detail.getSchemaSnapshotJson());
        Map<String, Object> formData = readMap(detail.getFormDataJson());
        readonlyPayeeAccountSnapshotEnhancer.enhanceFormData(schemaSnapshot, formData, fallbackPaymentCompanyId);
        vo.setSchemaSnapshot(schemaSnapshot);
        vo.setFormData(formData);
        vo.setCreatedAt(formatTime(detail.getCreatedAt()));
        vo.setUpdatedAt(formatTime(detail.getUpdatedAt()));
        return vo;
    }

    ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        ProcessDocumentExpenseDetail detail = processDocumentExpenseDetailMapper.selectOne(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, trimToNull(documentCode))
                        .eq(ProcessDocumentExpenseDetail::getDetailNo, trimToNull(detailNo))
                        .last("limit 1")
        );
        if (detail == null) {
            throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u660e\u7ec6\u4e0d\u5b58\u5728");
        }
        return detail;
    }

    ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        ExpenseDetailInstanceDTO dto = new ExpenseDetailInstanceDTO();
        dto.setDetailNo(detail.getDetailNo());
        dto.setDetailDesignCode(detail.getDetailDesignCode());
        dto.setDetailType(detail.getDetailType());
        dto.setEnterpriseMode(detail.getEnterpriseMode());
        dto.setExpenseTypeCode(detail.getExpenseTypeCode());
        dto.setBusinessSceneMode(detail.getBusinessSceneMode());
        dto.setDetailTitle(detail.getDetailTitle());
        dto.setSortOrder(detail.getSortOrder());
        dto.setFormData(readMap(detail.getFormDataJson()));
        return dto;
    }


    ExpenseCreateTemplateSummaryVO toTemplateSummary(ProcessDocumentTemplate template, Map<String, String> categoryNameMap) {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        summary.setTemplateCode(template.getTemplateCode());
        summary.setTemplateName(template.getTemplateName());
        summary.setTemplateType(template.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        summary.setCategoryCode(template.getCategoryCode());
        summary.setCategoryName(categoryNameMap.get(trimToNull(template.getCategoryCode())));
        summary.setFormDesignCode(template.getFormDesignCode());
        return summary;
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
            String value = firstLookupValue(formData.get(fieldKey));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String extractPaymentDate(Map<String, Object> schema, Map<String, Object> formData) {
        if (schema == null || formData == null) {
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
            String label = trimToNull(String.valueOf(blockMap.get("label")));
            if (label == null || !PAYMENT_DATE_LABELS.contains(label)) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(CONTROL_TYPE_DATE, String.valueOf(props.get("controlType")))) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            String value = firstStringValue(formData.get(fieldKey));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<String> extractArchiveValues(Map<String, Object> schema, Map<String, Object> formData, String archiveCode) {
        if (schema == null || formData == null || trimToNull(archiveCode) == null) {
            return Collections.emptyList();
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            if (!Objects.equals(archiveCode, trimToNull(String.valueOf(props.get("archiveCode"))))) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey == null) {
                continue;
            }
            collectStringValues(values, formData.get(fieldKey));
        }
        return new ArrayList<>(values);
    }

    private void collectPartyLookupIds(String value, Set<Long> userIds, Set<String> vendorCodes) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return;
        }
        if (normalized.startsWith("USER:")) {
            Long userId = toLong(normalized.substring("USER:".length()));
            if (userId != null) {
                userIds.add(userId);
            }
            return;
        }
        collectVendorCode(normalized, vendorCodes);
    }

    private void collectVendorCode(String value, Set<String> vendorCodes) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return;
        }
        if (normalized.startsWith("VENDOR:")) {
            normalized = trimToNull(normalized.substring("VENDOR:".length()));
        }
        if (normalized != null) {
            vendorCodes.add(normalized);
        }
    }

    /**
     * 瑙ｆ瀽鍚嶇О銆?
     */
    private String resolvePaymentCompanyName(String companyId, Map<String, SystemCompany> companyMap) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            return null;
        }
        SystemCompany company = companyMap.get(normalized);
        return company == null ? normalized : firstNonBlank(company.getCompanyName(), company.getCompanyCode(), normalized);
    }

    /**
     * 瑙ｆ瀽鍚嶇О銆?
     */
    private String resolvePartyName(String value, Map<Long, User> userMap, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return trimToNull(normalized.substring(PERSONAL_PAYEE_VALUE_PREFIX.length()));
        }
        if (normalized.startsWith("USER:")) {
            Long userId = toLong(normalized.substring("USER:".length()));
            User user = userId == null ? null : userMap.get(userId);
            return user == null ? normalized : firstNonBlank(user.getName(), user.getUsername(), normalized);
        }
        return resolveVendorName(normalized, vendorMap);
    }

    /**
     * 瑙ｆ瀽鍚嶇О銆?
     */
    private String resolveVendorName(String value, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("VENDOR:")) {
            normalized = trimToNull(normalized.substring("VENDOR:".length()));
        }
        if (normalized == null) {
            return null;
        }
        FinanceVendor vendor = vendorMap.get(normalized);
        return vendor == null ? normalized : firstNonBlank(vendor.getCVenName(), vendor.getCVenAbbName(), normalized);
    }

    /**
     * 瑙ｆ瀽鍚嶇О銆?
     */
    private List<String> resolveDepartmentNames(List<String> departmentIds, Map<String, String> departmentNameMap) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String departmentId : departmentIds) {
            String normalized = trimToNull(departmentId);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(departmentNameMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }

    /**
     * 瑙ｆ瀽鍚嶇О銆?
     */
    private List<String> resolveArchiveItemNames(
            String archiveCode,
            List<String> values,
            Map<String, Map<String, String>> archiveItemLabelMap
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> labelMap = trimToNull(archiveCode) == null
                ? Collections.emptyMap()
                : archiveItemLabelMap.getOrDefault(archiveCode, Collections.emptyMap());
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(labelMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }

    private void collectStringValues(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = firstLookupValue(item);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = firstLookupValue(value);
        if (normalized != null) {
            result.add(normalized);
        }
    }

    private String firstStringValue(Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(item == null ? null : String.valueOf(item));
                if (normalized != null) {
                    return normalized;
                }
            }
            return null;
        }
        return trimToNull(value == null ? null : String.valueOf(value));
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
                    trimObjectToNull(map.get("value")),
                    trimObjectToNull(map.get("code")),
                    trimObjectToNull(map.get("id")),
                    trimObjectToNull(map.get("sourceCode"))
            );
            if (normalized != null) {
                return normalized;
            }
            return trimObjectToNull(map.get("label"));
        }
        return trimToNull(value == null ? null : String.valueOf(value));
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

    private String trimObjectToNull(Object value) {
        return trimToNull(value == null ? null : String.valueOf(value));
    }

    private Long toLong(String value) {
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

    ExpenseApprovalTaskVO toTaskVO(ProcessDocumentTask task) {
        ExpenseApprovalTaskVO vo = new ExpenseApprovalTaskVO();
        vo.setId(task.getId());
        vo.setDocumentCode(task.getDocumentCode());
        vo.setNodeKey(task.getNodeKey());
        vo.setNodeName(task.getNodeName());
        vo.setNodeType(task.getNodeType());
        vo.setAssigneeUserId(task.getAssigneeUserId());
        vo.setAssigneeName(task.getAssigneeName());
        vo.setStatus(task.getStatus());
        vo.setTaskBatchNo(task.getTaskBatchNo());
        vo.setApprovalMode(task.getApprovalMode());
        vo.setTaskKind(task.getTaskKind());
        vo.setSourceTaskId(task.getSourceTaskId());
        vo.setActionComment(task.getActionComment());
        vo.setCreatedAt(formatTime(task.getCreatedAt()));
        vo.setHandledAt(formatTime(task.getHandledAt()));
        return vo;
    }

    ExpenseApprovalLogVO toLogVO(ProcessDocumentActionLog log) {
        ExpenseApprovalLogVO vo = new ExpenseApprovalLogVO();
        vo.setId(log.getId());
        vo.setDocumentCode(log.getDocumentCode());
        vo.setNodeKey(log.getNodeKey());
        vo.setNodeName(log.getNodeName());
        vo.setActionType(log.getActionType());
        vo.setActorUserId(log.getActorUserId());
        vo.setActorName(log.getActorName());
        vo.setActionComment(log.getActionComment());
        vo.setPayload(readMap(log.getPayloadJson()));
        vo.setCreatedAt(formatTime(log.getCreatedAt()));
        return vo;
    }

    private void markDocumentApproved(ProcessDocumentInstance instance, String terminalStatus) {
        LocalDateTime now = LocalDateTime.now();
        persistDocumentRuntimeState(
                instance,
                defaultText(trimToNull(terminalStatus), DOCUMENT_STATUS_COMPLETED),
                null,
                null,
                null,
                now,
                now
        );
    }

    private void markDocumentException(ProcessDocumentInstance instance, ProcessFlowNodeDTO node, String reason) {
        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(DOCUMENT_STATUS_EXCEPTION);
        instance.setCurrentNodeKey(node == null ? null : node.getNodeKey());
        instance.setCurrentNodeName(node == null ? null : node.getNodeName());
        instance.setCurrentTaskType("EXCEPTION");
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        appendLog(instance.getDocumentCode(), node == null ? null : node.getNodeKey(), node == null ? null : node.getNodeName(), LOG_EXCEPTION, null, "SYSTEM", reason, Collections.emptyMap());
    }

    /**
     * 娓呯悊鐩稿叧淇℃伅銆?
     */
    private void clearCurrentNode(ProcessDocumentInstance instance) {
        persistDocumentRuntimeState(
                instance,
                instance.getStatus(),
                null,
                null,
                null,
                instance.getFinishedAt(),
                LocalDateTime.now()
        );
    }

    void persistDocumentRuntimeState(
            ProcessDocumentInstance instance,
            String status,
            String currentNodeKey,
            String currentNodeName,
            String currentTaskType,
            LocalDateTime finishedAt,
            LocalDateTime updatedAt
    ) {
        validatePmNameLength(currentNodeName, "\u5f53\u524d\u8282\u70b9\u540d\u79f0");
        instance.setStatus(status);
        instance.setCurrentNodeKey(currentNodeKey);
        instance.setCurrentNodeName(currentNodeName);
        instance.setCurrentTaskType(currentTaskType);
        instance.setFinishedAt(finishedAt);
        instance.setUpdatedAt(updatedAt);
        processDocumentInstanceMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ProcessDocumentInstance>()
                        .eq("id", instance.getId())
                        .set("status", status)
                        .set("current_node_key", currentNodeKey)
                        .set("current_node_name", currentNodeName)
                        .set("current_task_type", currentTaskType)
                        .set("finished_at", finishedAt)
                        .set("updated_at", updatedAt)
        );
    }

    void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long actorUserId,
            String actorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        expenseDocumentActionLogSupport.appendLog(
                documentCode,
                nodeKey,
                nodeName,
                actionType,
                actorUserId,
                actorName,
                actionComment,
                payload
        );
    }

    void assertCanViewDocument(ProcessDocumentInstance instance, Long userId, boolean allowCrossView) {
        if (Objects.equals(instance.getSubmitterUserId(), userId)) {
            return;
        }
        if (Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT) || !allowCrossView) {
            throw new IllegalStateException(MESSAGE_DOCUMENT_VIEW_FORBIDDEN);
        }
    }

    LocalDateTime resolveDisplaySubmittedAt(ProcessDocumentInstance instance) {
        if (instance == null) {
            return null;
        }
        if (Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)) {
            return instance.getUpdatedAt() == null ? instance.getCreatedAt() : instance.getUpdatedAt();
        }
        ProcessDocumentActionLog latestSubmitLog = processDocumentActionLogMapper.selectOne(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, instance.getDocumentCode())
                        .in(ProcessDocumentActionLog::getActionType, List.of(LOG_SUBMIT, LOG_RESUBMIT))
                        .orderByDesc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
                        .last("LIMIT 1")
        );
        return latestSubmitLog == null || latestSubmitLog.getCreatedAt() == null
                ? instance.getCreatedAt()
                : latestSubmitLog.getCreatedAt();
    }

    void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        if (!Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("\u53ea\u6709\u63d0\u5355\u4eba\u672c\u4eba\u53ef\u4ee5\u64cd\u4f5c\u5f53\u524d\u5355\u636e");
        }
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void resumeSourceTask(Long sourceTaskId, LocalDateTime now) {
        if (sourceTaskId == null) {
            return;
        }
        ProcessDocumentTask sourceTask = processDocumentTaskMapper.selectById(sourceTaskId);
        if (sourceTask == null || !Objects.equals(sourceTask.getStatus(), TASK_STATUS_PAUSED)) {
            return;
        }
        sourceTask.setStatus(TASK_STATUS_PENDING);
        sourceTask.setCreatedAt(now);
        sourceTask.setHandledAt(null);
        processDocumentTaskMapper.updateById(sourceTask);
    }

    ProcessDocumentTemplate requireTemplateForDocument(String templateCode) {
        ProcessDocumentTemplate template = loadTemplateByCode(templateCode, false);
        if (template == null) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u7ed1\u5b9a\u7684\u6a21\u677f\u4e0d\u5b58\u5728\uff0c\u65e0\u6cd5\u7ee7\u7eed\u5904\u7406");
        }
        return template;
    }

    /**
     * 鍔犺浇缂栫爜銆?
     */
    private ProcessDocumentTemplate loadTemplateByCode(String templateCode, boolean enabledOnly) {
        String normalizedCode = trimToNull(templateCode);
        if (normalizedCode == null) {
            return null;
        }
        return templateMapper.selectOne(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getTemplateCode, normalizedCode)
                        .eq(enabledOnly, ProcessDocumentTemplate::getEnabled, 1)
                        .last("limit 1")
        );
    }

    ProcessDocumentTemplate requireTemplate(String templateCode) {
        if (trimToNull(templateCode) == null) {
            throw new IllegalArgumentException("妯℃澘缂栫爜涓嶈兘涓虹┖");
        }
        ProcessDocumentTemplate template = loadTemplateByCode(templateCode, true);
        if (template == null) {
            throw new IllegalStateException("\u672a\u627e\u5230\u53ef\u7528\u6a21\u677f");
        }
        return template;
    }

    ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("鍗曟嵁缂栫爜涓嶈兘涓虹┖");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("\u672a\u627e\u5230\u5bf9\u5e94\u5355\u636e");
        }
        return instance;
    }

    private ProcessDocumentTask requireOpenPaymentTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("\u672a\u627e\u5230\u652f\u4ed8\u4efb\u52a1");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("\u5f53\u524d\u7528\u6237\u65e0\u6743\u5904\u7406\u8be5\u652f\u4ed8\u4efb\u52a1");
        }
        if (!NODE_TYPE_PAYMENT.equals(trimToNull(task.getNodeType()))) {
            throw new IllegalStateException("褰撳墠浠诲姟涓嶆槸鏀粯浠诲姟");
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus())) {
            throw new IllegalStateException("\u652f\u4ed8\u4efb\u52a1\u5df2\u5904\u7406");
        }
        return task;
    }

    /**
     * 鍔犺浇鐩稿叧淇℃伅銆?
     */
    ProcessFormDesign loadFormDesign(String formDesignCode) {
        String normalizedCode = trimToNull(formDesignCode);
        if (normalizedCode == null) {
            return null;
        }
        return processFormDesignMapper.selectOne(
                Wrappers.<ProcessFormDesign>lambdaQuery()
                        .eq(ProcessFormDesign::getFormCode, normalizedCode)
                        .last("limit 1")
        );
    }

    /**
     * 鍔犺浇鏄庣粏銆?
     */
    ProcessExpenseDetailDesign loadExpenseDetailDesign(String detailDesignCode) {
        String normalizedCode = trimToNull(detailDesignCode);
        if (normalizedCode == null) {
            return null;
        }
        return processExpenseDetailDesignMapper.selectOne(
                Wrappers.<ProcessExpenseDetailDesign>lambdaQuery()
                        .eq(ProcessExpenseDetailDesign::getDetailCode, normalizedCode)
                        .last("limit 1")
        );
    }

    /**
     * 鏋勫缓涓婁笅鏂囥€?
     */
    private Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        Map<String, Object> context = mergeRuntimeFormData(formData, expenseDetails);
        if (currentUser != null && currentUser.getId() != null) {
            context.put("submitterUserId", currentUser.getId());
        }
        if (currentUser != null && currentUser.getDeptId() != null) {
            context.put("submitterDeptId", currentUser.getDeptId());
        }
        BigDecimal amount = resolveTotalAmount(formData, expenseDetails, template.getExpenseDetailModeDefault());
        if (amount != null) {
            context.put("amount", amount);
        }
        String documentType = trimToNull(template.getTemplateType());
        if (documentType != null) {
            context.put("documentType", documentType);
        }
        String expenseTypeCode = firstNonBlank(stringValue(formData.get("expenseTypeCode")), trimToNull(template.getCategoryCode()));
        if (expenseTypeCode != null) {
            context.put("expenseTypeCode", expenseTypeCode);
        }
        List<String> undertakeDeptIds = resolveUndertakeDeptIds(formDesign, formData, expenseDetailDesign, expenseDetails);
        if (!undertakeDeptIds.isEmpty()) {
            context.put("undertakeDeptIds", undertakeDeptIds);
        }
        return context;
    }

    /**
     * 瑙ｆ瀽鐩稿叧淇℃伅銆?
     */
    private List<String> resolveUndertakeDeptIds(
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        Set<String> deptIds = new LinkedHashSet<>();
        collectUndertakeDeptIdsFromSchema(deptIds, formDesign == null ? null : readSchema(formDesign.getSchemaJson()), formData);
        if (expenseDetailDesign != null && expenseDetails != null) {
            Map<String, Object> schema = readSchema(expenseDetailDesign.getSchemaJson());
            for (ExpenseDetailInstanceDTO expenseDetail : expenseDetails) {
                collectUndertakeDeptIdsFromSchema(deptIds, schema, expenseDetail == null ? null : expenseDetail.getFormData());
            }
        }
        return new ArrayList<>(deptIds);
    }

    /**
     * 瑙ｆ瀽鐩稿叧淇℃伅銆?
     */
    private List<String> resolveUndertakeDeptIdsFromSnapshots(
            Map<String, Object> mainSchema,
            Map<String, Object> mainFormData,
            List<ProcessDocumentExpenseDetail> expenseDetails
    ) {
        Set<String> deptIds = new LinkedHashSet<>();
        collectUndertakeDeptIdsFromSchema(deptIds, mainSchema, mainFormData);
        if (expenseDetails != null) {
            for (ProcessDocumentExpenseDetail expenseDetail : expenseDetails) {
                collectUndertakeDeptIdsFromSchema(
                        deptIds,
                        readMap(expenseDetail.getSchemaSnapshotJson()),
                        readMap(expenseDetail.getFormDataJson())
                );
            }
        }
        return new ArrayList<>(deptIds);
    }

    private void collectUndertakeDeptIdsFromSchema(Set<String> result, Map<String, Object> schema, Map<String, Object> formData) {
        if (schema == null || formData == null || formData.isEmpty()) {
            return;
        }
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return;
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
            if (!Objects.equals(String.valueOf(props.get("componentCode")), UNDERTAKE_DEPARTMENT_COMPONENT_CODE)) {
                continue;
            }
            String fieldKey = trimToNull(String.valueOf(blockMap.get("fieldKey")));
            if (fieldKey != null) {
                collectDeptIds(result, formData.get(fieldKey));
            }
        }
    }

    /**
     * 鍚堝苟鐩稿叧淇℃伅銆?
     */
    private Map<String, Object> mergeRuntimeFormData(Map<String, Object> formData, List<ExpenseDetailInstanceDTO> expenseDetails) {
        Map<String, Object> merged = formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            return merged;
        }
        List<Map<String, Object>> detailFormDataList = expenseDetails.stream()
                .<Map<String, Object>>map(item -> item == null || item.getFormData() == null
                        ? new LinkedHashMap<String, Object>()
                        : new LinkedHashMap<>(item.getFormData()))
                .toList();
        merged.put("expenseDetails", detailFormDataList);
        merged.put("__expenseDetailCount", detailFormDataList.size());
        return merged;
    }

    private void collectDeptIds(Set<String> result, Object value) {
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = trimToNull(String.valueOf(item));
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return;
        }
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        if (normalized != null) {
            result.add(normalized);
        }
    }

    Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("琛ㄥ崟缁撴瀯瑙ｆ瀽澶辫触", ex);
        }
    }

    Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    /**
     * 鍔犺浇鐩稿叧淇℃伅銆?
     */
    List<ProcessCustomArchiveDetailVO> loadSharedArchives(Map<String, Object> schema) {
        Set<String> archiveCodes = extractArchiveCodes(schema);
        if (archiveCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .in(ProcessCustomArchiveDesign::getArchiveCode, archiveCodes)
                        .orderByAsc(ProcessCustomArchiveDesign::getId)
        );
        if (archives.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<ProcessCustomArchiveItem>> itemMap = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archives.stream().map(ProcessCustomArchiveDesign::getId).toList())
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveItem::getArchiveId,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        List<Long> itemIds = itemMap.values().stream().flatMap(List::stream).map(ProcessCustomArchiveItem::getId).toList();
        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = itemIds.isEmpty()
                ? Collections.emptyMap()
                : customArchiveRuleMapper.selectList(
                Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                        .in(ProcessCustomArchiveRule::getArchiveItemId, itemIds)
                        .orderByAsc(ProcessCustomArchiveRule::getGroupNo, ProcessCustomArchiveRule::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getArchiveItemId,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        return archives.stream().map(archive -> {
            ProcessCustomArchiveDetailVO detail = new ProcessCustomArchiveDetailVO();
            detail.setId(archive.getId());
            detail.setArchiveCode(archive.getArchiveCode());
            detail.setArchiveName(archive.getArchiveName());
            detail.setArchiveType(archive.getArchiveType());
            detail.setArchiveTypeLabel("AUTO_RULE".equals(archive.getArchiveType()) ? "\u81ea\u52a8\u89c4\u5219" : "\u624b\u52a8\u7ef4\u62a4");
            detail.setArchiveDescription(archive.getArchiveDescription());
            detail.setStatus(archive.getStatus());
            detail.setItems(itemMap.getOrDefault(archive.getId(), Collections.emptyList()).stream().map(item -> {
                ProcessCustomArchiveItemDTO dto = new ProcessCustomArchiveItemDTO();
                dto.setId(item.getId());
                dto.setItemCode(item.getItemCode());
                dto.setItemName(item.getItemName());
                dto.setPriority(item.getPriority());
                dto.setStatus(item.getStatus());
                dto.setRules(ruleMap.getOrDefault(item.getId(), Collections.emptyList()).stream().map(rule -> {
                    ProcessCustomArchiveRuleDTO ruleDto = new ProcessCustomArchiveRuleDTO();
                    ruleDto.setId(rule.getId());
                    ruleDto.setGroupNo(rule.getGroupNo());
                    ruleDto.setFieldKey(rule.getFieldKey());
                    ruleDto.setOperator(rule.getOperator());
                    ruleDto.setCompareValue(readJsonValue(rule.getCompareValue()));
                    return ruleDto;
                }).toList());
                return dto;
            }).toList());
            return detail;
        }).toList();
    }

    private Set<String> extractArchiveCodes(Map<String, Object> schema) {
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks)) {
            return Collections.emptySet();
        }
        Set<String> archiveCodes = new LinkedHashSet<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "SHARED_FIELD")) {
                continue;
            }
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> props)) {
                continue;
            }
            String archiveCode = trimToNull(String.valueOf(props.get("archiveCode")));
            if (archiveCode != null) {
                archiveCodes.add(archiveCode);
            }
        }
        return archiveCodes;
    }

    Map<String, Object> toTemplateSnapshot(ProcessDocumentTemplate template) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("templateCode", template.getTemplateCode());
        snapshot.put("templateName", template.getTemplateName());
        snapshot.put("templateType", template.getTemplateType());
        snapshot.put("templateTypeLabel", resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        snapshot.put("categoryCode", template.getCategoryCode());
        snapshot.put("templateDescription", template.getTemplateDescription());
        snapshot.put("formDesignCode", template.getFormDesignCode());
        snapshot.put("expenseDetailDesignCode", template.getExpenseDetailDesignCode());
        snapshot.put("expenseDetailModeDefault", template.getExpenseDetailModeDefault());
        snapshot.put("approvalFlowCode", template.getApprovalFlow());
        snapshot.put("flowName", template.getFlowName());
        return snapshot;
    }

    /**
     * 瑙ｆ瀽蹇収銆?
     */
    String resolveFlowSnapshotJson(ProcessDocumentTemplate template) {
        String flowCode = trimToNull(template.getApprovalFlow());
        if (flowCode == null) {
            return null;
        }
        ProcessFlow flow = processFlowMapper.selectOne(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getFlowCode, flowCode)
                        .last("limit 1")
        );
        if (flow == null) {
            return null;
        }
        Long versionId = flow.getCurrentPublishedVersionId() != null
                ? flow.getCurrentPublishedVersionId()
                : flow.getCurrentDraftVersionId();
        if (versionId == null) {
            return null;
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(versionId);
        return version == null ? null : version.getSnapshotJson();
    }

    List<ProcessFormOptionVO> loadUserOptions(Map<String, Object> flowSnapshot) {
        if (!hasManualSelectApprovalNode(flowSnapshot)) {
            return Collections.emptyList();
        }
        Map<Long, String> departmentNameMap = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                SystemDepartment::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getName, User::getId)
        ).stream().map(user -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            String deptName = user.getDeptId() == null ? null : departmentNameMap.get(user.getDeptId());
            String baseLabel = firstNonBlank(trimToNull(user.getName()), trimToNull(user.getUsername()), "\u672a\u547d\u540d\u7528\u6237");
            option.setLabel(baseLabel + (deptName == null ? "" : " / " + deptName));
            option.setValue(String.valueOf(user.getId()));
            return option;
        }).toList();
    }

    private boolean hasManualSelectApprovalNode(Map<String, Object> flowSnapshot) {
        Object rawNodes = flowSnapshot == null ? null : flowSnapshot.get("nodes");
        if (!(rawNodes instanceof Collection<?> nodes)) {
            return false;
        }
        for (Object rawNode : nodes) {
            if (!(rawNode instanceof Map<?, ?> nodeMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(nodeMap.get("nodeType")), "APPROVAL")) {
                continue;
            }
            Object rawConfig = nodeMap.get("config");
            if (!(rawConfig instanceof Map<?, ?> config)) {
                continue;
            }
            if (Objects.equals(String.valueOf(config.get("approverType")), "MANUAL_SELECT")) {
                return true;
            }
        }
        return false;
    }

    Map<String, List<Long>> normalizeManualApproverSelections(Map<String, List<Long>> source) {
        Map<String, List<Long>> result = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return result;
        }
        source.forEach((nodeKey, userIds) -> {
            String normalizedNodeKey = trimToNull(nodeKey);
            if (normalizedNodeKey == null || userIds == null || userIds.isEmpty()) {
                return;
            }
            List<Long> normalizedUserIds = userIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!normalizedUserIds.isEmpty()) {
                result.put(normalizedNodeKey, normalizedUserIds);
            }
        });
        return result;
    }

    Map<String, Object> resolveRejectRuntimeMetadata(ProcessDocumentInstance instance) {
        if (instance == null || trimToNull(instance.getDocumentCode()) == null) {
            return Collections.emptyMap();
        }
        ProcessDocumentActionLog rejectLog = loadActionLogs(instance.getDocumentCode()).stream()
                .filter(item -> Objects.equals(trimToNull(item.getActionType()), "REJECT"))
                .reduce((left, right) -> right)
                .orElse(null);
        if (rejectLog == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = readMap(rejectLog.getPayloadJson());
        String rejectedByNodeKey = trimToNull(firstNonBlank(
                stringValue(payload.get("rejectedByNodeKey")),
                rejectLog.getNodeKey()
        ));
        if (rejectedByNodeKey == null) {
            return Collections.emptyMap();
        }
        FlowRuntimeSnapshot snapshot = readFlowRuntimeSnapshot(instance.getFlowSnapshotJson());
        ProcessFlowNodeDTO rejectedByNode = snapshot.node(rejectedByNodeKey);
        if (rejectedByNode == null) {
            return Collections.emptyMap();
        }
        Set<String> specialSettings = toStringSet(rejectedByNode.getConfig() == null ? null : rejectedByNode.getConfig().get("specialSettings"));
        String targetNodeKey = trimToNull(stringValue(payload.get("targetNodeKey")));
        String resumeNodeKey = null;
        if (targetNodeKey != null) {
            if (snapshot.node(targetNodeKey) == null) {
                targetNodeKey = null;
            } else if (specialSettings.contains("DIRECT_REACH_AFTER_ANY_REJECT")) {
                resumeNodeKey = rejectedByNodeKey;
            }
        } else if (specialSettings.contains("DIRECT_REACH_AFTER_RESUBMIT")) {
            resumeNodeKey = rejectedByNodeKey;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("latestRejectNodeKey", rejectedByNodeKey);
        metadata.put("latestRejectTargetNodeKey", targetNodeKey);
        if (resumeNodeKey != null) {
            metadata.put("resumeNodeKey", resumeNodeKey);
        }
        return metadata;
    }

    FlowRuntimeSnapshot readFlowRuntimeSnapshot(String snapshotJson) {
        if (trimToNull(snapshotJson) == null) {
            return new FlowRuntimeSnapshot(Collections.emptyList(), Collections.emptyList());
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(snapshotJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            List<ProcessFlowNodeDTO> nodes = objectMapper.convertValue(
                    raw.getOrDefault("nodes", Collections.emptyList()),
                    new TypeReference<List<ProcessFlowNodeDTO>>() {}
            );
            List<ProcessFlowRouteDTO> routes = objectMapper.convertValue(
                    raw.getOrDefault("routes", Collections.emptyList()),
                    new TypeReference<List<ProcessFlowRouteDTO>>() {}
            );
            return new FlowRuntimeSnapshot(nodes, routes);
        } catch (Exception ex) {
            throw new IllegalStateException("\u6d41\u7a0b\u8fd0\u884c\u65f6\u5feb\u7167\u89e3\u6790\u5931\u8d25", ex);
        }
    }

    /**
     * 鍔犺浇閫夐」銆?
     */
    List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setLabel(item.getDeptName());
            option.setValue(String.valueOf(item.getId()));
            return option;
        }).toList();
    }

    /**
     * 鍔犺浇鏄庣粏銆?
     */
    List<ProcessFormOptionVO> loadDepartmentOptionsForDetail(Map<String, Object> schema, Map<String, Object> formData) {
        List<String> departmentIds = resolveUndertakeDeptIdsFromSnapshots(schema, formData, Collections.emptyList());
        if (departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> normalizedIds = departmentIds.stream()
                .map(this::asLong)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .in(SystemDepartment::getId, normalizedIds)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setLabel(item.getDeptName());
            option.setValue(String.valueOf(item.getId()));
            return option;
        }).toList();
    }

    /**
     * 鍔犺浇閫夐」銆?
     */
    List<ProcessFormOptionVO> loadCompanyOptions() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        ).stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setLabel(firstNonBlank(item.getCompanyName(), item.getCompanyCode(), item.getCompanyId()));
            option.setValue(item.getCompanyId());
            return option;
        }).toList();
    }

    /**
     * 鍔犺浇鏄庣粏銆?
     */
    List<ProcessFormOptionVO> loadCompanyOptionsForDetail(Map<String, Object> schema, Map<String, Object> formData) {
        String companyId = extractFirstBusinessComponentValue(schema, formData, PAYMENT_COMPANY_COMPONENT_CODE);
        if (companyId == null) {
            return Collections.emptyList();
        }
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .eq(SystemCompany::getCompanyId, companyId)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        ).stream().map(item -> {
            ProcessFormOptionVO option = new ProcessFormOptionVO();
            option.setLabel(firstNonBlank(item.getCompanyName(), item.getCompanyCode(), item.getCompanyId()));
            option.setValue(item.getCompanyId());
            return option;
        }).toList();
    }

    /**
     * 鍔犺浇浠诲姟銆?
     */
    List<ProcessDocumentTask> loadPendingTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    List<ProcessDocumentTask> loadAllTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    /**
     * 鍔犺浇浠诲姟銆?
     */
    List<ProcessDocumentTask> loadOpenTasks(String documentCode) {
        return expenseDocumentTaskRuntimeSupport.loadOpenTasks(documentCode);
    }

    /**
     * 鍔犺浇浠诲姟銆?
     */
    private List<ProcessDocumentTask> loadNodeOpenTasks(String documentCode, String nodeKey) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    /**
     * 鍔犺浇浠诲姟銆?
     */
    private List<ProcessDocumentTask> loadNodeBatchTasks(String documentCode, String nodeKey, String batchNo) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .eq(ProcessDocumentTask::getTaskBatchNo, batchNo)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    void cancelOpenTasks(List<ProcessDocumentTask> tasks, Long keepTaskId, LocalDateTime handledAt) {
        expenseDocumentTaskRuntimeSupport.cancelOpenTasks(tasks, keepTaskId, handledAt);
    }

    /**
     * 鍔犺浇鐩稿叧淇℃伅銆?
     */
    List<ProcessDocumentActionLog> loadActionLogs(String documentCode) {
        return expenseDocumentActionLogSupport.loadActionLogs(documentCode);
    }

    /**
     * 瑙ｆ瀽鏍囬銆?
     */
    String resolveDocumentTitle(ProcessDocumentTemplate template, Map<String, Object> formData, String username) {
        return expenseDocumentMetadataSupport.resolveDocumentTitle(template, formData, username);
    }

    /**
     * 瑙ｆ瀽浜嬬敱銆?
     */
    String resolveDocumentReason(ProcessDocumentTemplate template, Map<String, Object> formData) {
        return expenseDocumentMetadataSupport.resolveDocumentReason(template, formData);
    }

    /**
     * 瑙ｆ瀽閲戦銆?
     */
    BigDecimal resolveTotalAmount(
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails,
            String defaultBusinessSceneMode
    ) {
        return ExpenseAmountResolver.resolveDocumentTotalAmount(formData, expenseDetails, defaultBusinessSceneMode);
    }

    String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("\u6570\u636e\u5e8f\u5217\u5316\u5931\u8d25", ex);
        }
    }

    private Object readJsonValue(String rawValue) {
        if (trimToNull(rawValue) == null) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValue, Object.class);
        } catch (Exception ex) {
            return rawValue;
        }
    }

    Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("JSON 鏄犲皠瑙ｆ瀽澶辫触", ex);
        }
    }

    Map<String, Object> readFormData(String json) {
        return readMap(json);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private int compareNumbers(Object actual, Object compare) {
        BigDecimal left = toBigDecimal(actual);
        BigDecimal right = toBigDecimal(compare);
        if (left == null || right == null) {
            return 0;
        }
        return left.compareTo(right);
    }

    private boolean between(Object actual, Object compare) {
        BigDecimal current = toBigDecimal(actual);
        if (current == null) {
            return false;
        }
        List<Object> range = toObjectList(compare);
        if (range.size() < 2) {
            return false;
        }
        BigDecimal start = toBigDecimal(range.get(0));
        BigDecimal end = toBigDecimal(range.get(1));
        if (start == null || end == null) {
            return false;
        }
        return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
    }

    private boolean containsValue(Object actual, Object compare) {
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        String actualText = trimToNull(String.valueOf(actual));
        String compareText = trimToNull(String.valueOf(compare));
        return actualText != null && compareText != null && actualText.contains(compareText);
    }

    private boolean anyIn(Object actual, Object compare, boolean defaultResult) {
        List<Object> compareList = toObjectList(compare);
        if (compareList.isEmpty()) {
            return defaultResult;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> compareList.stream().anyMatch(candidate -> valuesEqual(item, candidate)));
        }
        return compareList.stream().anyMatch(candidate -> valuesEqual(actual, candidate));
    }

    private boolean valuesEqual(Object actual, Object compare) {
        BigDecimal leftNumber = toBigDecimal(actual);
        BigDecimal rightNumber = toBigDecimal(compare);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        if (actual instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> valuesEqual(item, compare));
        }
        return Objects.equals(defaultText(asText(actual), ""), defaultText(asText(compare), ""));
    }

    private List<Object> toObjectList(Object value) {
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value == null) {
            return new ArrayList<>();
        }
        return List.of(value);
    }

    private Set<String> toStringSet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Collections.emptySet();
        }
        return collection.stream()
                .map(this::stringValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 鏋勫缓缂栫爜銆?
     */
    String buildDocumentCode() {
        String prefix = "DOC" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processDocumentInstanceMapper.selectCount(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .likeRight(ProcessDocumentInstance::getDocumentCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    /**
     * 鏋勫缓鐩稿叧淇℃伅銆?
     */
    private String buildReceiptContent(ProcessDocumentInstance instance, PmBankPaymentRecord record, SystemCompanyBankAccount account) {
        List<String> lines = new ArrayList<>();
        lines.add("\u62db\u5546\u94f6\u884c\u4e91\u76f4\u8fde\u56de\u5355");
        lines.add("\u5355\u636e\u7f16\u53f7\uff1a" + defaultText(instance.getDocumentCode(), "-"));
        lines.add("\u5355\u636e\u540d\u79f0\uff1a" + defaultText(instance.getDocumentTitle(), "-"));
        lines.add("\u4ed8\u6b3e\u8d26\u6237\uff1a" + defaultText(buildCompanyBankAccountName(account), "-"));
        lines.add("\u94f6\u884c\u8ba2\u5355\u53f7\uff1a" + defaultText(trimToNull(record.getBankOrderNo()), "-"));
        lines.add("\u94f6\u884c\u6d41\u6c34\u53f7\uff1a" + defaultText(trimToNull(record.getBankFlowNo()), "-"));
        lines.add("\u652f\u4ed8\u65f6\u95f4\uff1a" + defaultText(formatTime(record.getPaidAt()), "-"));
        lines.add("\u751f\u6210\u65f6\u95f4\uff1a" + formatTime(LocalDateTime.now()));
        return String.join(System.lineSeparator(), lines);
    }

    /**
     * 鏋勫缓鍚嶇О銆?
     */
    private String buildReceiptFileName(String documentCode) {
        return defaultText(documentCode, "document") + "-\u62db\u5546\u94f6\u884c\u4e91\u76f4\u8fde\u56de\u5355.txt";
    }

    /**
     * 鍔犺浇鏄犲皠銆?
     */
    private Map<String, PmBankPaymentRecord> loadLatestBankRecordMap(List<String> documentCodes) {
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

    /**
     * 鍔犺浇璐︽埛銆?
     */
    private Map<Long, PmBankPaymentRecord> loadLatestBankRecordByAccountId(Set<Long> companyBankAccountIds) {
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

    /**
     * 鍔犺浇鏄犲皠銆?
     */
    Map<Long, String> loadCompanyBankAccountNameMap(Set<Long> companyBankAccountIds) {
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

    ExpenseDocumentBankPaymentVO toDetailBankPayment(
            PmBankPaymentRecord record,
            String companyBankAccountName,
            String documentStatus
    ) {
        ExpenseDocumentBankPaymentVO item = new ExpenseDocumentBankPaymentVO();
        item.setBankProvider(record.getBankProvider());
        item.setBankChannel(record.getBankChannel());
        item.setCompanyBankAccountName(companyBankAccountName);
        item.setPaymentStatusCode(documentStatus);
        item.setPaymentStatusLabel(resolveStatusLabel(documentStatus));
        item.setManualPaid(isFlagEnabled(record.getManualPaid()));
        item.setPaidAt(formatTime(record.getPaidAt()));
        item.setReceiptStatusLabel(resolveReceiptStatusLabel(record));
        item.setReceiptReceivedAt(formatTime(record.getReceiptReceivedAt()));
        item.setBankFlowNo(record.getBankFlowNo());
        item.setBankOrderNo(record.getBankOrderNo());
        item.setLastErrorMessage(record.getLastErrorMessage());
        return item;
    }

    List<ExpenseDocumentBankReceiptVO> toDetailBankReceipts(PmBankPaymentRecord record) {
        if (trimToNull(record.getReceiptAttachmentId()) == null) {
            return Collections.emptyList();
        }
        ExpenseAttachmentService.StoredExpenseAttachment attachment = expenseAttachmentService.loadAttachment(record.getReceiptAttachmentId());
        ExpenseDocumentBankReceiptVO item = new ExpenseDocumentBankReceiptVO();
        item.setAttachmentId(record.getReceiptAttachmentId());
        item.setFileName(firstNonBlank(record.getReceiptFileName(), attachment.fileName()));
        item.setContentType(attachment.contentType());
        item.setFileSize(attachment.fileSize());
        item.setPreviewUrl("/api/auth/expenses/attachments/" + record.getReceiptAttachmentId() + "/content");
        item.setReceivedAt(formatTime(record.getReceiptReceivedAt()));
        return List.of(item);
    }

    /**
     * 鏌ユ壘閾惰銆?
     */
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

    private void disableOtherEnabledBankLinks(SystemCompanyBankAccount currentAccount) {
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

    private SystemCompanyBankAccount requireCompanyBankAccount(Long companyBankAccountId) {
        SystemCompanyBankAccount account = systemCompanyBankAccountMapper.selectById(companyBankAccountId);
        if (account == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u8d26\u6237\u4e0d\u5b58\u5728");
        }
        return account;
    }

    /**
     * 瑙ｆ瀽鐩稿叧淇℃伅銆?
     */
    private boolean resolveCallbackSuccess(ExpenseBankCallbackDTO dto) {
        if (dto == null) {
            return false;
        }
        if (dto.getSuccess() != null) {
            return dto.getSuccess();
        }
        String resultCode = defaultText(trimToNull(dto.getResultCode()), "");
        return Set.of("SUCCESS", "ACCEPTED", "00", "200").contains(resultCode);
    }

    /**
     * 瑙ｆ瀽鏍囩銆?
     */
    private String resolveBankLinkStatusLabel(SystemCompanyBankAccount account) {
        if (!isFlagEnabled(account.getDirectConnectEnabled())) {
            return "\u672a\u542f\u7528";
        }
        if (!BANK_PROVIDER_CMB.equals(trimToNull(account.getDirectConnectProvider()))
                || !BANK_CHANNEL_CMB_CLOUD.equals(trimToNull(account.getDirectConnectChannel()))) {
            return "\u672a\u914d\u7f6e";
        }
        return "\u5df2\u542f\u7528";
    }

    /**
     * 瑙ｆ瀽鐘舵€併€?
     */
    private String resolveBankLinkSyncStatus(SystemCompanyBankAccount account) {
        String status = trimToNull(account.getDirectConnectLastSyncStatus());
        return status == null ? "\u672a\u63a8\u9001" : status;
    }

    /**
     * 瑙ｆ瀽鏍囩銆?
     */
    private String resolveReceiptStatusLabel(PmBankPaymentRecord record) {
        if (record == null) {
            return "\u672a\u751f\u6210";
        }
        if (isFlagEnabled(record.getManualPaid()) && trimToNull(record.getReceiptAttachmentId()) == null) {
            return "\u624b\u52a8\u5df2\u652f\u4ed8";
        }
        return switch (defaultText(trimToNull(record.getReceiptStatus()), RECEIPT_STATUS_PENDING)) {
            case RECEIPT_STATUS_RECEIVED -> "\u5df2\u83b7\u53d6\u56de\u5355";
            case RECEIPT_STATUS_FAILED -> "\u56de\u5355\u67e5\u8be2\u5931\u8d25";
            default -> "\u5f85\u67e5\u8be2\u56de\u5355";
        };
    }

    /**
     * 鍒ゆ柇鐩稿叧淇℃伅銆?
     */
    private boolean isReceiptQueryEnabled(SystemCompanyBankAccount account) {
        if (account == null) {
            return false;
        }
        return Boolean.parseBoolean(readBankLinkExt(account).getOrDefault("receiptQueryEnabled", "false"));
    }

    private Map<String, String> readBankLinkExt(SystemCompanyBankAccount account) {
        Map<String, Object> ext = readMap(account == null ? null : account.getDirectConnectExtJson());
        Map<String, String> result = new LinkedHashMap<>();
        ext.forEach((key, value) -> result.put(key, value == null ? "" : String.valueOf(value)));
        return result;
    }

    /**
     * 鏋勫缓鍚嶇О銆?
     */
    private String buildCompanyBankAccountName(SystemCompanyBankAccount account) {
        if (account == null) {
            return null;
        }
        String tailNo = trimToNull(account.getAccountNo());
        String suffix = tailNo == null || tailNo.length() <= 4 ? tailNo : tailNo.substring(tailNo.length() - 4);
        return account.getAccountName() + (suffix == null ? "" : "(\u5c3e\u53f7 " + suffix + ")");
    }

    /**
     * 鏋勫缓閾惰銆?
     */
    private String buildBankPushRequestNo(String documentCode) {
        return defaultText(documentCode, "DOC") + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * 鏌ユ壘鍚嶇О銆?
     */
    private String findCompanyName(String companyId) {
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

    /**
     * 鏋勫缓鍏徃鍚嶇О鏄犲皠銆?
     */
    private Map<String, String> buildCompanyNameMap(Set<String> companyIds) {
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

    private void requireNotBlank(String value, String message) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 鍒ゆ柇鏍囪鏄惁鍚敤銆?
     */
    private boolean isFlagEnabled(Integer value) {
        return value != null && value == 1;
    }

    private LocalDateTime parseFlexibleDateTime(String rawValue, LocalDateTime defaultValue) {
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
                // 灏濊瘯涓嬩竴涓牸寮忓寲鍣?
            }
        }
        return defaultValue;
    }

    /**
     * 鏋勫缓璐圭敤鏄庣粏缂栧彿銆?
     */
    private String buildExpenseDetailNo(String documentCode, int sortOrder) {
        return documentCode + "-D" + String.format("%02d", sortOrder);
    }

    /**
     * 鏋勫缓浠诲姟鎵规鍙枫€?
     */
    private String buildTaskBatchNo(String documentCode, String nodeKey) {
        return documentCode + "-" + nodeKey + "-" + System.currentTimeMillis();
    }

    /**
     * 瑙ｆ瀽妯℃澘绫诲瀷鏍囩銆?
     */
    String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "\u7533\u8bf7\u5355";
            case "loan" -> "\u501f\u6b3e\u5355";
            case "contract" -> "\u5408\u540c\u5355";
            default -> "\u62a5\u9500\u5355";
        };
    }

    /**
     * 鍒ゆ柇鏄惁涓烘湁鏁堝凡瀹℃壒鐘舵€併€?
     */
    boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    private boolean isRelationSelectableStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYING.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    /**
     * 瑙ｆ瀽鐘舵€佹爣绛俱€?
     */
    String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED, DOCUMENT_STATUS_COMPLETED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case "DRAFT" -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u5ba1\u6279\u4e2d";
        };
    }

    /**
     * 瑙ｆ瀽鏄庣粏銆?
     */
    String resolveExpenseDetailType(ProcessDocumentTemplate template, ProcessExpenseDetailDesign expenseDetailDesign) {
        if (expenseDetailDesign != null && trimToNull(expenseDetailDesign.getDetailType()) != null) {
            return expenseDetailDesign.getDetailType();
        }
        return DETAIL_TYPE_NORMAL;
    }

    /**
     * 瑙ｆ瀽鏍囩銆?
     */
    String resolveExpenseDetailTypeLabel(String detailType) {
        return Objects.equals(trimToNull(detailType), DETAIL_TYPE_ENTERPRISE)
                ? "\u4f01\u4e1a\u5f80\u6765"
                : "\u666e\u901a\u62a5\u9500";
    }

    /**
     * 瑙ｆ瀽瀹炰緥銆?
     */
    private String resolveEnterpriseModeForInstance(ProcessDocumentTemplate template, ProcessExpenseDetailDesign expenseDetailDesign, String runtimeMode) {
        if (!Objects.equals(resolveExpenseDetailType(template, expenseDetailDesign), DETAIL_TYPE_ENTERPRISE)) {
            return null;
        }
        String normalizedMode = trimToNull(runtimeMode);
        if (normalizedMode == null) {
            normalizedMode = trimToNull(template.getExpenseDetailModeDefault());
        }
        if (!Objects.equals(normalizedMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
                && !Objects.equals(normalizedMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return null;
        }
        return normalizedMode;
    }

    /**
     * 瑙ｆ瀽鏍囩銆?
     */
    private String resolveEnterpriseModeLabel(String enterpriseMode) {
        if (Objects.equals(trimToNull(enterpriseMode), ENTERPRISE_MODE_PREPAY_UNBILLED)) {
            return "\u9884\u4ed8\u672a\u5230\u7968";
        }
        if (Objects.equals(trimToNull(enterpriseMode), ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return "\u5230\u7968\u5168\u989d\u652f\u4ed8";
        }
        return "";
    }

    private Map<String, Object> normalizeExpenseDetailFormData(
            Map<String, Object> formData,
            String detailType,
            String defaultBusinessSceneMode
    ) {
        Map<String, Object> normalized = formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
        String businessSceneMode = resolveBusinessSceneMode(detailType, normalized.get(FIELD_BUSINESS_SCENARIO), defaultBusinessSceneMode);
        if (businessSceneMode != null) {
            normalized.put(FIELD_BUSINESS_SCENARIO, businessSceneMode);
        } else if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            normalized.put(FIELD_BUSINESS_SCENARIO, "");
        }
        normalized.remove(FIELD_PENDING_WRITE_OFF_AMOUNT);
        return normalized;
    }

    /**
     * 瑙ｆ瀽瀹炰緥銆?
     */
    private String resolveBusinessSceneModeForInstance(
            String detailType,
            ProcessDocumentTemplate template,
            ExpenseDetailInstanceDTO expenseDetail,
            Map<String, Object> detailFormData
    ) {
        String businessSceneMode = resolveBusinessSceneMode(
                detailType,
                firstNonBlank(
                        expenseDetail.getBusinessSceneMode(),
                        stringValue(detailFormData.get(FIELD_BUSINESS_SCENARIO)),
                        expenseDetail.getEnterpriseMode()
                ),
                template.getExpenseDetailModeDefault()
        );
        if (businessSceneMode != null) {
            detailFormData.put(FIELD_BUSINESS_SCENARIO, businessSceneMode);
        } else if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            detailFormData.put(FIELD_BUSINESS_SCENARIO, "");
        }
        return businessSceneMode;
    }

    /**
     * 瑙ｆ瀽鐩稿叧淇℃伅銆?
     */
    private String resolveBusinessSceneMode(String detailType, Object rawMode, String defaultBusinessSceneMode) {
        if (!Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            return ENTERPRISE_MODE_INVOICE_FULL_PAYMENT;
        }
        String normalizedMode = trimToNull(rawMode == null ? null : String.valueOf(rawMode));
        if (Objects.equals(normalizedMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
                || Objects.equals(normalizedMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return normalizedMode;
        }
        normalizedMode = trimToNull(defaultBusinessSceneMode);
        if (Objects.equals(normalizedMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
                || Objects.equals(normalizedMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return normalizedMode;
        }
        return null;
    }

    private BigDecimal readInvoiceAmountForStorage(String detailType, String businessSceneMode, Map<String, Object> formData) {
        if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)
                && !Objects.equals(businessSceneMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return null;
        }
        return toBigDecimal(formData.get(FIELD_INVOICE_AMOUNT));
    }

    private BigDecimal readPendingWriteOffAmountForStorage(
            String detailType,
            String businessSceneMode,
            Map<String, Object> formData,
            BigDecimal actualPaymentAmount
    ) {
        if (!Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)
                || !Objects.equals(businessSceneMode, ENTERPRISE_MODE_PREPAY_UNBILLED)) {
            return null;
        }
        return ExpenseAmountResolver.resolvePrepayWriteOffAmount(formData, actualPaymentAmount);
    }

    /**
     * 鏋勫缓鏍囩銆?
     */
    private String buildAccountLabel(String accountName, String bankName) {
        String left = firstNonBlank(accountName, bankName);
        String right = left != null && Objects.equals(left, trimToNull(bankName)) ? null : trimToNull(bankName);
        return right == null ? (left == null ? "\u672a\u547d\u540d\u8d26\u6237" : left) : left + " / " + right;
    }

    /**
     * 鏋勫缓渚涘簲鍟嗚处鎴疯ˉ鍏呬俊鎭€?
     */
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return firstNonBlank(
                    stringValue(map.get("value")),
                    stringValue(map.get("label")),
                    stringValue(map.get("text"))
            );
        }
        return trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String defaultReason(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "\u6682\u65e0\u4e8b\u7531" : normalized;
    }

    private String defaultUsername(String username) {
        String normalized = trimToNull(username);
        return normalized == null ? "\u672a\u77e5\u7528\u6237" : normalized;
    }

    /**
     * 鍔犺浇鍏ㄩ儴鍚敤閮ㄩ棬骞舵寜 ID 寤虹珛鏄犲皠銆?
     */
    private Map<Long, SystemDepartment> loadAllDepartmentMap() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getStatus, 1)
        ).stream().collect(Collectors.toMap(
                SystemDepartment::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private User requireActiveUser(Long userId) {
        User user = loadActiveUser(userId);
        if (user == null) {
            throw new IllegalStateException("\u5f53\u524d\u7528\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u88ab\u7981\u7528\uff0c\u65e0\u6cd5\u7ee7\u7eed\u5904\u7406");
        }
        return user;
    }

    private String requireCurrentUserCompanyId(Long userId) {
        User user = requireActiveUser(userId);
        String companyId = trimToNull(user.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("\u5f53\u524d\u7528\u6237\u672a\u914d\u7f6e\u6240\u5c5e\u516c\u53f8\uff0c\u65e0\u6cd5\u7ee7\u7eed\u5904\u7406");
        }
        return companyId;
    }

    private String resolveExpenseCreateCompanyId(Long userId, String paymentCompanyId) {
        String explicitCompanyId = trimToNull(paymentCompanyId);
        return explicitCompanyId != null ? explicitCompanyId : requireCurrentUserCompanyId(userId);
    }

    /**
     * 鍔犺浇鍚敤涓殑鐢ㄦ埛銆?
     */
    User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

    /**
     * 鎵归噺鍔犺浇鍚敤涓殑鐢ㄦ埛銆?
     */
    private List<User> loadActiveUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    private SystemDepartment climbDepartment(SystemDepartment start, Map<Long, SystemDepartment> departmentMap, int steps) {
        SystemDepartment current = start;
        for (int index = 0; index < steps && current != null; index++) {
            current = current.getParentId() == null ? null : departmentMap.get(current.getParentId());
        }
        return current;
    }

    private String normalizeUserName(User user) {
        if (user == null) {
            return "\u672a\u547d\u540d\u7528\u6237";
        }
        String name = user.getName();
        if (name != null) {
            name = name.trim();
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        String username = user.getUsername();
        if (username != null) {
            username = username.trim();
        }
        if (username != null && !username.isEmpty()) {
            return username;
        }
        return "\u672a\u547d\u540d\u7528\u6237";
    }

    /**
     * 瑙ｆ瀽鐢ㄦ埛灞曠ず濮撳悕锛屼紭鍏堝彇鐪熷疄濮撳悕锛岀己澶辨椂鍥為€€璐﹀彿鍚嶃€?
     */
    String resolveUserDisplayName(Long userId, String username) {
        return resolveUserDisplayName(loadActiveUser(userId), username);
    }

    /**
     * 瑙ｆ瀽鐢ㄦ埛灞曠ず濮撳悕锛屼紭鍏堝彇鐪熷疄濮撳悕锛岀己澶辨椂鍥為€€璐﹀彿鍚嶃€?
     */
    String resolveUserDisplayName(User user, String username) {
        String displayName = user != null ? normalizeUserName(user) : defaultUsername(username);
        validatePmNameLength(displayName, "\u63d0\u4ea4\u4eba\u59d3\u540d");
        return displayName;
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Long parsed = asLong(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }
        Long parsed = asLong(value);
        if (parsed != null) {
            result.add(parsed);
        }
        return result;
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int clampLevel(Integer value) {
        int level = value == null ? 1 : value;
        if (level < 1) {
            return 1;
        }
        return Math.min(level, 10);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    void validatePmNameLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalStateException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
        }
    }

    void validatePmTitleLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_TITLE_MAX_LENGTH) {
            throw new IllegalStateException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 128 \u4e2a\u5b57\u7b26");
        }
    }

    private void validatePmFieldKeyLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException(label + "\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (normalized.length() > PM_FIELD_KEY_MAX_LENGTH) {
            throw new IllegalStateException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
        }
    }

    String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    record DocumentMutationContext(
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            ProcessExpenseDetailDesign expenseDetailDesign,
            Map<String, Object> formData,
            List<ExpenseDetailInstanceDTO> expenseDetails,
            String flowSnapshotJson,
            Map<String, Object> runtimeContext,
            String documentTitle,
            String documentReason,
            BigDecimal totalAmount
    ) {
    }

    private record DocumentBusinessBinding(
            String fieldKey,
            String componentCode,
            List<String> allowedTemplateTypes
    ) {
    }

    private record RelatedDocumentSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            int sortOrder
    ) {
    }

    private record WriteOffSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            BigDecimal requestedAmount,
            int sortOrder
    ) {
    }

    private record DocumentBusinessTargetKey(
            String fieldKey,
            String documentCode
    ) {
    }


}
