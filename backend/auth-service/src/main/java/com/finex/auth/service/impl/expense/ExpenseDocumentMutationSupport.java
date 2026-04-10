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
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDetailInstanceSummaryVO;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentBankPaymentVO;
import com.finex.auth.dto.ExpenseDocumentBankReceiptVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerGroupVO;
import com.finex.auth.dto.ExpenseDocumentPickerItemVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
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
import com.finex.auth.entity.ProcessTemplateScope;
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
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseDocumentMutationSupport {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String UNDERTAKE_DEPARTMENT_COMPONENT_CODE = "undertake-department";
    private static final String PAYMENT_COMPANY_COMPONENT_CODE = "payment-company";
    private static final String PAYEE_COMPONENT_CODE = "payee";
    private static final String COUNTERPARTY_COMPONENT_CODE = "counterparty";
    private static final String PERSONAL_PAYEE_VALUE_PREFIX = "PERSONAL_PAYEE:";
    private static final String PAYEE_SOURCE_PERSONAL = "PERSONAL_PRIVATE_PAYEE";
    private static final String CONTROL_TYPE_DATE = "DATE";
    private static final Set<String> PAYMENT_DATE_LABELS = Set.of("鏀粯鏃ユ湡", "浠樻鏃ユ湡");
    private static final String TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE = "TAG_ARCHIVE";
    private static final String DETAIL_TYPE_NORMAL = "NORMAL_REIMBURSEMENT";
    private static final String DETAIL_TYPE_ENTERPRISE = "ENTERPRISE_TRANSACTION";
    private static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    private static final String ENTERPRISE_MODE_INVOICE_FULL_PAYMENT = "INVOICE_FULL_PAYMENT";
    private static final String INVOICE_FREE_MODE_REQUIRED = "NOT_FREE";
    private static final String FIELD_EXPENSE_TYPE_CODE = ExpenseDetailSystemFieldSupport.FIELD_EXPENSE_TYPE_CODE;
    private static final String FIELD_BUSINESS_SCENARIO = ExpenseDetailSystemFieldSupport.FIELD_BUSINESS_SCENARIO;
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
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_DRAFT = "DRAFT";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";

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
    private static final String FLOW_FINISH_COMMENT = "Approval flow finished";
    private static final String ROOT_CONTAINER_KEY = "__ROOT__";
    private static final int NAVIGATION_HISTORY_LIMIT = 200;

    private final ProcessDocumentTemplateMapper templateMapper;
    private final ProcessFormDesignMapper processFormDesignMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    private final ProcessFlowMapper processFlowMapper;
    private final ProcessFlowVersionMapper processFlowVersionMapper;
    private final ProcessTemplateScopeMapper processTemplateScopeMapper;
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

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .eq(ProcessDocumentTemplate::getEnabled, 1)
                        .orderByAsc(ProcessDocumentTemplate::getSortOrder, ProcessDocumentTemplate::getId)
        ).stream()
                .filter(this::isTemplateAvailableForCreate)
                .map(this::toTemplateSummary)
                .toList();
    }

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        ProcessDocumentTemplate template = requireTemplate(templateCode);
        return buildTemplateDetail(userId, template);
    }

    private ExpenseCreateTemplateDetailVO buildTemplateDetail(Long userId, ProcessDocumentTemplate template) {
        ExpenseCreateTemplateDetailVO detail = new ExpenseCreateTemplateDetailVO();
        detail.setTemplateCode(template.getTemplateCode());
        detail.setTemplateName(template.getTemplateName());
        detail.setTemplateType(template.getTemplateType());
        detail.setTemplateTypeLabel(resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        detail.setCategoryCode(template.getCategoryCode());
        detail.setTemplateDescription(template.getTemplateDescription());
        detail.setFormDesignCode(template.getFormDesignCode());
        detail.setApprovalFlowCode(template.getApprovalFlow());
        detail.setFlowName(template.getFlowName());
        detail.setExpenseDetailDesignCode(template.getExpenseDetailDesignCode());
        detail.setExpenseDetailModeDefault(template.getExpenseDetailModeDefault());

        ProcessFormDesign formDesign = loadFormDesign(template.getFormDesignCode());
        if (formDesign != null) {
            detail.setFormName(formDesign.getFormName());
            detail.setSchema(readSchema(formDesign.getSchemaJson()));
            detail.setSharedArchives(loadSharedArchives(detail.getSchema()));
        } else {
            detail.setSchema(defaultSchema());
            detail.setSharedArchives(Collections.emptyList());
        }

        ProcessExpenseDetailDesign expenseDetailDesign = loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
        if (expenseDetailDesign != null) {
            detail.setExpenseDetailDesignName(expenseDetailDesign.getDetailName());
            detail.setExpenseDetailType(expenseDetailDesign.getDetailType());
            detail.setExpenseDetailTypeLabel(resolveExpenseDetailTypeLabel(expenseDetailDesign.getDetailType()));
            detail.setExpenseDetailSchema(expenseDetailSystemFieldSupport.readSchema(expenseDetailDesign.getSchemaJson(), expenseDetailDesign.getDetailType()));
            detail.setExpenseDetailSharedArchives(loadSharedArchives(detail.getExpenseDetailSchema()));
        } else {
            detail.setExpenseDetailType(resolveExpenseDetailType(template, null));
            detail.setExpenseDetailTypeLabel(resolveExpenseDetailTypeLabel(detail.getExpenseDetailType()));
            detail.setExpenseDetailSchema(defaultSchema());
            detail.setExpenseDetailSharedArchives(Collections.emptyList());
        }
        detail.setCompanyOptions(loadCompanyOptions());
        detail.setDepartmentOptions(loadDepartmentOptions());
        detail.setExpenseTypeOptions(expenseDetailSystemFieldSupport.loadExpenseTypeOptions());
        detail.setExpenseTypeInvoiceFreeModeMap(expenseDetailSystemFieldSupport.loadExpenseTypeInvoiceFreeModeMap());
        User currentUser = userId == null ? null : userMapper.selectById(userId);
        if (currentUser != null && currentUser.getDeptId() != null) {
            detail.setCurrentUserDeptId(String.valueOf(currentUser.getDeptId()));
            SystemDepartment department = systemDepartmentMapper.selectById(currentUser.getDeptId());
            if (department != null) {
                detail.setCurrentUserDeptName(department.getDeptName());
            }
        }
        return detail;
    }

    List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return financeVendorService.listActiveVendorOptions(requireCurrentUserCompanyId(userId), keyword, includeDisabled);
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
            String counterpartyCode
    ) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedLinkageMode = trimToNull(linkageMode);
        if ("EMPLOYEE".equalsIgnoreCase(normalizedLinkageMode)) {
            return listPersonalPayeeAccountOptions(userId, normalizedKeyword, trimToNull(payeeName));
        }
        if ("ENTERPRISE".equalsIgnoreCase(normalizedLinkageMode)) {
            return listCounterpartyPayeeAccountOptions(userId, normalizedKeyword, trimToNull(counterpartyCode));
        }

        String currentCompanyId = requireCurrentUserCompanyId(userId);
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
                        item.getCVenName(),
                        item.getCVenAbbName(),
                        item.getCVenBank(),
                        item.getCVenAccount(),
                        item.getCVenBankNub()
                ))
                .forEach(item -> {
                    ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
                    option.setValue("VENDOR:" + item.getCVenCode());
                    option.setLabel(buildAccountLabel(item.getCVenName(), item.getCVenBank()));
                    option.setSourceType("VENDOR");
                    option.setOwnerCode(item.getCVenCode());
                    option.setOwnerName(item.getCVenName());
                    option.setBankName(item.getCVenBank());
                    option.setAccountName(item.getCVenName());
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
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
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
                option.setSecondaryLabel("个人中心对私账户");
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
                    option.setSecondaryLabel(trimToNull(account.getBranchName()) != null ? account.getBranchName() : "个人中心对私账户");
                    return option;
                })
                .toList();
    }

    private List<ExpenseCreatePayeeAccountOptionVO> listCounterpartyPayeeAccountOptions(
            Long userId,
            String normalizedKeyword,
            String counterpartyCode
    ) {
        String normalizedVendorCode = trimToNull(counterpartyCode);
        if (normalizedVendorCode == null) {
            return Collections.emptyList();
        }
        String currentCompanyId = requireCurrentUserCompanyId(userId);
        FinanceVendor vendor = financeVendorMapper.selectOne(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .eq(FinanceVendor::getCompanyId, currentCompanyId)
                        .eq(FinanceVendor::getCVenCode, normalizedVendorCode)
                        .last("limit 1")
        );
        if (vendor == null || trimToNull(vendor.getCVenAccount()) == null) {
            return Collections.emptyList();
        }
        if (!matchesKeyword(
                normalizedKeyword,
                vendor.getCVenName(),
                vendor.getCVenAbbName(),
                vendor.getCVenBank(),
                vendor.getCVenAccount(),
                vendor.getCVenBankNub()
        )) {
            return Collections.emptyList();
        }
        ExpenseCreatePayeeAccountOptionVO option = new ExpenseCreatePayeeAccountOptionVO();
        option.setValue("VENDOR:" + vendor.getCVenCode());
        option.setLabel(buildAccountLabel(vendor.getCVenName(), vendor.getCVenBank()));
        option.setSourceType("VENDOR");
        option.setOwnerCode(vendor.getCVenCode());
        option.setOwnerName(vendor.getCVenName());
        option.setBankName(vendor.getCVenBank());
        option.setAccountName(firstNonBlank(vendor.getReceiptAccountName(), vendor.getCVenName()));
        option.setAccountNoMasked(maskAccountNo(vendor.getCVenAccount()));
        option.setSecondaryLabel(buildVendorAccountSecondary(vendor));
        return List.of(option);
    }

    @Transactional(rollbackFor = Exception.class)
    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        String templateCode = dto == null ? null : dto.getTemplateCode();
        String stage = "load-template";
        String documentCode = null;
        int expenseDetailCount = 0;
        log.info("Expense submit stage={} templateCode={} userId={} detailCount={}", stage, templateCode, userId, expenseDetailCount);

        try {
            ProcessDocumentTemplate template = requireTemplate(templateCode);
            stage = "load-form-design";
            ProcessFormDesign formDesign = loadFormDesign(template.getFormDesignCode());
            stage = "load-expense-detail-design";
            ProcessExpenseDetailDesign expenseDetailDesign = loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
            Map<String, Object> formData = dto.getFormData() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.getFormData());
            List<ExpenseDetailInstanceDTO> expenseDetails = normalizeExpenseDetails(dto.getExpenseDetails());
            expenseDetailCount = expenseDetails.size();
            stage = "validate-submit-context";
            String flowSnapshotJson = validateSubmitContext(template, formDesign, expenseDetailDesign, expenseDetails);
            User currentUser = userId == null ? null : userMapper.selectById(userId);
            stage = "build-runtime-context";
            Map<String, Object> runtimeFlowContext = expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                    currentUser,
                    template,
                    formDesign,
                    formData,
                    expenseDetailDesign,
                    expenseDetails
            );
            String submitterDisplayName = resolveUserDisplayName(currentUser, username);

            ProcessDocumentInstance instance = new ProcessDocumentInstance();
            stage = "persist-document";
            log.info("Expense submit stage={} templateCode={} userId={} detailCount={}", stage, template.getTemplateCode(), userId, expenseDetailCount);
            instance.setDocumentCode(buildDocumentCode());
            documentCode = instance.getDocumentCode();
            instance.setTemplateCode(template.getTemplateCode());
            instance.setTemplateName(template.getTemplateName());
            instance.setTemplateType(template.getTemplateType());
            instance.setFormDesignCode(template.getFormDesignCode());
            instance.setApprovalFlowCode(template.getApprovalFlow());
            instance.setFlowName(template.getFlowName());
            instance.setSubmitterUserId(userId);
            instance.setSubmitterName(submitterDisplayName);
            instance.setDocumentTitle(resolveDocumentTitle(template, formData, username));
            instance.setDocumentReason(resolveDocumentReason(template, formData));
            instance.setTotalAmount(resolveTotalAmount(formData));
            instance.setStatus(DOCUMENT_STATUS_PENDING);
            instance.setFormDataJson(writeJson(formData));
            instance.setTemplateSnapshotJson(writeJson(toTemplateSnapshot(template)));
            instance.setFormSchemaSnapshotJson(formDesign == null ? writeJson(defaultSchema()) : formDesign.getSchemaJson());
            instance.setFlowSnapshotJson(flowSnapshotJson);
            instance.setCreatedAt(LocalDateTime.now());
            instance.setUpdatedAt(LocalDateTime.now());
            processDocumentInstanceMapper.insert(instance);

            stage = "append-submit-log";
            appendLog(instance.getDocumentCode(), null, null, LOG_SUBMIT, userId, submitterDisplayName, null, buildSubmitPayload(template));
            stage = "persist-expense-details";
            saveExpenseDetailInstances(instance.getDocumentCode(), template, expenseDetailDesign, expenseDetails);
            stage = "sync-document-relations";
            syncDocumentBusinessRelations(instance.getDocumentCode(), formDesign, formData);
            stage = "initialize-runtime";
            expenseWorkflowRuntimeSupport.initializeRuntime(instance, runtimeFlowContext);
            if (isEffectiveApprovedStatus(requireDocument(instance.getDocumentCode()).getStatus())) {
                finalizeEffectiveWriteOffs(instance.getDocumentCode());
            }

            ExpenseDocumentSubmitResultVO result = new ExpenseDocumentSubmitResultVO();
            result.setId(instance.getId());
            result.setDocumentCode(instance.getDocumentCode());
            result.setStatus(instance.getStatus());
            log.info(
                    "Expense submit stage=success templateCode={} userId={} detailCount={} documentCode={} status={}",
                    template.getTemplateCode(),
                    userId,
                    expenseDetailCount,
                    instance.getDocumentCode(),
                    instance.getStatus()
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "Expense submit failed stage={} templateCode={} userId={} detailCount={} documentCode={} cause={}",
                    stage,
                    templateCode,
                    userId,
                    expenseDetailCount,
                    documentCode,
                    ex.getClass().getSimpleName(),
                    ex
            );
            throw ex;
        }
    }

    List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return instances.isEmpty() ? Collections.emptyList() : toExpenseSummaries(instances);
    }

    List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .ne(ProcessDocumentInstance::getStatus, DOCUMENT_STATUS_DRAFT)
                        .orderByDesc(ProcessDocumentInstance::getCreatedAt, ProcessDocumentInstance::getId)
        );
        return instances.isEmpty() ? Collections.emptyList() : toExpenseSummaries(instances);
    }

    List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        String normalizedKind = normalizeDashboardOutstandingKind(kind);
        String templateType = Objects.equals(normalizedKind, WRITEOFF_SOURCE_LOAN) ? "loan" : "report";
        List<ProcessDocumentInstance> instances = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .eq(ProcessDocumentInstance::getTemplateType, templateType)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        );
        if (instances.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> documentCodes = instances.stream().map(ProcessDocumentInstance::getDocumentCode).toList();
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(documentCodes);
        Map<String, BigDecimal> prepayAmountMap = Objects.equals(normalizedKind, WRITEOFF_SOURCE_PREPAY_REPORT)
                ? loadPrepayReportAmountMap(documentCodes)
                : Collections.emptyMap();

        Map<String, BigDecimal> outstandingAmountMap = new LinkedHashMap<>();
        List<ProcessDocumentInstance> outstandingInstances = instances.stream()
                .filter(instance -> {
                    BigDecimal outstandingAmount = resolveOutstandingAmount(instance, normalizedKind, prepayAmountMap, effectiveAmountMap);
                    if (outstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        return false;
                    }
                    outstandingAmountMap.put(instance.getDocumentCode(), outstandingAmount);
                    return true;
                })
                .toList();
        if (outstandingInstances.isEmpty()) {
            return Collections.emptyList();
        }

        return toExpenseSummaries(outstandingInstances).stream()
                .peek(item -> item.setOutstandingAmount(defaultDecimal(outstandingAmountMap.get(item.getDocumentCode()))))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        ProcessDocumentInstance instance = requireDocument(documentCode);
        if (!allowCrossView && !Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("Current user cannot view this document");
        }
        return buildDocumentDetail(instance);
    }

    ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        ProcessDocumentInstance instance = requireDocument(documentCode);
        if (!allowCrossView && !Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("Current user cannot view this expense detail");
        }
        ProcessDocumentExpenseDetail detail = requireExpenseDetail(documentCode, detailNo);
        return toExpenseDetailDetailVO(detail);
    }

    ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    ) {
        String normalizedRelationType = normalizeRelationType(relationType);
        List<String> normalizedTemplateTypes = normalizePickerTemplateTypes(normalizedRelationType, templateTypes);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        String excludedDocumentCode = trimToNull(excludeDocumentCode);
        String normalizedKeyword = trimToNull(keyword);

        List<ProcessDocumentInstance> visibleApprovedDocuments = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .in(ProcessDocumentInstance::getTemplateType, normalizedTemplateTypes)
                        .ne(excludedDocumentCode != null, ProcessDocumentInstance::getDocumentCode, excludedDocumentCode)
                        .eq(!allowCrossView, ProcessDocumentInstance::getSubmitterUserId, userId)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        ).stream()
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getDocumentCode(),
                        item.getDocumentTitle(),
                        item.getTemplateName(),
                        item.getDocumentReason()
                ))
                .toList();

        ExpenseDocumentPickerVO result = new ExpenseDocumentPickerVO();
        result.setRelationType(normalizedRelationType);
        if (visibleApprovedDocuments.isEmpty()) {
            return result;
        }

        if (Objects.equals(normalizedRelationType, RELATION_TYPE_RELATED)) {
            for (String templateType : normalizedTemplateTypes) {
                result.getGroups().add(buildRelatedGroup(templateType, visibleApprovedDocuments, safePage, safePageSize));
            }
            return result;
        }

        for (String templateType : normalizedTemplateTypes) {
            ExpenseDocumentPickerGroupVO group = buildWriteOffGroup(templateType, visibleApprovedDocuments, safePage, safePageSize);
            if (group.getTotal() > 0) {
                result.getGroups().add(group);
            }
        }
        return result;
    }

    ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    ) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        requireSubmitter(target, userId);
        ensureDashboardWriteOffTargetSupported(target);

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        String normalizedKeyword = trimToNull(keyword);

        List<ProcessDocumentInstance> sourceReports = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .eq(ProcessDocumentInstance::getTemplateType, "report")
                        .ne(ProcessDocumentInstance::getDocumentCode, targetDocumentCode)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        ).stream()
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getDocumentCode(),
                        item.getDocumentTitle(),
                        item.getTemplateName(),
                        item.getDocumentReason()
                ))
                .toList();

        ExpenseDocumentPickerVO result = new ExpenseDocumentPickerVO();
        result.setRelationType(RELATION_TYPE_WRITEOFF);
        if (sourceReports.isEmpty()) {
            return result;
        }

        Map<String, BigDecimal> sourceEffectiveAmountMap = loadEffectiveSourceWriteOffAmountMap(
                sourceReports.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        Set<String> boundSourceCodes = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream()
                .map(ProcessDocumentWriteOff::getSourceDocumentCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ExpenseDocumentPickerItemVO> items = new ArrayList<>();
        for (ProcessDocumentInstance report : sourceReports) {
            if (boundSourceCodes.contains(report.getDocumentCode())) {
                continue;
            }
            BigDecimal availableAmount = resolveReportSourceAvailableAmount(report, sourceEffectiveAmountMap);
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ExpenseDocumentPickerItemVO item = toPickerItem(report);
            item.setAvailableWriteOffAmount(availableAmount);
            items.add(item);
        }
        if (items.isEmpty()) {
            return result;
        }

        result.getGroups().add(paginatePickerGroup("report", items, safePage, safePageSize));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        ProcessDocumentInstance sourceReport = requireDocument(sourceReportDocumentCode);
        requireSubmitter(target, userId);
        requireSubmitter(sourceReport, userId);
        ensureDashboardWriteOffTargetSupported(target);
        ensureApprovedReportSource(sourceReport);
        if (Objects.equals(target.getDocumentCode(), sourceReport.getDocumentCode())) {
            throw new IllegalStateException("鏍搁攢鏉ユ簮鎶ラ攢鍗曚笉鑳戒笌鐩爣鍗曟嵁鐩稿悓");
        }

        long duplicateCount = processDocumentWriteOffMapper.selectCount(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, sourceReportDocumentCode)
                        .eq(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        );
        if (duplicateCount > 0) {
            throw new IllegalStateException("璇ユ姤閿€鍗曚笌鐩爣鍗曟嵁宸插瓨鍦ㄦ湁鏁堟牳閿€鍏崇郴");
        }

        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(targetDocumentCode));
        String targetKind = resolveWriteOffSourceKind(target, prepayAmountMap);
        Map<String, BigDecimal> targetEffectiveAmountMap = loadEffectiveWriteOffAmountMap(List.of(targetDocumentCode));
        BigDecimal targetRemaining = resolveCurrentAvailableWriteOffAmount(target, targetKind, prepayAmountMap, targetEffectiveAmountMap);
        if (targetRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鐩爣鍗曟嵁宸叉棤鍙牳閿€浣欓");
        }

        Map<String, BigDecimal> sourceEffectiveAmountMap = loadEffectiveSourceWriteOffAmountMap(List.of(sourceReportDocumentCode));
        BigDecimal sourceRemaining = resolveReportSourceAvailableAmount(sourceReport, sourceEffectiveAmountMap);
        if (sourceRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鏉ユ簮鎶ラ攢鍗曞凡鏃犲彲鐢ㄦ牳閿€浣欓");
        }

        BigDecimal effectiveAmount = targetRemaining.min(sourceRemaining);
        if (effectiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鏈鏍搁攢閲戦蹇呴』澶т簬 0");
        }

        LocalDateTime now = LocalDateTime.now();
        ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
        writeOff.setSourceDocumentCode(sourceReportDocumentCode);
        writeOff.setSourceFieldKey(DASHBOARD_WRITEOFF_SOURCE_FIELD_KEY);
        writeOff.setTargetDocumentCode(targetDocumentCode);
        writeOff.setTargetTemplateType(target.getTemplateType());
        writeOff.setWriteoffSourceKind(targetKind);
        writeOff.setRequestedAmount(effectiveAmount);
        writeOff.setEffectiveAmount(effectiveAmount);
        writeOff.setAvailableSnapshotAmount(targetRemaining);
        writeOff.setRemainingSnapshotAmount(targetRemaining.subtract(effectiveAmount));
        writeOff.setSortOrder(1);
        writeOff.setStatus(WRITEOFF_STATUS_EFFECTIVE);
        writeOff.setEffectiveAt(now);
        writeOff.setCreatedAt(now);
        writeOff.setUpdatedAt(now);
        processDocumentWriteOffMapper.insert(writeOff);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentInstance instance = requireDocument(documentCode);
        requireSubmitter(instance, userId);
        if (!Objects.equals(trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)) {
            throw new IllegalStateException("褰撳墠鍗曟嵁涓嶆槸鍙噸鎻愯崏绋跨姸鎬?");
        }
        String submitterDisplayName = resolveUserDisplayName(userId, username);
        DocumentMutationContext mutation = buildMutationContext(instance, dto, true);
        instance.setSubmitterName(submitterDisplayName);
        applyDocumentMutation(instance, mutation, true);
        appendLog(instance.getDocumentCode(), null, null, LOG_RESUBMIT, userId, submitterDisplayName, null, Map.of(
                "templateCode", instance.getTemplateCode(),
                "templateName", instance.getTemplateName()
        ));
        syncDocumentBusinessRelations(instance.getDocumentCode(), mutation.formDesign(), mutation.formData());
        expenseWorkflowRuntimeSupport.initializeRuntime(instance, mutation.runtimeContext());
        if (isEffectiveApprovedStatus(requireDocument(instance.getDocumentCode()).getStatus())) {
            finalizeEffectiveWriteOffs(instance.getDocumentCode());
        }
        ExpenseDocumentSubmitResultVO result = new ExpenseDocumentSubmitResultVO();
        result.setId(instance.getId());
        result.setDocumentCode(instance.getDocumentCode());
        result.setStatus(instance.getStatus());
        return result;
    }

    ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        long totalStartedAt = System.nanoTime();
        String documentCode = instance.getDocumentCode();
        String templateType = trimToNull(instance.getTemplateType());
        ExpenseDocumentDetailVO detail = new ExpenseDocumentDetailVO();
        detail.setDocumentCode(instance.getDocumentCode());
        detail.setDocumentTitle(instance.getDocumentTitle());
        detail.setDocumentReason(instance.getDocumentReason());
        detail.setStatus(instance.getStatus());
        detail.setStatusLabel(resolveStatusLabel(instance.getStatus()));
        detail.setTotalAmount(defaultDecimal(instance.getTotalAmount()));
        detail.setSubmitterUserId(instance.getSubmitterUserId());
        detail.setSubmitterName(instance.getSubmitterName());
        detail.setTemplateName(instance.getTemplateName());
        detail.setTemplateType(instance.getTemplateType());
        detail.setCurrentNodeKey(instance.getCurrentNodeKey());
        detail.setCurrentNodeName(instance.getCurrentNodeName());
        detail.setCurrentTaskType(instance.getCurrentTaskType());
        detail.setSubmittedAt(formatTime(instance.getCreatedAt()));
        detail.setFinishedAt(formatTime(instance.getFinishedAt()));
        long snapshotStartedAt = System.nanoTime();
        Map<String, Object> templateSnapshot = readMap(instance.getTemplateSnapshotJson());
        Map<String, Object> formSchemaSnapshot = readMap(instance.getFormSchemaSnapshotJson());
        Map<String, Object> formData = readFormData(instance.getFormDataJson());
        Map<String, Object> flowSnapshot = readMap(instance.getFlowSnapshotJson());
        long snapshotElapsedAt = elapsedMillis(snapshotStartedAt);
        detail.setTemplateSnapshot(templateSnapshot);
        detail.setFormSchemaSnapshot(formSchemaSnapshot);
        detail.setFormData(formData);
        detail.setFlowSnapshot(flowSnapshot);

        long companyOptionsStartedAt = System.nanoTime();
        List<ProcessFormOptionVO> companyOptions = loadCompanyOptionsForDetail(formSchemaSnapshot, formData);
        long companyOptionsElapsedAt = elapsedMillis(companyOptionsStartedAt);
        detail.setCompanyOptions(companyOptions);

        long departmentOptionsStartedAt = System.nanoTime();
        List<ProcessFormOptionVO> departmentOptions = loadDepartmentOptionsForDetail(formSchemaSnapshot, formData);
        long departmentOptionsElapsedAt = elapsedMillis(departmentOptionsStartedAt);
        detail.setDepartmentOptions(departmentOptions);

        long expenseDetailsStartedAt = System.nanoTime();
        List<ExpenseDetailInstanceSummaryVO> expenseDetails = safeLoadExpenseDetailSummaries(documentCode);
        long expenseDetailsElapsedAt = elapsedMillis(expenseDetailsStartedAt);
        detail.setExpenseDetails(expenseDetails);

        long currentTasksStartedAt = System.nanoTime();
        List<ExpenseApprovalTaskVO> currentTasks = loadPendingTasks(documentCode).stream().map(this::toTaskVO).toList();
        long currentTasksElapsedAt = elapsedMillis(currentTasksStartedAt);
        detail.setCurrentTasks(currentTasks);

        long actionLogsStartedAt = System.nanoTime();
        List<ExpenseApprovalLogVO> actionLogs = loadActionLogs(documentCode).stream().map(this::toLogVO).toList();
        long actionLogsElapsedAt = elapsedMillis(actionLogsStartedAt);
        detail.setActionLogs(actionLogs);

        PmBankPaymentRecord bankPaymentRecord = findLatestBankPaymentRecord(documentCode);
        if (bankPaymentRecord != null) {
            Map<Long, String> companyBankAccountNameMap = loadCompanyBankAccountNameMap(
                    bankPaymentRecord.getCompanyBankAccountId() == null
                            ? Collections.emptySet()
                            : Set.of(bankPaymentRecord.getCompanyBankAccountId())
            );
            detail.setBankPayment(toDetailBankPayment(
                    bankPaymentRecord,
                    companyBankAccountNameMap.get(bankPaymentRecord.getCompanyBankAccountId()),
                    instance.getStatus()
            ));
            detail.setBankReceipts(toDetailBankReceipts(bankPaymentRecord));
        }

        log.info(
                "Expense detail built documentCode={} templateType={} totalMs={} snapshotMs={} companyOptionsMs={} departmentOptionsMs={} expenseDetailsMs={} pendingTasksMs={} actionLogsMs={} expenseDetailCount={} pendingTaskCount={} actionLogCount={}",
                documentCode,
                defaultText(templateType, "-"),
                elapsedMillis(totalStartedAt),
                snapshotElapsedAt,
                companyOptionsElapsedAt,
                departmentOptionsElapsedAt,
                expenseDetailsElapsedAt,
                currentTasksElapsedAt,
                actionLogsElapsedAt,
                expenseDetails.size(),
                currentTasks.size(),
                actionLogs.size()
        );
        return detail;
    }

    private ExpenseDocumentPickerGroupVO buildRelatedGroup(
            String templateType,
            List<ProcessDocumentInstance> documents,
            int page,
            int pageSize
    ) {
        List<ExpenseDocumentPickerItemVO> items = documents.stream()
                .filter(item -> Objects.equals(trimToNull(item.getTemplateType()), templateType))
                .map(this::toPickerItem)
                .toList();
        return paginatePickerGroup(templateType, items, page, pageSize);
    }

    private ExpenseDocumentPickerGroupVO buildWriteOffGroup(
            String templateType,
            List<ProcessDocumentInstance> documents,
            int page,
            int pageSize
    ) {
        List<ProcessDocumentInstance> typedDocuments = documents.stream()
                .filter(item -> Objects.equals(trimToNull(item.getTemplateType()), templateType))
                .toList();
        if (typedDocuments.isEmpty()) {
            return paginatePickerGroup(templateType, Collections.emptyList(), page, pageSize);
        }

        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
                typedDocuments.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        List<ExpenseDocumentPickerItemVO> items = new ArrayList<>();
        if (Objects.equals(templateType, "loan")) {
            for (ProcessDocumentInstance instance : typedDocuments) {
                BigDecimal totalAmount = defaultDecimal(instance.getTotalAmount());
                BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
                BigDecimal availableAmount = totalAmount.subtract(effectiveAmount);
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                ExpenseDocumentPickerItemVO item = toPickerItem(instance);
                item.setAvailableWriteOffAmount(availableAmount);
                item.setWriteOffSourceKind(WRITEOFF_SOURCE_LOAN);
                items.add(item);
            }
            return paginatePickerGroup(templateType, items, page, pageSize);
        }

        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
                typedDocuments.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        for (ProcessDocumentInstance instance : typedDocuments) {
            BigDecimal prepayAmount = defaultDecimal(prepayAmountMap.get(instance.getDocumentCode()));
            if (prepayAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
            BigDecimal availableAmount = prepayAmount.subtract(effectiveAmount);
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ExpenseDocumentPickerItemVO item = toPickerItem(instance);
                item.setAvailableWriteOffAmount(availableAmount);
            item.setWriteOffSourceKind(WRITEOFF_SOURCE_PREPAY_REPORT);
            items.add(item);
        }
        return paginatePickerGroup(templateType, items, page, pageSize);
    }

    private ExpenseDocumentPickerGroupVO paginatePickerGroup(
            String templateType,
            List<ExpenseDocumentPickerItemVO> items,
            int page,
            int pageSize
    ) {
        ExpenseDocumentPickerGroupVO group = new ExpenseDocumentPickerGroupVO();
        group.setTemplateType(templateType);
        group.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType, null));
        group.setPage(page);
        group.setPageSize(pageSize);
        group.setTotal(items.size());
        int fromIndex = Math.min(Math.max((page - 1) * pageSize, 0), items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        group.setItems(new ArrayList<>(items.subList(fromIndex, toIndex)));
        return group;
    }

    private ExpenseDocumentPickerItemVO toPickerItem(ProcessDocumentInstance instance) {
        ExpenseDocumentPickerItemVO item = new ExpenseDocumentPickerItemVO();
        item.setDocumentCode(instance.getDocumentCode());
        item.setDocumentTitle(instance.getDocumentTitle());
        item.setTemplateType(instance.getTemplateType());
        item.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), null));
        item.setTemplateName(instance.getTemplateName());
        item.setStatus(instance.getStatus());
        item.setStatusLabel(resolveStatusLabel(instance.getStatus()));
        item.setTotalAmount(defaultDecimal(instance.getTotalAmount()));
        return item;
    }

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
                        detail -> defaultDecimal(detail.getPendingWriteOffAmount()),
                        BigDecimal::add
                )
        ));
    }

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

    private Map<String, BigDecimal> loadEffectiveSourceWriteOffAmountMap(List<String> sourceDocumentCodes) {
        if (sourceDocumentCodes == null || sourceDocumentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .in(ProcessDocumentWriteOff::getSourceDocumentCode, sourceDocumentCodes)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentWriteOff::getSourceDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        item -> defaultDecimal(item.getEffectiveAmount()),
                        BigDecimal::add
                )
        ));
    }

    private String normalizeDashboardOutstandingKind(String kind) {
        String normalizedKind = trimToNull(kind);
        if (Objects.equals(normalizedKind, WRITEOFF_SOURCE_LOAN) || Objects.equals(normalizedKind, WRITEOFF_SOURCE_PREPAY_REPORT)) {
            return normalizedKind;
        }
        throw new IllegalArgumentException("涓嶆敮鎸佺殑寰呭鐞嗗崟鎹被鍨?");
    }

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

    private BigDecimal resolveReportSourceAvailableAmount(
            ProcessDocumentInstance sourceReport,
            Map<String, BigDecimal> sourceEffectiveAmountMap
    ) {
        BigDecimal availableAmount = defaultDecimal(sourceReport.getTotalAmount())
                .subtract(defaultDecimal(sourceEffectiveAmountMap.get(sourceReport.getDocumentCode())));
        return availableAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableAmount;
    }

    private void ensureDashboardWriteOffTargetSupported(ProcessDocumentInstance target) {
        if (!isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鍗曟嵁鏀寔鏍搁攢");
        }
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(target.getDocumentCode()));
        resolveWriteOffSourceKind(target, prepayAmountMap);
    }

    private void ensureApprovedReportSource(ProcessDocumentInstance sourceReport) {
        if (!isEffectiveApprovedStatus(sourceReport.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鎶ラ攢鍗曞彲浣滀负鏍搁攢鏉ユ簮");
        }
        if (!Objects.equals(normalizeTemplateType(sourceReport.getTemplateType()), "report")) {
            throw new IllegalStateException("浠呮姤閿€鍗曞彲浣滀负鏍搁攢鏉ユ簮");
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

    private boolean isTemplateAvailableForCreate(ProcessDocumentTemplate template) {
        if (!Objects.equals(trimToNull(template.getTemplateType()), "report")) {
            return true;
        }
        return trimToNull(template.getExpenseDetailDesignCode()) != null;
    }

    private List<ExpenseDetailInstanceDTO> normalizeExpenseDetails(List<ExpenseDetailInstanceDTO> expenseDetails) {
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

    private void validateExpenseDetailSubmission(
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        String templateType = trimToNull(template.getTemplateType());
        if (!Objects.equals(templateType, "report")) {
            if (!expenseDetails.isEmpty()) {
                throw new IllegalArgumentException("鍙湁鎶ラ攢鍗曟敮鎸佽垂鐢ㄦ槑缁?");
            }
            return;
        }
        if (expenseDetailDesign == null) {
            throw new IllegalStateException("褰撳墠鎶ラ攢妯℃澘鏈粦瀹氳垂鐢ㄦ槑缁嗚〃鍗?");
        }
        if (expenseDetails.isEmpty()) {
            throw new IllegalArgumentException("鎶ラ攢鍗曡嚦灏戦渶瑕?1 浠借垂鐢ㄦ槑缁?");
        }
        if (expenseDetails.size() > 10) {
            throw new IllegalArgumentException("鎶ラ攢鍗曟渶澶氬彧鑳戒繚瀛?10 浠借垂鐢ㄦ槑缁?");
        }
    }

    private String validateSubmitContext(
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        if (trimToNull(template.getFormDesignCode()) == null) {
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘鏈粦瀹氫富琛ㄥ崟锛岃鍏堜慨澶嶆ā鏉块厤缃?");
        }
        if (formDesign == null) {
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘缁戝畾鐨勪富琛ㄥ崟涓嶅瓨鍦紝璇峰厛淇妯℃澘閰嶇疆");
        }
        validateExpenseDetailSubmission(template, expenseDetailDesign, expenseDetails);
        if (Objects.equals(trimToNull(template.getTemplateType()), "report")
                && trimToNull(template.getExpenseDetailDesignCode()) != null
                && expenseDetailDesign == null) {
            throw new IllegalStateException("褰撳墠鎶ラ攢妯℃澘缁戝畾鐨勮垂鐢ㄦ槑缁嗚〃鍗曚笉瀛樺湪锛岃鍏堜慨澶嶆ā鏉块厤缃?");
        }
        return validateFlowSnapshotForSubmit(template);
    }

    private void syncDocumentBusinessRelations(
            String documentCode,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        if (trimToNull(documentCode) == null || formDesign == null) {
            return;
        }
        voidActiveRelations(documentCode);
        voidPendingWriteOffs(documentCode);

        List<DocumentBusinessBinding> bindings = collectDocumentBusinessBindings(formDesign);
        if (bindings.isEmpty()) {
            return;
        }

        List<RelatedDocumentSelection> relatedSelections = new ArrayList<>();
        List<WriteOffSelection> writeOffSelections = new ArrayList<>();
        for (DocumentBusinessBinding binding : bindings) {
            if (Objects.equals(binding.componentCode(), RELATED_DOCUMENT_COMPONENT_CODE)) {
                relatedSelections.addAll(normalizeRelatedDocumentSelections(documentCode, binding, formData));
            } else if (Objects.equals(binding.componentCode(), WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                writeOffSelections.addAll(normalizeWriteOffSelections(documentCode, binding, formData));
            }
        }

        Set<String> targetDocumentCodes = new LinkedHashSet<>();
        relatedSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        writeOffSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        if (targetDocumentCodes.isEmpty()) {
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

        for (RelatedDocumentSelection selection : relatedSelections) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, selection.documentCode(), "鍏宠仈鍗曟嵁");
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException("鍏宠仈鍗曟嵁绫诲瀷涓嶅湪褰撳墠缁勪欢鍏佽鑼冨洿鍐?");
            }
            ProcessDocumentRelation relation = new ProcessDocumentRelation();
            relation.setSourceDocumentCode(documentCode);
            relation.setSourceFieldKey(selection.fieldKey());
            relation.setTargetDocumentCode(selection.documentCode());
            relation.setTargetTemplateType(normalizedTemplateType);
            relation.setSortOrder(selection.sortOrder());
            relation.setStatus(RELATION_STATUS_ACTIVE);
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            processDocumentRelationMapper.insert(relation);
        }

        for (WriteOffSelection selection : writeOffSelections) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, selection.documentCode(), "鏍搁攢鍗曟嵁");
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁绫诲瀷涓嶅湪褰撳墠缁勪欢鍏佽鑼冨洿鍐?");
            }
            String writeOffSourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, writeOffSourceKind, prepayAmountMap, effectiveAmountMap);
            if (selection.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("鏍搁攢閲戦蹇呴』澶т簬 0");
            }
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("褰撳墠鏍搁攢鍗曟嵁宸叉棤鍙牳閿€浣欓");
            }
            if (selection.requestedAmount().compareTo(availableAmount) > 0) {
                throw new IllegalStateException("鏍搁攢閲戦涓嶈兘瓒呰繃褰撳墠鍙牳閿€浣欓");
            }
            ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
            writeOff.setSourceDocumentCode(documentCode);
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
        }
    }

    private void finalizeEffectiveWriteOffs(String documentCode) {
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
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, writeOff.getTargetDocumentCode(), "鏍搁攢鍗曟嵁");
            String sourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, sourceKind, prepayAmountMap, effectiveAmountMap);
            BigDecimal requestedAmount = defaultDecimal(writeOff.getRequestedAmount());
            if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("鏍搁攢閲戦蹇呴』澶т簬 0");
            }
            if (requestedAmount.compareTo(availableAmount) > 0) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁 " + writeOff.getTargetDocumentCode() + " 鐨勫彲鏍搁攢浣欓涓嶈冻锛岃鍒锋柊鍚庨噸璇?");
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
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鍏宠仈鑷繁");
            }
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
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鏍搁攢鑷繁");
            }
            BigDecimal requestedAmount = toBigDecimal(record.get("writeOffAmount"));
            if (requestedAmount == null) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁缂哄皯鏍搁攢閲戦");
            }
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

    private ProcessDocumentInstance requireApprovedTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            String actionName
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null || !isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException(actionName + "鐩爣涓嶅瓨鍦ㄦ垨鏈€氳繃瀹℃壒");
        }
        return target;
    }

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
        throw new IllegalStateException("褰撳墠鍗曟嵁涓嶆敮鎸佷綔涓烘牳閿€鐩爣");
    }

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
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘缁戝畾鐨勬祦绋嬩笉瀛樺湪锛岃鍏堜慨澶嶆ā鏉块厤缃?");
        }
        Long versionId = flow.getCurrentPublishedVersionId() != null
                ? flow.getCurrentPublishedVersionId()
                : flow.getCurrentDraftVersionId();
        if (versionId == null) {
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘缁戝畾鐨勬祦绋嬪皻鏈厤缃彲鐢ㄧ増鏈紝璇峰厛鍙戝竷娴佺▼");
        }
        ProcessFlowVersion version = processFlowVersionMapper.selectById(versionId);
        String snapshotJson = version == null ? null : trimToNull(version.getSnapshotJson());
        if (snapshotJson == null) {
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘缁戝畾鐨勬祦绋嬪揩鐓т笉瀛樺湪锛岃鍏堥噸鏂板彂甯冩祦绋?");
        }
        try {
            expenseWorkflowRuntimeSupport.validateFlowSnapshot(snapshotJson);
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("褰撳墠瀹℃壒妯℃澘缁戝畾鐨勬祦绋嬪揩鐓ф崯鍧忥紝璇峰厛閲嶆柊鍙戝竷娴佺▼", ex);
        }
        return snapshotJson;
    }

    private Map<String, Object> buildSubmitPayload(ProcessDocumentTemplate template) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateCode", template.getTemplateCode());
        payload.put("templateName", defaultText(template.getTemplateName(), template.getTemplateCode()));
        return payload;
    }

    private void saveExpenseDetailInstances(
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
                detail.setInvoiceAmount(readInvoiceAmountForStorage(detailType, businessSceneMode, detailFormData));
                detail.setActualPaymentAmount(toBigDecimal(detailFormData.get(FIELD_ACTUAL_PAYMENT_AMOUNT)));
                detail.setPendingWriteOffAmount(readPendingWriteOffAmountForStorage(detailType, businessSceneMode, detailFormData));
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

    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        );
    }

    private List<ExpenseDetailInstanceSummaryVO> safeLoadExpenseDetailSummaries(String documentCode) {
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

    private ExpenseDetailInstanceDetailVO toExpenseDetailDetailVO(ProcessDocumentExpenseDetail detail) {
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
        vo.setSchemaSnapshot(readMap(detail.getSchemaSnapshotJson()));
        vo.setFormData(readMap(detail.getFormDataJson()));
        vo.setCreatedAt(formatTime(detail.getCreatedAt()));
        vo.setUpdatedAt(formatTime(detail.getUpdatedAt()));
        return vo;
    }

    private ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        ProcessDocumentExpenseDetail detail = processDocumentExpenseDetailMapper.selectOne(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, trimToNull(documentCode))
                        .eq(ProcessDocumentExpenseDetail::getDetailNo, trimToNull(detailNo))
                        .last("limit 1")
        );
        if (detail == null) {
            throw new IllegalStateException("璐圭敤鏄庣粏涓嶅瓨鍦?");
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

    private ExpenseDocumentEditContextVO buildEditContext(Long userId, ProcessDocumentInstance instance, Long taskId, String editMode) {
        ProcessDocumentTemplate template = requireTemplateForDocument(instance.getTemplateCode());
        ExpenseCreateTemplateDetailVO templateDetail = buildTemplateDetail(userId, template);
        ExpenseDocumentEditContextVO context = new ExpenseDocumentEditContextVO();
        context.setEditMode(editMode);
        context.setDocumentCode(instance.getDocumentCode());
        context.setTaskId(taskId);
        copyTemplateDetail(templateDetail, context);
        context.setFormData(readFormData(instance.getFormDataJson()));
        context.setExpenseDetails(loadExpenseDetails(instance.getDocumentCode()).stream().map(this::toRuntimeExpenseDetailDTO).toList());
        return context;
    }

    private void copyTemplateDetail(ExpenseCreateTemplateDetailVO source, ExpenseDocumentEditContextVO target) {
        target.setTemplateCode(source.getTemplateCode());
        target.setTemplateName(source.getTemplateName());
        target.setTemplateType(source.getTemplateType());
        target.setTemplateTypeLabel(source.getTemplateTypeLabel());
        target.setCategoryCode(source.getCategoryCode());
        target.setTemplateDescription(source.getTemplateDescription());
        target.setFormDesignCode(source.getFormDesignCode());
        target.setApprovalFlowCode(source.getApprovalFlowCode());
        target.setFlowName(source.getFlowName());
        target.setFormName(source.getFormName());
        target.setSchema(source.getSchema());
        target.setExpenseDetailDesignCode(source.getExpenseDetailDesignCode());
        target.setExpenseDetailDesignName(source.getExpenseDetailDesignName());
        target.setExpenseDetailType(source.getExpenseDetailType());
        target.setExpenseDetailTypeLabel(source.getExpenseDetailTypeLabel());
        target.setExpenseDetailModeDefault(source.getExpenseDetailModeDefault());
        target.setExpenseDetailSchema(source.getExpenseDetailSchema());
        target.setSharedArchives(source.getSharedArchives());
        target.setExpenseDetailSharedArchives(source.getExpenseDetailSharedArchives());
        target.setCompanyOptions(source.getCompanyOptions());
        target.setDepartmentOptions(source.getDepartmentOptions());
        target.setExpenseTypeOptions(source.getExpenseTypeOptions());
        target.setExpenseTypeInvoiceFreeModeMap(source.getExpenseTypeInvoiceFreeModeMap());
        target.setCurrentUserDeptId(source.getCurrentUserDeptId());
        target.setCurrentUserDeptName(source.getCurrentUserDeptName());
    }

    DocumentMutationContext buildMutationContext(ProcessDocumentInstance instance, ExpenseDocumentUpdateDTO dto, boolean resetRuntime) {
        ProcessDocumentTemplate template = requireTemplateForDocument(instance.getTemplateCode());
        ProcessFormDesign formDesign = loadFormDesign(template.getFormDesignCode());
        ProcessExpenseDetailDesign expenseDetailDesign = loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
        Map<String, Object> formData = dto == null || dto.getFormData() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(dto.getFormData());
        List<ExpenseDetailInstanceDTO> expenseDetails = normalizeExpenseDetails(dto == null ? Collections.emptyList() : dto.getExpenseDetails());
        validateExpenseDetailSubmission(template, expenseDetailDesign, expenseDetails);
        User submitter = loadActiveUser(instance.getSubmitterUserId());
        Map<String, Object> runtimeContext = resetRuntime
                ? expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                        submitter,
                        template,
                        formDesign,
                        formData,
                        expenseDetailDesign,
                        expenseDetails
                )
                : Collections.emptyMap();
        return new DocumentMutationContext(
                template,
                formDesign,
                expenseDetailDesign,
                formData,
                expenseDetails,
                runtimeContext,
                resolveDocumentTitle(template, formData, instance.getSubmitterName()),
                resolveDocumentReason(template, formData),
                resolveTotalAmount(formData)
        );
    }

    void applyDocumentMutation(ProcessDocumentInstance instance, DocumentMutationContext context, boolean resetRuntime) {
        LocalDateTime now = LocalDateTime.now();
        if (resetRuntime) {
            cancelOpenTasks(loadOpenTasks(instance.getDocumentCode()), null, now);
            instance.setStatus(DOCUMENT_STATUS_DRAFT);
            instance.setCurrentNodeKey(null);
            instance.setCurrentNodeName(null);
            instance.setCurrentTaskType(null);
            instance.setFinishedAt(null);
            instance.setTemplateName(context.template().getTemplateName());
            instance.setTemplateType(context.template().getTemplateType());
            instance.setFormDesignCode(context.template().getFormDesignCode());
            instance.setApprovalFlowCode(context.template().getApprovalFlow());
            instance.setFlowName(context.template().getFlowName());
            instance.setTemplateSnapshotJson(writeJson(toTemplateSnapshot(context.template())));
            instance.setFormSchemaSnapshotJson(context.formDesign() == null ? writeJson(defaultSchema()) : context.formDesign().getSchemaJson());
            instance.setFlowSnapshotJson(resolveFlowSnapshotJson(context.template()));
        }
        instance.setDocumentTitle(context.documentTitle());
        instance.setDocumentReason(context.documentReason());
        instance.setTotalAmount(context.totalAmount());
        instance.setFormDataJson(writeJson(context.formData()));
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        replaceExpenseDetailInstances(instance.getDocumentCode(), context.template(), context.expenseDetailDesign(), context.expenseDetails());
    }

    private void replaceExpenseDetailInstances(
            String documentCode,
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        processDocumentExpenseDetailMapper.delete(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
        );
        saveExpenseDetailInstances(documentCode, template, expenseDetailDesign, expenseDetails);
    }

    private ExpenseCreateTemplateSummaryVO toTemplateSummary(ProcessDocumentTemplate template) {
        ExpenseCreateTemplateSummaryVO summary = new ExpenseCreateTemplateSummaryVO();
        summary.setTemplateCode(template.getTemplateCode());
        summary.setTemplateName(template.getTemplateName());
        summary.setTemplateType(template.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        summary.setCategoryCode(template.getCategoryCode());
        summary.setFormDesignCode(template.getFormDesignCode());
        return summary;
    }

    private List<ExpenseSummaryVO> toExpenseSummaries(List<ProcessDocumentInstance> instances) {
        SummaryEnrichmentData enrichmentData = buildSummaryEnrichment(instances);
        return instances.stream().map(instance -> toExpenseSummary(instance, enrichmentData)).toList();
    }

    private ExpenseSummaryVO toExpenseSummary(ProcessDocumentInstance instance, SummaryEnrichmentData enrichmentData) {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        SummaryMetadata metadata = enrichmentData.metadata(instance.getDocumentCode());
        String statusLabel = resolveStatusLabel(instance.getStatus());
        summary.setDocumentCode(instance.getDocumentCode());
        summary.setNo(instance.getDocumentCode());
        summary.setType(trimToNull(instance.getTemplateName()) != null ? instance.getTemplateName() : resolveTemplateTypeLabel(instance.getTemplateType(), null));
        summary.setReason(trimToNull(instance.getDocumentReason()) != null ? instance.getDocumentReason() : defaultReason(instance.getDocumentTitle()));
        summary.setDocumentTitle(instance.getDocumentTitle());
        summary.setDocumentReason(instance.getDocumentReason());
        summary.setSubmitterName(instance.getSubmitterName());
        summary.setSubmitterDeptName(metadata.submitterDeptName());
        summary.setTemplateName(instance.getTemplateName());
        summary.setTemplateType(instance.getTemplateType());
        summary.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel") == null
                ? null
                : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel"))));
        summary.setCurrentNodeName(instance.getCurrentNodeName());
        summary.setDocumentStatus(instance.getStatus());
        summary.setDocumentStatusLabel(statusLabel);
        summary.setAmount(defaultDecimal(instance.getTotalAmount()));
        summary.setDate(instance.getCreatedAt() == null ? "" : instance.getCreatedAt().format(DATE_FORMATTER));
        summary.setStatus(statusLabel);
        summary.setSubmittedAt(formatTime(instance.getCreatedAt()));
        summary.setPaymentDate(metadata.paymentDate());
        summary.setPaymentCompanyName(metadata.paymentCompanyName());
        summary.setPayeeName(metadata.payeeName());
        summary.setCounterpartyName(metadata.counterpartyName());
        summary.setUndertakeDepartmentNames(metadata.undertakeDepartmentNames());
        summary.setTagNames(metadata.tagNames());
        return summary;
    }

    private ExpenseApprovalPendingItemVO toPendingItem(
            ProcessDocumentTask task,
            ProcessDocumentInstance instance,
            SummaryEnrichmentData enrichmentData
    ) {
        ExpenseApprovalPendingItemVO item = new ExpenseApprovalPendingItemVO();
        SummaryMetadata metadata = instance == null ? SummaryMetadata.empty() : enrichmentData.metadata(task.getDocumentCode());
        item.setTaskId(task.getId());
        item.setDocumentCode(task.getDocumentCode());
        item.setDocumentTitle(instance == null ? "" : instance.getDocumentTitle());
        item.setDocumentReason(instance == null ? "" : instance.getDocumentReason());
        item.setTemplateName(instance == null ? "" : instance.getTemplateName());
        item.setTemplateType(instance == null ? null : instance.getTemplateType());
        item.setTemplateTypeLabel(instance == null ? null : resolveTemplateTypeLabel(instance.getTemplateType(), readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel") == null
                ? null
                : String.valueOf(readMap(instance.getTemplateSnapshotJson()).get("templateTypeLabel"))));
        item.setSubmitterName(instance == null ? "" : instance.getSubmitterName());
        item.setSubmitterDeptName(metadata.submitterDeptName());
        item.setAmount(instance == null ? BigDecimal.ZERO : defaultDecimal(instance.getTotalAmount()));
        item.setNodeKey(task.getNodeKey());
        item.setNodeName(task.getNodeName());
        item.setStatus(task.getStatus());
        item.setDocumentStatus(instance == null ? null : instance.getStatus());
        item.setDocumentStatusLabel(instance == null ? null : resolveStatusLabel(instance.getStatus()));
        item.setSubmittedAt(instance == null ? null : formatTime(instance.getCreatedAt()));
        item.setPaymentDate(metadata.paymentDate());
        item.setPaymentCompanyName(metadata.paymentCompanyName());
        item.setPayeeName(metadata.payeeName());
        item.setCounterpartyName(metadata.counterpartyName());
        item.setUndertakeDepartmentNames(metadata.undertakeDepartmentNames());
        item.setTagNames(metadata.tagNames());
        item.setTaskCreatedAt(formatTime(task.getCreatedAt()));
        return item;
    }

    private SummaryEnrichmentData buildSummaryEnrichment(List<ProcessDocumentInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return SummaryEnrichmentData.empty();
        }

        List<String> documentCodes = instances.stream()
                .map(ProcessDocumentInstance::getDocumentCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, List<ProcessDocumentExpenseDetail>> expenseDetailMap = documentCodes.isEmpty()
                ? Collections.emptyMap()
                : processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.toList()
        ));

        List<String> templateCodes = instances.stream()
                .map(ProcessDocumentInstance::getTemplateCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, ProcessDocumentTemplate> templateMap = templateCodes.isEmpty()
                ? Collections.emptyMap()
                : templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .in(ProcessDocumentTemplate::getTemplateCode, templateCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentTemplate::getTemplateCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, String> tagArchiveCodeByTemplateCode = loadTagArchiveCodeByTemplateCode(templateMap);

        Map<String, SummaryDraft> draftMap = new LinkedHashMap<>();
        Set<Long> userIds = new LinkedHashSet<>();
        Set<String> companyIds = new LinkedHashSet<>();
        Set<String> vendorCodes = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        Set<String> archiveCodes = new LinkedHashSet<>();

        for (ProcessDocumentInstance instance : instances) {
            Map<String, Object> formData = readMap(instance.getFormDataJson());
            Map<String, Object> schema = readSchema(instance.getFormSchemaSnapshotJson());
            String documentCode = instance.getDocumentCode();
            List<ProcessDocumentExpenseDetail> expenseDetails = expenseDetailMap.getOrDefault(documentCode, Collections.emptyList());
            String tagArchiveCode = tagArchiveCodeByTemplateCode.get(instance.getTemplateCode());
            SummaryDraft draft = new SummaryDraft();
            draft.setDocumentCode(documentCode);
            draft.setPaymentCompanyId(extractFirstBusinessComponentValue(schema, formData, PAYMENT_COMPANY_COMPONENT_CODE));
            draft.setPayeeValue(extractFirstBusinessComponentValue(schema, formData, PAYEE_COMPONENT_CODE));
            draft.setCounterpartyValue(extractFirstBusinessComponentValue(schema, formData, COUNTERPARTY_COMPONENT_CODE));
            draft.setPaymentDate(extractPaymentDate(schema, formData));
            draft.setUndertakeDepartmentIds(resolveUndertakeDeptIdsFromSnapshots(schema, formData, expenseDetails));
            draft.setTagArchiveCode(tagArchiveCode);
            draft.setTagValues(tagArchiveCode == null ? Collections.emptyList() : extractArchiveValues(schema, formData, tagArchiveCode));
            draftMap.put(documentCode, draft);

            if (instance.getSubmitterUserId() != null) {
                userIds.add(instance.getSubmitterUserId());
            }
            if (draft.getPaymentCompanyId() != null) {
                companyIds.add(draft.getPaymentCompanyId());
            }
            collectPartyLookupIds(draft.getPayeeValue(), userIds, vendorCodes);
            collectVendorCode(draft.getCounterpartyValue(), vendorCodes);
            departmentIds.addAll(draft.getUndertakeDepartmentIds());
            if (tagArchiveCode != null) {
                archiveCodes.add(tagArchiveCode);
            }
        }

        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .in(User::getId, userIds)
        ).stream().collect(Collectors.toMap(
                User::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        userMap.values().stream()
                .map(User::getDeptId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .forEach(departmentIds::add);

        Map<String, SystemCompany> companyMap = companyIds.isEmpty()
                ? Collections.emptyMap()
                : systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, FinanceVendor> vendorMap = vendorCodes.isEmpty()
                ? Collections.emptyMap()
                : financeVendorMapper.selectList(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .in(FinanceVendor::getCVenCode, vendorCodes)
        ).stream().collect(Collectors.toMap(
                FinanceVendor::getCVenCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<Long> departmentIdValues = departmentIds.stream().map(this::toLong).filter(Objects::nonNull).toList();
        Map<String, String> departmentNameMap = departmentIdValues.isEmpty()
                ? Collections.emptyMap()
                : systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .in(SystemDepartment::getId, departmentIdValues)
        ).stream().collect(Collectors.toMap(
                item -> String.valueOf(item.getId()),
                SystemDepartment::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, Map<String, String>> archiveItemLabelMap = loadArchiveItemLabelMap(archiveCodes);

        Map<String, SummaryMetadata> metadataMap = new LinkedHashMap<>();
        for (ProcessDocumentInstance instance : instances) {
            SummaryDraft draft = draftMap.get(instance.getDocumentCode());
            User submitter = instance.getSubmitterUserId() == null ? null : userMap.get(instance.getSubmitterUserId());
            SummaryMetadata metadata = new SummaryMetadata(
                    submitter == null || submitter.getDeptId() == null ? null : departmentNameMap.get(String.valueOf(submitter.getDeptId())),
                    draft == null ? null : draft.getPaymentCompanyId(),
                    draft == null ? null : resolvePaymentCompanyName(draft.getPaymentCompanyId(), companyMap),
                    draft == null ? null : resolvePartyName(draft.getPayeeValue(), userMap, vendorMap),
                    draft == null ? null : resolveVendorName(draft.getCounterpartyValue(), vendorMap),
                    draft == null ? null : draft.getPaymentDate(),
                    draft == null ? Collections.emptyList() : resolveDepartmentNames(draft.getUndertakeDepartmentIds(), departmentNameMap),
                    draft == null ? Collections.emptyList() : resolveArchiveItemNames(draft.getTagArchiveCode(), draft.getTagValues(), archiveItemLabelMap)
            );
            metadataMap.put(instance.getDocumentCode(), metadata);
        }
        return new SummaryEnrichmentData(metadataMap);
    }

    private Map<String, String> loadTagArchiveCodeByTemplateCode(Map<String, ProcessDocumentTemplate> templateMap) {
        if (templateMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> templateIds = templateMap.values().stream()
                .map(ProcessDocumentTemplate::getId)
                .filter(Objects::nonNull)
                .toList();
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> templateCodeById = templateMap.values().stream()
                .filter(item -> item.getId() != null && trimToNull(item.getTemplateCode()) != null)
                .collect(Collectors.toMap(
                        ProcessDocumentTemplate::getId,
                        ProcessDocumentTemplate::getTemplateCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return processTemplateScopeMapper.selectList(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .in(ProcessTemplateScope::getTemplateId, templateIds)
                        .eq(ProcessTemplateScope::getOptionType, TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE)
                        .orderByAsc(ProcessTemplateScope::getSortOrder, ProcessTemplateScope::getId)
        ).stream()
                .filter(item -> trimToNull(templateCodeById.get(item.getTemplateId())) != null)
                .collect(Collectors.toMap(
                        item -> templateCodeById.get(item.getTemplateId()),
                        ProcessTemplateScope::getOptionCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Map<String, String>> loadArchiveItemLabelMap(Set<String> archiveCodes) {
        if (archiveCodes == null || archiveCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .in(ProcessCustomArchiveDesign::getArchiveCode, archiveCodes)
        );
        if (archives.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> archiveCodeById = archives.stream().collect(Collectors.toMap(
                ProcessCustomArchiveDesign::getId,
                ProcessCustomArchiveDesign::getArchiveCode,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archiveCodeById.keySet())
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        );
        Map<String, Map<String, String>> labelMap = new LinkedHashMap<>();
        for (ProcessCustomArchiveItem item : items) {
            String archiveCode = archiveCodeById.get(item.getArchiveId());
            if (archiveCode == null) {
                continue;
            }
            labelMap.computeIfAbsent(archiveCode, ignored -> new LinkedHashMap<>())
                    .put(trimToNull(item.getItemCode()) == null ? item.getItemName() : item.getItemCode(), item.getItemName());
        }
        return labelMap;
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

    private String resolvePaymentCompanyName(String companyId, Map<String, SystemCompany> companyMap) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            return null;
        }
        SystemCompany company = companyMap.get(normalized);
        return company == null ? normalized : firstNonBlank(company.getCompanyName(), company.getCompanyCode(), normalized);
    }

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

    private ExpenseApprovalTaskVO toTaskVO(ProcessDocumentTask task) {
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

    private ExpenseApprovalLogVO toLogVO(ProcessDocumentActionLog log) {
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
        instance.setStatus(defaultText(trimToNull(terminalStatus), DOCUMENT_STATUS_APPROVED));
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setFinishedAt(now);
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
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

    private void clearCurrentNode(ProcessDocumentInstance instance) {
        instance.setCurrentNodeKey(null);
        instance.setCurrentNodeName(null);
        instance.setCurrentTaskType(null);
        instance.setUpdatedAt(LocalDateTime.now());
        processDocumentInstanceMapper.updateById(instance);
    }

    private void persistDocumentRuntimeState(
            ProcessDocumentInstance instance,
            String status,
            String currentNodeKey,
            String currentNodeName,
            String currentTaskType,
            LocalDateTime finishedAt,
            LocalDateTime updatedAt
    ) {
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
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setDocumentCode(documentCode);
        log.setNodeKey(nodeKey);
        log.setNodeName(nodeName);
        log.setActionType(actionType);
        log.setActorUserId(actorUserId);
        log.setActorName(actorName);
        log.setActionComment(trimToNull(actionComment));
        log.setPayloadJson(payload == null || payload.isEmpty() ? null : writeJson(payload));
        log.setCreatedAt(LocalDateTime.now());
        processDocumentActionLogMapper.insert(log);
    }

    void assertCanViewDocument(ProcessDocumentInstance instance, Long userId, boolean allowCrossView) {
        if (!allowCrossView && !Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("Current user cannot view this document");
        }
    }

    void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        if (!Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("鍙湁鎻愬崟浜哄彲浠ユ墽琛屽綋鍓嶆搷浣?");
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

    private ProcessDocumentTemplate requireTemplateForDocument(String templateCode) {
        ProcessDocumentTemplate template = loadTemplateByCode(templateCode, false);
        if (template == null) {
            throw new IllegalStateException("褰撳墠鍗曟嵁缁戝畾鐨勬ā鏉夸笉瀛樺湪锛屾棤娉曠户缁鐞?");
        }
        return template;
    }

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

    private ProcessDocumentTemplate requireTemplate(String templateCode) {
        if (trimToNull(templateCode) == null) {
            throw new IllegalArgumentException("Template code is required");
        }
        ProcessDocumentTemplate template = loadTemplateByCode(templateCode, true);
        if (template == null) {
            throw new IllegalStateException("Available template not found");
        }
        return template;
    }

    ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Document code is required");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("Document not found");
        }
        return instance;
    }

    private ProcessDocumentTask requireOpenPaymentTask(Long taskId, Long userId) {
        ProcessDocumentTask task = processDocumentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalStateException("Payment task not found");
        }
        if (!Objects.equals(task.getAssigneeUserId(), userId)) {
            throw new IllegalStateException("Current user cannot handle this payment task");
        }
        if (!NODE_TYPE_PAYMENT.equals(trimToNull(task.getNodeType()))) {
            throw new IllegalStateException("Current task is not a payment task");
        }
        if (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus())) {
            throw new IllegalStateException("Payment task has already been handled");
        }
        return task;
    }

    private ProcessFormDesign loadFormDesign(String formDesignCode) {
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

    private ProcessExpenseDetailDesign loadExpenseDetailDesign(String detailDesignCode) {
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
        BigDecimal amount = resolveTotalAmount(formData);
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

    private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

    private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

    private List<ProcessCustomArchiveDetailVO> loadSharedArchives(Map<String, Object> schema) {
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
            detail.setArchiveTypeLabel("AUTO_RULE".equals(archive.getArchiveType()) ? "鑷姩鍒掑垎" : "鎻愪緵閫夋嫨");
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

    private Map<String, Object> toTemplateSnapshot(ProcessDocumentTemplate template) {
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

    private String resolveFlowSnapshotJson(ProcessDocumentTemplate template) {
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

    private List<ProcessFormOptionVO> loadDepartmentOptions() {
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

    private List<ProcessFormOptionVO> loadDepartmentOptionsForDetail(Map<String, Object> schema, Map<String, Object> formData) {
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

    private List<ProcessFormOptionVO> loadCompanyOptions() {
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

    private List<ProcessFormOptionVO> loadCompanyOptionsForDetail(Map<String, Object> schema, Map<String, Object> formData) {
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

    private List<ProcessDocumentTask> loadPendingTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getStatus, TASK_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private List<ProcessDocumentTask> loadOpenTasks(String documentCode) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private List<ProcessDocumentTask> loadNodeOpenTasks(String documentCode, String nodeKey) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .in(ProcessDocumentTask::getStatus, List.of(TASK_STATUS_PENDING, TASK_STATUS_PAUSED))
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private List<ProcessDocumentTask> loadNodeBatchTasks(String documentCode, String nodeKey, String batchNo) {
        return processDocumentTaskMapper.selectList(
                Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                        .eq(ProcessDocumentTask::getNodeKey, nodeKey)
                        .eq(ProcessDocumentTask::getTaskBatchNo, batchNo)
                        .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
        );
    }

    private void cancelOpenTasks(List<ProcessDocumentTask> tasks, Long keepTaskId, LocalDateTime handledAt) {
        for (ProcessDocumentTask task : tasks) {
            if (Objects.equals(task.getId(), keepTaskId)
                    || (!TASK_STATUS_PENDING.equals(task.getStatus()) && !TASK_STATUS_PAUSED.equals(task.getStatus()))) {
                continue;
            }
            task.setStatus(TASK_STATUS_CANCELLED);
            task.setHandledAt(handledAt);
            processDocumentTaskMapper.updateById(task);
        }
    }

    private List<ProcessDocumentActionLog> loadActionLogs(String documentCode) {
        return processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
        );
    }

    private String resolveDocumentTitle(ProcessDocumentTemplate template, Map<String, Object> formData, String username) {
        String title = firstNonBlank(
                stringValue(formData.get("__documentTitle")),
                stringValue(formData.get("documentTitle")),
                stringValue(formData.get("title"))
        );
        if (title != null) {
            return title;
        }
        return template.getTemplateName() + "-" + defaultUsername(username) + "-" + LocalDate.now().format(DATE_FORMATTER);
    }

    private String resolveDocumentReason(ProcessDocumentTemplate template, Map<String, Object> formData) {
        String reason = firstNonBlank(
                stringValue(formData.get("__documentReason")),
                stringValue(formData.get("documentReason")),
                stringValue(formData.get("reason")),
                stringValue(formData.get("summary")),
                stringValue(formData.get("bankPushSummary"))
        );
        return reason == null ? defaultReason(template.getTemplateName()) : reason;
    }

    private BigDecimal resolveTotalAmount(Map<String, Object> formData) {
        BigDecimal directAmount = toBigDecimal(formData.get("__totalAmount"));
        if (directAmount != null) {
            return directAmount;
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            if (key.contains("amount") || key.contains("money") || key.contains("閲戦")) {
                BigDecimal amount = toBigDecimal(entry.getValue());
                if (amount != null) {
                    return amount;
                }
            }
        }
        return null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize data", ex);
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

    private Map<String, Object> readMap(String json) {
        if (trimToNull(json) == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse json map", ex);
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

    private String buildDocumentCode() {
        String prefix = "DOC" + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = processDocumentInstanceMapper.selectCount(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .likeRight(ProcessDocumentInstance::getDocumentCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String buildReceiptContent(ProcessDocumentInstance instance, PmBankPaymentRecord record, SystemCompanyBankAccount account) {
        List<String> lines = new ArrayList<>();
        lines.add("招商银行云直连回单");
        lines.add("单据编号: " + defaultText(instance.getDocumentCode(), "-"));
        lines.add("单据名称: " + defaultText(instance.getDocumentTitle(), "-"));
        lines.add("付款账号: " + defaultText(buildCompanyBankAccountName(account), "-"));
        lines.add("银行订单号: " + defaultText(trimToNull(record.getBankOrderNo()), "-"));
        lines.add("银行流水号: " + defaultText(trimToNull(record.getBankFlowNo()), "-"));
        lines.add("支付时间: " + defaultText(formatTime(record.getPaidAt()), "-"));
        lines.add("回单生成时间: " + formatTime(LocalDateTime.now()));
        return String.join(System.lineSeparator(), lines);
    }

    private String buildReceiptFileName(String documentCode) {
        return defaultText(documentCode, "document") + "-银行回单.txt";
    }

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

    private Map<Long, String> loadCompanyBankAccountNameMap(Set<Long> companyBankAccountIds) {
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

    private ExpenseDocumentBankPaymentVO toDetailBankPayment(
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

    private List<ExpenseDocumentBankReceiptVO> toDetailBankReceipts(PmBankPaymentRecord record) {
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

    private PmBankPaymentRecord findLatestBankPaymentRecord(String documentCode) {
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
            throw new IllegalArgumentException("公司账户不存在");
        }
        return account;
    }

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

    private String resolveBankLinkStatusLabel(SystemCompanyBankAccount account) {
        if (!isFlagEnabled(account.getDirectConnectEnabled())) {
            return "未启用";
        }
        if (!BANK_PROVIDER_CMB.equals(trimToNull(account.getDirectConnectProvider()))
                || !BANK_CHANNEL_CMB_CLOUD.equals(trimToNull(account.getDirectConnectChannel()))) {
            return "未配置";
        }
        return "已启用";
    }

    private String resolveBankLinkSyncStatus(SystemCompanyBankAccount account) {
        String status = trimToNull(account.getDirectConnectLastSyncStatus());
        return status == null ? "未推送" : status;
    }

    private String resolveReceiptStatusLabel(PmBankPaymentRecord record) {
        if (record == null) {
            return "未生成";
        }
        if (isFlagEnabled(record.getManualPaid()) && trimToNull(record.getReceiptAttachmentId()) == null) {
            return "手动已支付";
        }
        return switch (defaultText(trimToNull(record.getReceiptStatus()), RECEIPT_STATUS_PENDING)) {
            case RECEIPT_STATUS_RECEIVED -> "已获取回单";
            case RECEIPT_STATUS_FAILED -> "回单查询失败";
            default -> "待查询回单";
        };
    }

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

    private String buildCompanyBankAccountName(SystemCompanyBankAccount account) {
        if (account == null) {
            return null;
        }
        String tailNo = trimToNull(account.getAccountNo());
        String suffix = tailNo == null || tailNo.length() <= 4 ? tailNo : tailNo.substring(tailNo.length() - 4);
        return account.getAccountName() + (suffix == null ? "" : "（尾号" + suffix + "）");
    }

    private String buildBankPushRequestNo(String documentCode) {
        return defaultText(documentCode, "DOC") + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

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
                // try next formatter
            }
        }
        return defaultValue;
    }

    private String buildExpenseDetailNo(String documentCode, int sortOrder) {
        return documentCode + "-D" + String.format("%02d", sortOrder);
    }

    private String buildTaskBatchNo(String documentCode, String nodeKey) {
        return documentCode + "-" + nodeKey + "-" + System.currentTimeMillis();
    }

    private String resolveTemplateTypeLabel(String templateType, String currentLabel) {
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

    private boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

    private String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED -> "\u5df2\u901a\u8fc7";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case "DRAFT" -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u5ba1\u6279\u4e2d";
        };
    }

    private String resolveExpenseDetailType(ProcessDocumentTemplate template, ProcessExpenseDetailDesign expenseDetailDesign) {
        if (expenseDetailDesign != null && trimToNull(expenseDetailDesign.getDetailType()) != null) {
            return expenseDetailDesign.getDetailType();
        }
        return DETAIL_TYPE_NORMAL;
    }

    private String resolveExpenseDetailTypeLabel(String detailType) {
        return Objects.equals(trimToNull(detailType), DETAIL_TYPE_ENTERPRISE)
                ? "\u4f01\u4e1a\u5f80\u6765"
                : "\u666e\u901a\u62a5\u9500";
    }

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
            return ENTERPRISE_MODE_PREPAY_UNBILLED;
        }
        return normalizedMode;
    }

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
        }
        if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            if (Objects.equals(businessSceneMode, ENTERPRISE_MODE_PREPAY_UNBILLED)) {
                normalized.remove(FIELD_INVOICE_AMOUNT);
                normalized.remove(FIELD_INVOICE_ATTACHMENTS);
            } else if (Objects.equals(businessSceneMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
                normalized.remove(FIELD_PENDING_WRITE_OFF_AMOUNT);
            }
        }
        return normalized;
    }

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
        }
        return businessSceneMode;
    }

    private String resolveBusinessSceneMode(String detailType, Object rawMode, String defaultBusinessSceneMode) {
        if (!Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)) {
            return ENTERPRISE_MODE_INVOICE_FULL_PAYMENT;
        }
        String normalizedMode = trimToNull(rawMode == null ? null : String.valueOf(rawMode));
        if (normalizedMode == null) {
            normalizedMode = trimToNull(defaultBusinessSceneMode);
        }
        if (!Objects.equals(normalizedMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
                && !Objects.equals(normalizedMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return ENTERPRISE_MODE_PREPAY_UNBILLED;
        }
        return normalizedMode;
    }

    private BigDecimal readInvoiceAmountForStorage(String detailType, String businessSceneMode, Map<String, Object> formData) {
        if (Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)
                && !Objects.equals(businessSceneMode, ENTERPRISE_MODE_INVOICE_FULL_PAYMENT)) {
            return null;
        }
        return toBigDecimal(formData.get(FIELD_INVOICE_AMOUNT));
    }

    private BigDecimal readPendingWriteOffAmountForStorage(String detailType, String businessSceneMode, Map<String, Object> formData) {
        if (!Objects.equals(detailType, DETAIL_TYPE_ENTERPRISE)
                || !Objects.equals(businessSceneMode, ENTERPRISE_MODE_PREPAY_UNBILLED)) {
            return null;
        }
        return toBigDecimal(formData.get(FIELD_PENDING_WRITE_OFF_AMOUNT));
    }

    private String buildAccountLabel(String accountName, String bankName) {
        String left = firstNonBlank(accountName, bankName);
        String right = left != null && Objects.equals(left, trimToNull(bankName)) ? null : trimToNull(bankName);
        return right == null ? (left == null ? "\u672a\u547d\u540d\u8d26\u6237" : left) : left + " / " + right;
    }

    private String buildVendorAccountSecondary(FinanceVendor vendor) {
        List<String> parts = new ArrayList<>();
        if (trimToNull(vendor.getCVenBankNub()) != null) {
            parts.add(vendor.getCVenBankNub().trim());
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
        return normalized == null ? "褰撳墠鐢ㄦ埛" : normalized;
    }

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
            throw new IllegalStateException("鐩爣瀹℃壒浜轰笉瀛樺湪鎴栧凡鍋滅敤");
        }
        return user;
    }

    private String requireCurrentUserCompanyId(Long userId) {
        User user = requireActiveUser(userId);
        String companyId = trimToNull(user.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("瑜版挸澧犻悽銊﹀煕閺堫亞绮︾€规艾鍙曢崣闀愬瘜娴?");
        }
        return companyId;
    }

    private User loadActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null && Objects.equals(user.getStatus(), 1) ? user : null;
    }

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
        String name = trimToNull(user.getName());
        return name != null ? name : defaultText(asText(user.getUsername()), "鏈懡鍚嶇敤鎴?");
    }

    private String resolveUserDisplayName(Long userId, String username) {
        return resolveUserDisplayName(loadActiveUser(userId), username);
    }

    private String resolveUserDisplayName(User user, String username) {
        if (user != null) {
            return normalizeUserName(user);
        }
        return defaultUsername(username);
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

    private String trimToNull(String value) {
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

    private static final class SummaryEnrichmentData {
        private static final SummaryEnrichmentData EMPTY = new SummaryEnrichmentData(Collections.emptyMap());
        private final Map<String, SummaryMetadata> metadataByDocumentCode;

        private SummaryEnrichmentData(Map<String, SummaryMetadata> metadataByDocumentCode) {
            this.metadataByDocumentCode = metadataByDocumentCode;
        }

        private static SummaryEnrichmentData empty() {
            return EMPTY;
        }

        private SummaryMetadata metadata(String documentCode) {
            return metadataByDocumentCode.getOrDefault(documentCode, SummaryMetadata.empty());
        }
    }

    private static final class SummaryMetadata {
        private static final SummaryMetadata EMPTY = new SummaryMetadata(
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );

        private final String submitterDeptName;
        private final String paymentCompanyId;
        private final String paymentCompanyName;
        private final String payeeName;
        private final String counterpartyName;
        private final String paymentDate;
        private final List<String> undertakeDepartmentNames;
        private final List<String> tagNames;

        private SummaryMetadata(
                String submitterDeptName,
                String paymentCompanyId,
                String paymentCompanyName,
                String payeeName,
                String counterpartyName,
                String paymentDate,
                List<String> undertakeDepartmentNames,
                List<String> tagNames
        ) {
            this.submitterDeptName = submitterDeptName;
            this.paymentCompanyId = paymentCompanyId;
            this.paymentCompanyName = paymentCompanyName;
            this.payeeName = payeeName;
            this.counterpartyName = counterpartyName;
            this.paymentDate = paymentDate;
            this.undertakeDepartmentNames = undertakeDepartmentNames == null ? Collections.emptyList() : undertakeDepartmentNames;
            this.tagNames = tagNames == null ? Collections.emptyList() : tagNames;
        }

        private static SummaryMetadata empty() {
            return EMPTY;
        }

        private String submitterDeptName() {
            return submitterDeptName;
        }

        private String paymentCompanyId() {
            return paymentCompanyId;
        }

        private String paymentCompanyName() {
            return paymentCompanyName;
        }

        private String payeeName() {
            return payeeName;
        }

        private String counterpartyName() {
            return counterpartyName;
        }

        private String paymentDate() {
            return paymentDate;
        }

        private List<String> undertakeDepartmentNames() {
            return undertakeDepartmentNames;
        }

        private List<String> tagNames() {
            return tagNames;
        }
    }

    private static final class SummaryDraft {
        private String documentCode;
        private String paymentCompanyId;
        private String payeeValue;
        private String counterpartyValue;
        private String paymentDate;
        private List<String> undertakeDepartmentIds = Collections.emptyList();
        private String tagArchiveCode;
        private List<String> tagValues = Collections.emptyList();

        private void setDocumentCode(String documentCode) {
            this.documentCode = documentCode;
        }

        private void setPaymentCompanyId(String paymentCompanyId) {
            this.paymentCompanyId = paymentCompanyId;
        }

        private void setPayeeValue(String payeeValue) {
            this.payeeValue = payeeValue;
        }

        private void setCounterpartyValue(String counterpartyValue) {
            this.counterpartyValue = counterpartyValue;
        }

        private void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        private void setUndertakeDepartmentIds(List<String> undertakeDepartmentIds) {
            this.undertakeDepartmentIds = undertakeDepartmentIds == null ? Collections.emptyList() : undertakeDepartmentIds;
        }

        private void setTagArchiveCode(String tagArchiveCode) {
            this.tagArchiveCode = tagArchiveCode;
        }

        private void setTagValues(List<String> tagValues) {
            this.tagValues = tagValues == null ? Collections.emptyList() : tagValues;
        }

        private String getPaymentCompanyId() {
            return paymentCompanyId;
        }

        private String getPayeeValue() {
            return payeeValue;
        }

        private String getCounterpartyValue() {
            return counterpartyValue;
        }

        private String getPaymentDate() {
            return paymentDate;
        }

        private List<String> getUndertakeDepartmentIds() {
            return undertakeDepartmentIds;
        }

        private String getTagArchiveCode() {
            return tagArchiveCode;
        }

        private List<String> getTagValues() {
            return tagValues;
        }
    }

}
