package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentVO;
import com.finex.auth.dto.ExpenseBankLinkSaveDTO;
import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.PmBankPaymentRecord;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.entity.ProcessFlowVersion;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.PmBankPaymentRecordMapper;
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
import com.finex.auth.mapper.ProcessExpenseDetailDesignMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.ProcessFormDesignMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.FinanceVendorService;
import com.finex.auth.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentServiceImplTest {

    @Mock
    private ProcessDocumentTemplateMapper templateMapper;
    @Mock
    private ProcessFormDesignMapper processFormDesignMapper;
    @Mock
    private ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    @Mock
    private ProcessCustomArchiveItemMapper customArchiveItemMapper;
    @Mock
    private ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    @Mock
    private ProcessFlowMapper processFlowMapper;
    @Mock
    private ProcessFlowVersionMapper processFlowVersionMapper;
    @Mock
    private ProcessTemplateScopeMapper processTemplateScopeMapper;
    @Mock
    private FinanceVendorMapper financeVendorMapper;
    @Mock
    private SystemPermissionMapper systemPermissionMapper;
    @Mock
    private SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private SystemRolePermissionMapper systemRolePermissionMapper;
    @Mock
    private SystemUserRoleMapper systemUserRoleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;
    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    @Mock
    private PmBankPaymentRecordMapper pmBankPaymentRecordMapper;
    @Mock
    private ExpenseAttachmentService expenseAttachmentService;
    @Mock
    private FinanceVendorService financeVendorService;
    @Mock
    private ProcessExpenseDetailDesignMapper processExpenseDetailDesignMapper;
    @Mock
    private ProcessExpenseTypeMapper processExpenseTypeMapper;
    @Mock
    private ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;
    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private ExpenseDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ExpenseDocumentServiceImpl(
                templateMapper,
                processFormDesignMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processFlowMapper,
                processFlowVersionMapper,
                processTemplateScopeMapper,
                financeVendorMapper,
                systemPermissionMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                userMapper,
                userBankAccountMapper,
                processDocumentInstanceMapper,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                pmBankPaymentRecordMapper,
                expenseAttachmentService,
                financeVendorService,
                processExpenseDetailDesignMapper,
                processExpenseTypeMapper,
                expenseDetailSystemFieldSupport,
                notificationService,
                objectMapper
        );
    }

    @Test
    void submitDocumentKeepsPaymentCompanyAndAttachmentMetadataInJson() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        ProcessFormDesign formDesign = createFormDesign();
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();
        User submitter = new User();
        submitter.setId(1L);
        submitter.setName("zhangsan");
        submitter.setStatus(1);
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(100L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentExpenseDetailMapper.insert(any(ProcessDocumentExpenseDetail.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertNotNull(result);
        assertTrue(result.getDocumentCode().startsWith("DOC"));
        assertEquals("APPROVED", result.getStatus());

        ArgumentCaptor<ProcessDocumentInstance> instanceCaptor = ArgumentCaptor.forClass(ProcessDocumentInstance.class);
        verify(processDocumentInstanceMapper).insert(instanceCaptor.capture());
        Map<String, Object> storedMainForm = objectMapper.readValue(
                instanceCaptor.getValue().getFormDataJson(),
                new TypeReference<LinkedHashMap<String, Object>>() {}
        );
        assertEquals("COMPANY202603260001", storedMainForm.get("payment-company-1775147068712-fc8890"));

        ArgumentCaptor<ProcessDocumentExpenseDetail> detailCaptor = ArgumentCaptor.forClass(ProcessDocumentExpenseDetail.class);
        verify(processDocumentExpenseDetailMapper).insert(detailCaptor.capture());
        ProcessDocumentExpenseDetail detail = detailCaptor.getValue();
        assertEquals("INVOICE_FULL_PAYMENT", detail.getBusinessSceneMode());
        assertEquals(0, detail.getInvoiceAmount().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, detail.getActualPaymentAmount().compareTo(BigDecimal.valueOf(100)));
        assertNull(detail.getPendingWriteOffAmount());

        Map<String, Object> storedDetailForm = objectMapper.readValue(
                detail.getFormDataJson(),
                new TypeReference<LinkedHashMap<String, Object>>() {}
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) storedDetailForm.get("invoiceAttachments");
        assertEquals(1, attachments.size());
        assertEquals("750032b377f24ab99bb3fd0b7c411887", attachments.get(0).get("attachmentId"));
        assertEquals("application/pdf", attachments.get(0).get("contentType"));
        assertEquals("invoice-demo.pdf", attachments.get(0).get("fileName"));
        verify(processDocumentTaskMapper, never()).insert(any());
    }

    @Test
    void submitDocumentRejectsMissingFlowBindingBeforePersistence() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        template.setApprovalFlow("FLOW-001");
        ProcessFormDesign formDesign = createFormDesign();
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.submitDocument(1L, "zhangsan", dto)
        );

        assertEquals("\u5f53\u524d\u5ba1\u6279\u6a21\u677f\u7ed1\u5b9a\u7684\u6d41\u7a0b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u4fee\u590d\u6a21\u677f\u914d\u7f6e", exception.getMessage());
        verify(processDocumentInstanceMapper, never()).insert(any(ProcessDocumentInstance.class));
    }

    @Test
    void submitDocumentKeepsDetailNoScopedWithinDocument() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        ProcessFormDesign formDesign = createFormDesign();
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();
        User submitter = new User();
        submitter.setId(1L);
        submitter.setName("zhangsan");
        submitter.setStatus(1);
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 1L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(100L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentExpenseDetailMapper.insert(any(ProcessDocumentExpenseDetail.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        service.submitDocument(1L, "zhangsan", dto);
        service.submitDocument(1L, "zhangsan", dto);

        ArgumentCaptor<ProcessDocumentInstance> instanceCaptor = ArgumentCaptor.forClass(ProcessDocumentInstance.class);
        verify(processDocumentInstanceMapper, times(2)).insert(instanceCaptor.capture());
        List<ProcessDocumentInstance> instances = instanceCaptor.getAllValues();

        ArgumentCaptor<ProcessDocumentExpenseDetail> detailCaptor = ArgumentCaptor.forClass(ProcessDocumentExpenseDetail.class);
        verify(processDocumentExpenseDetailMapper, times(2)).insert(detailCaptor.capture());
        List<ProcessDocumentExpenseDetail> details = detailCaptor.getAllValues();

        assertEquals("D001", details.get(0).getDetailNo());
        assertEquals("D001", details.get(1).getDetailNo());
        assertNotEquals(instances.get(0).getDocumentCode(), instances.get(1).getDocumentCode());
        assertEquals(instances.get(0).getDocumentCode(), details.get(0).getDocumentCode());
        assertEquals(instances.get(1).getDocumentCode(), details.get(1).getDocumentCode());
    }

    @Test
    void getTemplateDetailReturnsContractLabelWithoutExpenseDetailDesign() throws Exception {
        ProcessDocumentTemplate template = createContractTemplate();
        ProcessFormDesign formDesign = createFormDesign();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(userMapper.selectById(1L)).thenReturn(new User());

        var detail = service.getTemplateDetail(1L, template.getTemplateCode());

        assertEquals("contract", detail.getTemplateType());
        assertEquals("合同单", detail.getTemplateTypeLabel());
        assertEquals("FORM-001", detail.getFormDesignCode());
        assertEquals("合同主表单", detail.getFormName());
        assertNull(detail.getExpenseDetailDesignCode());
    }

    @Test
    void submitDocumentAllowsContractTemplateWithoutExpenseDetails() throws Exception {
        ProcessDocumentTemplate template = createContractTemplate();
        ProcessFormDesign formDesign = createFormDesign();
        ExpenseDocumentSubmitDTO dto = createContractSubmitDto();
        User submitter = new User();
        submitter.setId(1L);
        submitter.setName("zhangsan");
        submitter.setStatus(1);
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(101L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(processDocumentExpenseDetailMapper, never()).insert(any(ProcessDocumentExpenseDetail.class));
        verify(processExpenseDetailDesignMapper, never()).selectOne(any());
    }

    @Test
    void getDocumentPickerGroupsRelatedAndWriteoffDocumentsByAllowedTypes() {
        ProcessDocumentInstance report = createApprovedDocument("DOC-REPORT-001", "report", "差旅报销单", BigDecimal.valueOf(1200), 1L);
        ProcessDocumentInstance application = createApprovedDocument("DOC-APP-001", "application", "出差申请单", BigDecimal.valueOf(300), 1L);
        ProcessDocumentInstance contract = createApprovedDocument("DOC-CON-001", "contract", "年度合同单", BigDecimal.valueOf(5000), 1L);
        ProcessDocumentInstance loan = createApprovedDocument("DOC-LOAN-001", "loan", "项目借款单", BigDecimal.valueOf(800), 1L);
        ProcessDocumentExpenseDetail prepayDetail = new ProcessDocumentExpenseDetail();
        prepayDetail.setDocumentCode("DOC-REPORT-001");
        prepayDetail.setBusinessSceneMode("PREPAY_UNBILLED");
        prepayDetail.setPendingWriteOffAmount(BigDecimal.valueOf(300));

        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(report, application, contract, loan));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(prepayDetail));
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of());

        var related = service.getDocumentPicker(1L, "RELATED", null, null, 1, 10, null, true);
        var writeoff = service.getDocumentPicker(1L, "WRITEOFF", null, null, 1, 10, null, true);

        assertEquals(List.of("report", "application", "contract", "loan"),
                related.getGroups().stream().map(item -> item.getTemplateType()).toList());
        assertEquals(List.of("report", "loan"),
                writeoff.getGroups().stream().map(item -> item.getTemplateType()).toList());
        assertEquals(Double.valueOf(300D), writeoff.getGroups().get(0).getItems().get(0).getAvailableWriteOffAmount());
        assertEquals(Double.valueOf(800D), writeoff.getGroups().get(1).getItems().get(0).getAvailableWriteOffAmount());
    }

    @Test
    void submitDocumentPersistsRelatedAndWriteoffBusinessBindings() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        template.setApprovalFlow("FLOW-001");
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("relatedDocs", "related-document", List.of("report", "application", "contract", "loan")),
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("report", "loan"))
        ));
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();
        dto.getFormData().put("relatedDocs", List.of(Map.of(
                "documentCode", "DOC-APP-001",
                "documentTitle", "出差申请单"
        )));
        dto.getFormData().put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-LOAN-001",
                "documentTitle", "项目借款单",
                "writeOffAmount", 120
        )));
        User submitter = createActiveUser(1L, "zhangsan");
        User approver = createActiveUser(2L, "李四");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();
        ProcessDocumentInstance relatedTarget = createApprovedDocument("DOC-APP-001", "application", "出差申请单", BigDecimal.valueOf(300), 2L);
        ProcessDocumentInstance writeoffTarget = createApprovedDocument("DOC-LOAN-001", "loan", "项目借款单", BigDecimal.valueOf(500), 2L);

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);
        when(processFlowMapper.selectOne(any())).thenReturn(createApprovalFlow());
        when(processFlowVersionMapper.selectById(11L)).thenReturn(createApprovalFlowVersion());
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(100L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(relatedTarget, writeoffTarget));
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentRelationMapper.insert(any(ProcessDocumentRelation.class))).thenReturn(1);
        when(processDocumentWriteOffMapper.insert(any(ProcessDocumentWriteOff.class))).thenReturn(1);
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentExpenseDetailMapper.insert(any(ProcessDocumentExpenseDetail.class))).thenReturn(1);
        when(processDocumentTaskMapper.insert(any(ProcessDocumentTask.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(approver));

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertEquals("PENDING_APPROVAL", result.getStatus());

        ArgumentCaptor<ProcessDocumentRelation> relationCaptor = ArgumentCaptor.forClass(ProcessDocumentRelation.class);
        verify(processDocumentRelationMapper).insert(relationCaptor.capture());
        assertEquals("relatedDocs", relationCaptor.getValue().getSourceFieldKey());
        assertEquals("DOC-APP-001", relationCaptor.getValue().getTargetDocumentCode());
        assertEquals("application", relationCaptor.getValue().getTargetTemplateType());

        ArgumentCaptor<ProcessDocumentWriteOff> writeOffCaptor = ArgumentCaptor.forClass(ProcessDocumentWriteOff.class);
        verify(processDocumentWriteOffMapper).insert(writeOffCaptor.capture());
        assertEquals("writeoffDocs", writeOffCaptor.getValue().getSourceFieldKey());
        assertEquals("DOC-LOAN-001", writeOffCaptor.getValue().getTargetDocumentCode());
        assertEquals("loan", writeOffCaptor.getValue().getTargetTemplateType());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(writeOffCaptor.getValue().getRequestedAmount()));
        assertEquals("LOAN", writeOffCaptor.getValue().getWriteoffSourceKind());
        assertEquals("PENDING_EFFECTIVE", writeOffCaptor.getValue().getStatus());
    }

    @Test
    void submitDocumentRejectsWriteoffTargetOutsideAllowedTemplateTypes() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("loan"))
        ));
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();
        dto.getFormData().put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-REPORT-001",
                "writeOffAmount", 88
        )));
        User submitter = createActiveUser(1L, "zhangsan");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();
        ProcessDocumentInstance reportTarget = createApprovedDocument("DOC-REPORT-001", "report", "预付报销单", BigDecimal.valueOf(200), 2L);

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(102L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(reportTarget));
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentExpenseDetailMapper.insert(any(ProcessDocumentExpenseDetail.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.submitDocument(1L, "zhangsan", dto)
        );

        assertEquals("核销单据类型不在当前组件允许范围内", exception.getMessage());
        verify(processDocumentWriteOffMapper, never()).insert(any(ProcessDocumentWriteOff.class));
    }

    @Test
    void listQueryDocumentSummariesReturnsAllNonDraftDocuments() {
        ProcessDocumentInstance pending = new ProcessDocumentInstance();
        pending.setDocumentCode("DOC-001");
        pending.setDocumentTitle("差旅审批单");
        pending.setDocumentReason("上海出差");
        pending.setTemplateName("差旅报销模板");
        pending.setSubmitterName("张三");
        pending.setCurrentNodeName("财务审批");
        pending.setStatus("PENDING_APPROVAL");
        pending.setTotalAmount(BigDecimal.valueOf(1880.5));
        pending.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 1, 9, 0));

        ProcessDocumentInstance approved = new ProcessDocumentInstance();
        approved.setDocumentCode("DOC-002");
        approved.setDocumentTitle("借款单");
        approved.setDocumentReason("项目借款");
        approved.setTemplateName("借款模板");
        approved.setSubmitterName("李四");
        approved.setCurrentNodeName(null);
        approved.setStatus("APPROVED");
        approved.setTotalAmount(BigDecimal.valueOf(5000));
        approved.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 2, 10, 0));

        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(approved, pending));

        var result = service.listQueryDocumentSummaries(9L);

        assertEquals(2, result.size());
        assertEquals("DOC-002", result.get(0).getDocumentCode());
        assertEquals("借款单", result.get(0).getDocumentTitle());
        assertEquals("李四", result.get(0).getSubmitterName());
        assertEquals("借款模板", result.get(0).getTemplateName());
        assertEquals("已通过", result.get(0).getStatus());
        assertEquals("DOC-001", result.get(1).getDocumentCode());
        assertEquals("财务审批", result.get(1).getCurrentNodeName());
    }

    @Test
    void submitDocumentWritesApproverNamesIntoApprovalPendingLog() throws Exception {
        ProcessDocumentTemplate template = createContractTemplate();
        template.setApprovalFlow("FLOW-001");
        ProcessFormDesign formDesign = createFormDesign();
        ProcessFlow flow = createApprovalFlow();
        ProcessFlowVersion version = createApprovalFlowVersion();
        ExpenseDocumentSubmitDTO dto = createContractSubmitDto();
        User submitter = createActiveUser(1L, "张三");
        User approver = createActiveUser(2L, "李四");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processFlowMapper.selectOne(any())).thenReturn(flow);
        when(processFlowVersionMapper.selectById(11L)).thenReturn(version);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(101L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentTaskMapper.insert(any())).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(approver));

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.getStatus());
        assertEquals("张三", storedInstance.get().getSubmitterName());

        ArgumentCaptor<ProcessDocumentActionLog> logCaptor = ArgumentCaptor.forClass(ProcessDocumentActionLog.class);
        verify(processDocumentActionLogMapper, times(2)).insert(logCaptor.capture());
        ProcessDocumentActionLog submitLog = logCaptor.getAllValues().stream()
                .filter(item -> "SUBMIT".equals(item.getActionType()))
                .findFirst()
                .orElseThrow();
        assertEquals("张三", submitLog.getActorName());
        ProcessDocumentActionLog approvalPendingLog = logCaptor.getAllValues().stream()
                .filter(item -> "APPROVAL_PENDING".equals(item.getActionType()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> payload = objectMapper.readValue(
                approvalPendingLog.getPayloadJson(),
                new TypeReference<LinkedHashMap<String, Object>>() {}
        );
        assertEquals(List.of(2), payload.get("approverUserIds"));
        assertEquals(List.of("李四"), payload.get("approverNames"));
    }

    @Test
    void submitDocumentFallsBackToUsernameWhenUserNameIsBlank() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        ProcessFormDesign formDesign = createFormDesign();
        ProcessExpenseDetailDesign detailDesign = createExpenseDetailDesign();
        ExpenseDocumentSubmitDTO dto = createSubmitDto();
        User submitter = new User();
        submitter.setId(1L);
        submitter.setUsername("zhangsan");
        submitter.setStatus(1);
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processExpenseDetailDesignMapper.selectOne(any())).thenReturn(detailDesign);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(104L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentExpenseDetailMapper.insert(any(ProcessDocumentExpenseDetail.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        service.submitDocument(1L, "zhangsan", dto);

        assertEquals("zhangsan", storedInstance.get().getSubmitterName());
        ArgumentCaptor<ProcessDocumentActionLog> logCaptor = ArgumentCaptor.forClass(ProcessDocumentActionLog.class);
        verify(processDocumentActionLogMapper, times(2)).insert(logCaptor.capture());
        ProcessDocumentActionLog submitLog = logCaptor.getAllValues().stream()
                .filter(item -> "SUBMIT".equals(item.getActionType()))
                .findFirst()
                .orElseThrow();
        assertEquals("zhangsan", submitLog.getActorName());
    }

    @Test
    void submitDocumentTreatsBlankParentNodeKeyAsRootAndCreatesApprovalTask() throws Exception {
        ProcessDocumentTemplate template = createContractTemplate();
        template.setApprovalFlow("FLOW-001");
        ProcessFormDesign formDesign = createFormDesign();
        ProcessFlow flow = createApprovalFlow();
        ProcessFlowVersion version = createApprovalFlowVersionWithBlankRootKey();
        ExpenseDocumentSubmitDTO dto = createContractSubmitDto();
        User submitter = createActiveUser(1L, "zhangsan");
        User approver = createActiveUser(2L, "lisi");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processFlowMapper.selectOne(any())).thenReturn(flow);
        when(processFlowVersionMapper.selectById(11L)).thenReturn(version);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(102L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentTaskMapper.insert(any(ProcessDocumentTask.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(approver));

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.getStatus());

        ArgumentCaptor<ProcessDocumentActionLog> logCaptor = ArgumentCaptor.forClass(ProcessDocumentActionLog.class);
        verify(processDocumentActionLogMapper, times(2)).insert(logCaptor.capture());
        List<ProcessDocumentActionLog> logs = logCaptor.getAllValues();
        assertTrue(logs.stream().anyMatch(item -> "APPROVAL_PENDING".equals(item.getActionType())));
        assertFalse(logs.stream().anyMatch(item -> "FINISH".equals(item.getActionType())));
        verify(processDocumentTaskMapper).insert(any(ProcessDocumentTask.class));
    }

    @Test
    void submitDocumentKeepsApprovedWhenFlowHasNoApprovalNodes() throws Exception {
        ProcessDocumentTemplate template = createContractTemplate();
        template.setApprovalFlow("FLOW-001");
        ProcessFormDesign formDesign = createFormDesign();
        ProcessFlow flow = createApprovalFlow();
        ProcessFlowVersion version = createFlowVersionWithoutNodes();
        ExpenseDocumentSubmitDTO dto = createContractSubmitDto();
        User submitter = createActiveUser(1L, "zhangsan");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>();

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(processFormDesignMapper.selectOne(any())).thenReturn(formDesign);
        when(processFlowMapper.selectOne(any())).thenReturn(flow);
        when(processFlowVersionMapper.selectById(11L)).thenReturn(version);
        when(processDocumentInstanceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            instance.setId(103L);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).insert(any(ProcessDocumentInstance.class));
        doAnswer(invocation -> {
            ProcessDocumentInstance instance = invocation.getArgument(0);
            storedInstance.set(copyInstance(instance));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentInstanceMapper.selectOne(any())).thenAnswer(invocation -> storedInstance.get());
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(submitter);

        ExpenseDocumentSubmitResultVO result = service.submitDocument(1L, "zhangsan", dto);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(processDocumentTaskMapper, never()).insert(any(ProcessDocumentTask.class));
    }

    @Test
    void repairMisapprovedDocumentsRebuildsPendingTasksForBlankRootSnapshots() throws Exception {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setId(201L);
        instance.setDocumentCode("DOC-REPAIR-001");
        instance.setStatus("APPROVED");
        instance.setApprovalFlowCode("FLOW-001");
        instance.setSubmitterUserId(1L);
        instance.setSubmitterName("lisi");
        instance.setFlowSnapshotJson(createApprovalFlowVersionWithBlankRootKey().getSnapshotJson());
        instance.setFormSchemaSnapshotJson(objectMapper.writeValueAsString(Map.of("blocks", List.of())));
        instance.setFormDataJson(objectMapper.writeValueAsString(Map.of()));

        ProcessDocumentActionLog submitLog = new ProcessDocumentActionLog();
        submitLog.setId(301L);
        submitLog.setDocumentCode(instance.getDocumentCode());
        submitLog.setActionType("SUBMIT");

        ProcessDocumentActionLog finishLog = new ProcessDocumentActionLog();
        finishLog.setId(302L);
        finishLog.setDocumentCode(instance.getDocumentCode());
        finishLog.setActionType("FINISH");
        finishLog.setActionComment("Approval flow finished");

        User approver = createActiveUser(2L, "lisi");
        AtomicReference<ProcessDocumentInstance> storedInstance = new AtomicReference<>(copyInstance(instance));

        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(submitLog, finishLog));
        when(processDocumentTaskMapper.selectCount(any())).thenReturn(0L);
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            ProcessDocumentInstance updated = invocation.getArgument(0);
            storedInstance.set(copyInstance(updated));
            return 1;
        }).when(processDocumentInstanceMapper).updateById(any(ProcessDocumentInstance.class));
        when(processDocumentActionLogMapper.deleteById(302L)).thenReturn(1);
        when(processDocumentActionLogMapper.insert(any(ProcessDocumentActionLog.class))).thenReturn(1);
        when(processDocumentTaskMapper.insert(any(ProcessDocumentTask.class))).thenReturn(1);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(approver));

        List<String> repaired = service.repairMisapprovedDocumentsByRootContainerBug();

        assertEquals(List.of("DOC-REPAIR-001"), repaired);
        verify(processDocumentActionLogMapper).deleteById(302L);
        verify(processDocumentTaskMapper).insert(any(ProcessDocumentTask.class));
        assertEquals("PENDING_APPROVAL", storedInstance.get().getStatus());
        assertEquals("finance", storedInstance.get().getCurrentNodeKey());
    }

    @Test
    void listExpenseSummariesBuildsAdvancedFilterFields() throws Exception {
        ProcessDocumentInstance instance = buildSummaryInstance();
        ProcessDocumentExpenseDetail detail = buildSummaryExpenseDetail();
        ProcessDocumentTemplate template = createTemplate();
        template.setId(11L);
        template.setTemplateTypeLabel("报销单");
        ProcessTemplateScope scope = new ProcessTemplateScope();
        scope.setTemplateId(11L);
        scope.setOptionType("TAG_ARCHIVE");
        scope.setOptionCode("PROCESS_TAG_OPTIONS");
        ProcessCustomArchiveDesign archiveDesign = new ProcessCustomArchiveDesign();
        archiveDesign.setId(31L);
        archiveDesign.setArchiveCode("PROCESS_TAG_OPTIONS");
        ProcessCustomArchiveItem archiveItem = new ProcessCustomArchiveItem();
        archiveItem.setArchiveId(31L);
        archiveItem.setItemCode("TAG-A");
        archiveItem.setItemName("重点");
        archiveItem.setStatus(1);

        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(instance));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(detail));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(processTemplateScopeMapper.selectList(any())).thenReturn(List.of(scope));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(archiveDesign));
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(archiveItem));
        when(userMapper.selectList(any())).thenReturn(List.of(buildSubmitterUser(), buildPayeeUser()));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany()));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor()));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildSubmitterDept(),
                buildUndertakeDept(5L, "市场部"),
                buildUndertakeDept(6L, "销售部")
        ));

        var result = service.listExpenseSummaries(1L);

        assertEquals(1, result.size());
        assertEquals("审批中", result.get(0).getDocumentStatusLabel());
        assertEquals("财务部", result.get(0).getSubmitterDeptName());
        assertEquals("华南公司", result.get(0).getPaymentCompanyName());
        assertEquals("李四", result.get(0).getPayeeName());
        assertEquals("广州供应商", result.get(0).getCounterpartyName());
        assertEquals("2026-04-06", result.get(0).getPaymentDate());
        assertEquals(List.of("市场部", "销售部"), result.get(0).getUndertakeDepartmentNames());
        assertEquals(List.of("重点"), result.get(0).getTagNames());
    }

    @Test
    void listPendingApprovalsKeepsTaskStatusAndAddsDocumentStatusFields() throws Exception {
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setNodeKey("NODE-1");
        task.setNodeName("财务审批");
        task.setStatus("PENDING");
        task.setAssigneeUserId(9L);
        task.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 4, 11, 0));

        ProcessDocumentTemplate template = createTemplate();
        template.setId(11L);
        ProcessTemplateScope scope = new ProcessTemplateScope();
        scope.setTemplateId(11L);
        scope.setOptionType("TAG_ARCHIVE");
        scope.setOptionCode("PROCESS_TAG_OPTIONS");

        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(buildSummaryInstance()));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(buildSummaryExpenseDetail()));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(processTemplateScopeMapper.selectList(any())).thenReturn(List.of(scope));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of(buildSubmitterUser(), buildPayeeUser()));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany()));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor()));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildSubmitterDept(),
                buildUndertakeDept(5L, "市场部"),
                buildUndertakeDept(6L, "销售部")
        ));

        var result = service.listPendingApprovals(9L);

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        assertEquals("PENDING_APPROVAL", result.get(0).getDocumentStatus());
        assertEquals("审批中", result.get(0).getDocumentStatusLabel());
        assertEquals("华南公司", result.get(0).getPaymentCompanyName());
    }

    @Test
    void getDocumentDetailBuildsReportDetailAndSkipsBrokenExpenseDetailRows() throws Exception {
        ProcessDocumentInstance instance = buildSummaryInstance();

        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(11L);
        task.setDocumentCode("DOC-001");
        task.setNodeKey("finance");
        task.setNodeName("Finance Approval");
        task.setNodeType("APPROVAL");
        task.setAssigneeUserId(2L);
        task.setAssigneeName("LiSi");
        task.setStatus("PENDING");
        task.setTaskBatchNo("BATCH-1");
        task.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 5, 10, 0));

        ProcessDocumentActionLog actionLog = new ProcessDocumentActionLog();
        actionLog.setId(21L);
        actionLog.setDocumentCode("DOC-001");
        actionLog.setActionType("SUBMIT");
        actionLog.setActorUserId(1L);
        actionLog.setActorName("ZhangSan");
        actionLog.setActionComment("submit");
        actionLog.setPayloadJson(objectMapper.writeValueAsString(Map.of()));
        actionLog.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 5, 9, 30));

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(instance);
        List<ProcessDocumentExpenseDetail> expenseDetails = new java.util.ArrayList<>();
        expenseDetails.add(null);
        expenseDetails.add(buildSummaryExpenseDetail());

        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(expenseDetails);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(actionLog));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany()));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(buildUndertakeDept(5L, "Market Dept")));

        var detail = service.getDocumentDetail(1L, "DOC-001", false);

        assertEquals("DOC-001", detail.getDocumentCode());
        assertEquals(1, detail.getExpenseDetails().size());
        assertEquals("D001", detail.getExpenseDetails().get(0).getDetailNo());
        assertEquals(1, detail.getCurrentTasks().size());
        assertEquals("LiSi", detail.getCurrentTasks().get(0).getAssigneeName());
        assertEquals(1, detail.getActionLogs().size());
        assertEquals("SUBMIT", detail.getActionLogs().get(0).getActionType());
        assertEquals(1, detail.getCompanyOptions().size());
        assertEquals("COMPANY-001", detail.getCompanyOptions().get(0).getValue());
        assertEquals(1, detail.getDepartmentOptions().size());
        assertEquals("5", detail.getDepartmentOptions().get(0).getValue());
    }

    @Test
    void getDocumentNavigationUsesPendingTaskSequenceFirst() {
        ProcessDocumentInstance currentInstance = new ProcessDocumentInstance();
        currentInstance.setDocumentCode("DOC-002");

        ProcessDocumentTask firstTask = new ProcessDocumentTask();
        firstTask.setDocumentCode("DOC-001");
        firstTask.setAssigneeUserId(9L);
        firstTask.setStatus("PENDING");

        ProcessDocumentTask currentTask = new ProcessDocumentTask();
        currentTask.setDocumentCode("DOC-002");
        currentTask.setAssigneeUserId(9L);
        currentTask.setStatus("PENDING");

        ProcessDocumentTask nextTask = new ProcessDocumentTask();
        nextTask.setDocumentCode("DOC-003");
        nextTask.setAssigneeUserId(9L);
        nextTask.setStatus("PENDING");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(currentInstance);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(firstTask, currentTask, nextTask));

        var navigation = service.getDocumentNavigation(9L, "DOC-002", true);

        assertEquals("DOC-001", navigation.getPrevDocumentCode());
        assertEquals("DOC-003", navigation.getNextDocumentCode());
        verify(processDocumentActionLogMapper, never()).selectList(any());
        verify(processDocumentInstanceMapper, never()).selectList(any());
    }

    @Test
    void getDocumentNavigationKeepsCurrentDocumentReachableOutsideHistoryMatches() {
        ProcessDocumentInstance currentInstance = new ProcessDocumentInstance();
        currentInstance.setDocumentCode("DOC-CURRENT");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(currentInstance);
        when(processDocumentTaskMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(currentInstance));

        var navigation = service.getDocumentNavigation(9L, "DOC-CURRENT", true);

        assertNull(navigation.getPrevDocumentCode());
        assertNull(navigation.getNextDocumentCode());
        verify(processDocumentInstanceMapper).selectList(any());
    }

    @Test
    void listBankLinksReturnsReadableBankLinkSummary() {
        SystemCompanyBankAccount account = createBankAccount(1L, "COMP-001");
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");
        account.setDirectConnectLastSyncStatus("最近推送成功");

        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setCompanyBankAccountId(1L);
        record.setReceiptStatus("RECEIVED");

        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(account));
        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(createCompany("COMP-001", "华南公司")));

        var result = service.listBankLinks();

        assertEquals(1, result.size());
        assertEquals("华南公司", result.get(0).getCompanyName());
        assertEquals("已启用", result.get(0).getDirectConnectStatusLabel());
        assertEquals("最近推送成功", result.get(0).getLastDirectConnectStatus());
        assertEquals("已获取回单", result.get(0).getLastReceiptStatus());
    }

    @Test
    void updateBankLinkEnablingCurrentAccountDisablesSiblingAccount() {
        SystemCompanyBankAccount current = createBankAccount(1L, "COMP-001");
        SystemCompanyBankAccount sibling = createBankAccount(2L, "COMP-001");
        sibling.setDirectConnectEnabled(1);
        sibling.setDirectConnectProvider("CMB");
        sibling.setDirectConnectChannel("CMB_CLOUD");

        ExpenseBankLinkSaveDTO dto = new ExpenseBankLinkSaveDTO();
        dto.setEnabled(true);
        dto.setDirectConnectProvider("CMB");
        dto.setDirectConnectChannel("CMB_CLOUD");
        dto.setOperatorKey("OP-001");
        dto.setCallbackSecret("SECRET-001");
        dto.setPublicKeyRef("PUB-001");
        dto.setReceiptQueryEnabled(true);
        dto.setDirectConnectCustomerNo("CUST-001");
        dto.setDirectConnectApiBaseUrl("https://cmb.example.com");

        when(systemCompanyBankAccountMapper.selectById(1L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(current, sibling));
        org.mockito.Mockito.lenient().when(systemCompanyBankAccountMapper.updateById(any(SystemCompanyBankAccount.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(systemCompanyMapper.selectList(any())).thenReturn(List.of(createCompany("COMP-001", "华南公司")));

        var result = service.updateBankLink(1L, dto);

        assertEquals(1, current.getDirectConnectEnabled());
        assertEquals("CMB", current.getDirectConnectProvider());
        assertEquals("CMB_CLOUD", current.getDirectConnectChannel());
        assertEquals(0, sibling.getDirectConnectEnabled());
        assertEquals("DISABLED", sibling.getDirectConnectLastSyncStatus());
        assertEquals("OP-001", result.getOperatorKey());
        assertTrue(result.isReceiptQueryEnabled());
        verify(systemCompanyBankAccountMapper, times(2)).updateById(any(SystemCompanyBankAccount.class));
    }

    @Test
    void runBankReceiptPollingAttachesReceiptAndMarksDocumentFinished() {
        PmBankPaymentRecord record = new PmBankPaymentRecord();
        record.setId(91L);
        record.setDocumentCode("DOC-PAY-001");
        record.setCompanyBankAccountId(1L);
        record.setManualPaid(0);
        record.setPaidAt(LocalDateTime.of(2026, 4, 6, 9, 30));
        record.setReceiptStatus("PENDING");
        record.setReceiptQueryCount(0);
        record.setBankOrderNo("BO-001");
        record.setBankFlowNo("BF-001");

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-PAY-001");
        instance.setDocumentTitle("付款单-001");
        instance.setStatus("PAYMENT_COMPLETED");

        SystemCompanyBankAccount account = createBankAccount(1L, "COMP-001");
        account.setDirectConnectEnabled(1);
        account.setDirectConnectProvider("CMB");
        account.setDirectConnectChannel("CMB_CLOUD");
        account.setDirectConnectExtJson("{\"receiptQueryEnabled\":true}");

        ExpenseAttachmentVO attachment = new ExpenseAttachmentVO();
        attachment.setAttachmentId("ATT-001");
        attachment.setFileName("DOC-PAY-001-银行回单.txt");
        attachment.setContentType("text/plain");
        attachment.setFileSize(128L);

        when(pmBankPaymentRecordMapper.selectList(any())).thenReturn(List.of(record));
        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(instance);
        when(systemCompanyBankAccountMapper.selectById(1L)).thenReturn(account);
        when(expenseAttachmentService.saveGeneratedAttachment(any(), any(), any())).thenReturn(attachment);
        when(pmBankPaymentRecordMapper.updateById(any(PmBankPaymentRecord.class))).thenReturn(1);
        when(processDocumentInstanceMapper.updateById(any(ProcessDocumentInstance.class))).thenReturn(1);

        service.runBankReceiptPolling();

        assertEquals("PAYMENT_FINISHED", instance.getStatus());
        assertEquals("RECEIVED", record.getReceiptStatus());
        assertEquals("ATT-001", record.getReceiptAttachmentId());
        assertEquals("DOC-PAY-001-银行回单.txt", record.getReceiptFileName());
        assertNotNull(record.getReceiptReceivedAt());
        verify(expenseAttachmentService).saveGeneratedAttachment(
                org.mockito.ArgumentMatchers.contains("DOC-PAY-001"),
                org.mockito.ArgumentMatchers.eq("text/plain"),
                any(byte[].class)
        );
        verify(pmBankPaymentRecordMapper).updateById(record);
        verify(processDocumentInstanceMapper).updateById(instance);
    }

    private SystemCompanyBankAccount createBankAccount(Long id, String companyId) {
        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        account.setId(id);
        account.setCompanyId(companyId);
        account.setBankName("招商银行");
        account.setAccountName("华南公司基本户");
        account.setAccountNo("6225880011223344");
        account.setStatus(1);
        account.setDirectConnectEnabled(0);
        return account;
    }

    private SystemCompany createCompany(String companyId, String companyName) {
        SystemCompany company = new SystemCompany();
        company.setCompanyId(companyId);
        company.setCompanyName(companyName);
        return company;
    }

    private ProcessDocumentTemplate createTemplate() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("FX202603310003");
        template.setTemplateName("enterprise-payment");
        template.setTemplateType("report");
        template.setEnabled(1);
        template.setFormDesignCode("FORM-001");
        template.setExpenseDetailDesignCode("EDD202603310002");
        template.setExpenseDetailModeDefault("INVOICE_FULL_PAYMENT");
        template.setCategoryCode("660100");
        return template;
    }

    private ProcessDocumentTemplate createContractTemplate() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setTemplateCode("HT202604040001");
        template.setTemplateName("contract-template");
        template.setTemplateType("contract");
        template.setTemplateTypeLabel("合同单");
        template.setEnabled(1);
        template.setFormDesignCode("FORM-001");
        template.setExpenseDetailDesignCode(null);
        template.setCategoryCode("business-application");
        return template;
    }

    private ProcessFormDesign createFormDesign() throws Exception {
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setFormCode("FORM-001");
        formDesign.setFormName("合同主表单");
        formDesign.setSchemaJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of()
        )));
        return formDesign;
    }

    private ProcessFormDesign createFormDesignWithBlocks(List<Map<String, Object>> blocks) throws Exception {
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setFormCode("FORM-001");
        formDesign.setFormName("合同主表单");
        formDesign.setSchemaJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", blocks
        )));
        return formDesign;
    }

    private Map<String, Object> createBusinessComponentBlock(
            String fieldKey,
            String componentCode,
            List<String> allowedTemplateTypes
    ) {
        return new LinkedHashMap<>(Map.of(
                "blockId", fieldKey,
                "fieldKey", fieldKey,
                "kind", "BUSINESS_COMPONENT",
                "label", fieldKey,
                "span", 1,
                "props", new LinkedHashMap<>(Map.of(
                        "componentCode", componentCode,
                        "allowedTemplateTypes", allowedTemplateTypes
                ))
        ));
    }

    private Map<String, Object> createDateBlock(String fieldKey, String label) {
        return new LinkedHashMap<>(Map.of(
                "blockId", fieldKey,
                "fieldKey", fieldKey,
                "kind", "FIELD",
                "label", label,
                "span", 1,
                "props", new LinkedHashMap<>(Map.of(
                        "controlType", "DATE"
                ))
        ));
    }

    private Map<String, Object> createArchiveBlock(String fieldKey, String label, String archiveCode) {
        return new LinkedHashMap<>(Map.of(
                "blockId", fieldKey,
                "fieldKey", fieldKey,
                "kind", "FIELD",
                "label", label,
                "span", 1,
                "props", new LinkedHashMap<>(Map.of(
                        "controlType", "SELECT",
                        "archiveCode", archiveCode
                ))
        ));
    }

    private ProcessDocumentInstance buildSummaryInstance() throws Exception {
        List<Map<String, Object>> blocks = List.of(
                createBusinessComponentBlock("payment-company-field", "payment-company", List.of("report")),
                createBusinessComponentBlock("payee-field", "payee", List.of("report")),
                createBusinessComponentBlock("counterparty-field", "counterparty", List.of("report")),
                createBusinessComponentBlock("undertake-field", "undertake-department", List.of("report")),
                createDateBlock("payment-date-field", "支付日期"),
                createArchiveBlock("tag-field", "标签", "PROCESS_TAG_OPTIONS")
        );
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("payment-company-field", "COMPANY-001");
        formData.put("payee-field", "USER:2");
        formData.put("counterparty-field", "VEN-001");
        formData.put("undertake-field", "5");
        formData.put("payment-date-field", "2026-04-06");
        formData.put("tag-field", List.of("TAG-A"));

        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setId(1L);
        instance.setDocumentCode("DOC-001");
        instance.setTemplateCode("FX202603310003");
        instance.setTemplateName("企业付款模板");
        instance.setTemplateType("report");
        instance.setDocumentTitle("企业付款审批单");
        instance.setDocumentReason("支付合同款");
        instance.setSubmitterUserId(1L);
        instance.setSubmitterName("张三");
        instance.setCurrentNodeName("财务审批");
        instance.setStatus("PENDING_APPROVAL");
        instance.setTotalAmount(BigDecimal.valueOf(888.88));
        instance.setCreatedAt(java.time.LocalDateTime.of(2026, 4, 4, 9, 30));
        instance.setFormDataJson(objectMapper.writeValueAsString(formData));
        instance.setFormSchemaSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", blocks
        )));
        instance.setTemplateSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "templateTypeLabel", "报销单"
        )));
        return instance;
    }

    private ProcessDocumentExpenseDetail buildSummaryExpenseDetail() throws Exception {
        ProcessDocumentExpenseDetail detail = new ProcessDocumentExpenseDetail();
        detail.setDocumentCode("DOC-001");
        detail.setDetailNo("D001");
        detail.setSortOrder(1);
        detail.setSchemaSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of(createBusinessComponentBlock("detail-undertake", "undertake-department", List.of("report")))
        )));
        detail.setFormDataJson(objectMapper.writeValueAsString(Map.of(
                "detail-undertake", "6"
        )));
        return detail;
    }

    private User buildSubmitterUser() {
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        user.setDeptId(9L);
        return user;
    }

    private User buildPayeeUser() {
        User user = new User();
        user.setId(2L);
        user.setName("李四");
        user.setDeptId(8L);
        return user;
    }

    private SystemCompany buildCompany() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY-001");
        company.setCompanyName("华南公司");
        return company;
    }

    private FinanceVendor buildVendor() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN-001");
        vendor.setCVenName("广州供应商");
        return vendor;
    }

    private SystemDepartment buildSubmitterDept() {
        SystemDepartment department = new SystemDepartment();
        department.setId(9L);
        department.setDeptName("财务部");
        return department;
    }

    private SystemDepartment buildUndertakeDept(Long id, String name) {
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(name);
        return department;
    }

    private ProcessExpenseDetailDesign createExpenseDetailDesign() throws Exception {
        ProcessExpenseDetailDesign detailDesign = new ProcessExpenseDetailDesign();
        detailDesign.setDetailCode("EDD202603310002");
        detailDesign.setDetailName("enterprise-detail-form");
        detailDesign.setDetailType("ENTERPRISE_TRANSACTION");
        detailDesign.setSchemaJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", List.of()
        )));
        return detailDesign;
    }

    private ExpenseDocumentSubmitDTO createSubmitDto() {
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        dto.setTemplateCode("FX202603310003");

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("payee-1774789826410-4660f7", "USER:2");
        formData.put("payee-account-1774789926171-24f717", "USER_ACCOUNT:2");
        formData.put("payment-company-1775147068712-fc8890", "COMPANY202603260001");
        formData.put("undertake-department-1775147063283-69bd2b", "5");
        formData.put("amount-1774789945054-dbffe5", 100);
        formData.put("__totalAmount", 100);
        dto.setFormData(formData);

        ExpenseDetailInstanceDTO detail = new ExpenseDetailInstanceDTO();
        detail.setDetailNo("D001");
        detail.setDetailDesignCode("EDD202603310002");
        detail.setDetailType("ENTERPRISE_TRANSACTION");
        detail.setEnterpriseMode("INVOICE_FULL_PAYMENT");
        detail.setExpenseTypeCode("660100");
        detail.setBusinessSceneMode("INVOICE_FULL_PAYMENT");
        detail.setDetailTitle("detail-1");
        detail.setSortOrder(1);

        Map<String, Object> detailFormData = new LinkedHashMap<>();
        detailFormData.put("expenseTypeCode", "660100");
        detailFormData.put("businessScenario", "INVOICE_FULL_PAYMENT");
        detailFormData.put("invoiceAmount", 100);
        detailFormData.put("actualPaymentAmount", 100);
        detailFormData.put("pendingWriteOffAmount", null);
        detailFormData.put("invoiceAttachments", List.of(new LinkedHashMap<>(Map.of(
                "attachmentId", "750032b377f24ab99bb3fd0b7c411887",
                "fileName", "invoice-demo.pdf",
                "contentType", "application/pdf",
                "fileSize", 109610,
                "previewUrl", "/api/auth/expenses/attachments/750032b377f24ab99bb3fd0b7c411887/content"
        ))));
        detail.setFormData(detailFormData);
        dto.setExpenseDetails(List.of(detail));
        return dto;
    }

    private ExpenseDocumentSubmitDTO createContractSubmitDto() {
        ExpenseDocumentSubmitDTO dto = new ExpenseDocumentSubmitDTO();
        dto.setTemplateCode("HT202604040001");
        dto.setFormData(new LinkedHashMap<>(Map.of(
                "contractName", "年度服务合同",
                "contractAmount", 50000
        )));
        dto.setExpenseDetails(List.of());
        return dto;
    }

    private ProcessFlow createApprovalFlow() {
        ProcessFlow flow = new ProcessFlow();
        flow.setFlowCode("FLOW-001");
        flow.setCurrentPublishedVersionId(11L);
        return flow;
    }

    private ProcessFlowVersion createApprovalFlowVersion() throws Exception {
        ProcessFlowVersion version = new ProcessFlowVersion();
        version.setId(11L);
        version.setSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "finance",
                                "nodeName", "财务审批",
                                "nodeType", "APPROVAL",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(2))
                                )
                        )
                ),
                "routes", List.of()
        )));
        return version;
    }

    private ProcessFlowVersion createApprovalFlowVersionWithBlankRootKey() throws Exception {
        ProcessFlowVersion version = new ProcessFlowVersion();
        version.setId(11L);
        version.setSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        Map.of(
                                "nodeKey", "finance",
                                "nodeName", "Finance Approval",
                                "nodeType", "APPROVAL",
                                "parentNodeKey", "",
                                "displayOrder", 1,
                                "config", Map.of(
                                        "approverType", "DESIGNATED_MEMBER",
                                        "designatedMemberConfig", Map.of("userIds", List.of(2))
                                )
                        )
                ),
                "routes", List.of()
        )));
        return version;
    }

    private ProcessFlowVersion createFlowVersionWithoutNodes() throws Exception {
        ProcessFlowVersion version = new ProcessFlowVersion();
        version.setId(11L);
        version.setSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "nodes", List.of(),
                "routes", List.of()
        )));
        return version;
    }

    private User createActiveUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setStatus(1);
        return user;
    }

    private ProcessDocumentInstance createApprovedDocument(
            String documentCode,
            String templateType,
            String documentTitle,
            BigDecimal totalAmount,
            Long submitterUserId
    ) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(documentCode);
        instance.setTemplateType(templateType);
        instance.setDocumentTitle(documentTitle);
        instance.setTemplateName(documentTitle + "-模板");
        instance.setStatus("APPROVED");
        instance.setSubmitterUserId(submitterUserId);
        instance.setTotalAmount(totalAmount);
        return instance;
    }

    @Test
    void listVendorOptionsUsesCurrentUserCompanyId() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setStatus(1);
        currentUser.setCompanyId("COMPANY_A");

        ExpenseCreateVendorOptionVO option = new ExpenseCreateVendorOptionVO();
        option.setCVenCode("VEN_A");
        option.setCVenName("Company Vendor");

        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(financeVendorService.listActiveVendorOptions("COMPANY_A", "Vendor", null)).thenReturn(List.of(option));

        List<ExpenseCreateVendorOptionVO> result = service.listVendorOptions(1L, "Vendor", null);

        assertEquals(1, result.size());
        assertEquals("VEN_A", result.get(0).getCVenCode());
        verify(financeVendorService).listActiveVendorOptions("COMPANY_A", "Vendor", null);
    }

    @Test
    void listPayeeOptionsPersonalOnlyReturnsCurrentUserEnabledPrivateNames() {
        UserBankAccount first = new UserBankAccount();
        first.setId(8L);
        first.setUserId(1L);
        first.setStatus(1);
        first.setAccountName("张三");
        first.setAccountNo("6222000011112222");
        first.setBankName("招商银行");
        first.setBranchName("上海分行");

        UserBankAccount duplicate = new UserBankAccount();
        duplicate.setId(9L);
        duplicate.setUserId(1L);
        duplicate.setStatus(1);
        duplicate.setAccountName("张三");
        duplicate.setAccountNo("6222000011113333");
        duplicate.setBankName("工商银行");
        duplicate.setBranchName("北京分行");

        UserBankAccount ignored = new UserBankAccount();
        ignored.setId(10L);
        ignored.setUserId(1L);
        ignored.setStatus(1);
        ignored.setAccountName("李四");
        ignored.setAccountNo("95588000001111");
        ignored.setBankName("建设银行");

        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(first, duplicate, ignored));

        List<ExpenseCreatePayeeOptionVO> result = service.listPayeeOptions(1L, "张", true);

        assertEquals(1, result.size());
        assertEquals("PERSONAL_PAYEE:张三", result.get(0).getValue());
        assertEquals("张三", result.get(0).getLabel());
        assertEquals("PERSONAL_PRIVATE_PAYEE", result.get(0).getSourceType());
        assertEquals("张三", result.get(0).getSourceCode());
        assertEquals("个人中心对私账户", result.get(0).getSecondaryLabel());
        verify(userBankAccountMapper).selectList(any());
    }

    @Test
    void listPayeeAccountOptionsEmployeeModeFiltersCurrentUserAccountsByPayeeName() {
        UserBankAccount first = new UserBankAccount();
        first.setId(8L);
        first.setUserId(1L);
        first.setStatus(1);
        first.setAccountName("张三");
        first.setAccountNo("6222000011112222");
        first.setBankName("招商银行");
        first.setBranchName("上海分行");

        UserBankAccount second = new UserBankAccount();
        second.setId(9L);
        second.setUserId(1L);
        second.setStatus(1);
        second.setAccountName("张三");
        second.setAccountNo("6222000099998888");
        second.setBankName("中国银行");
        second.setBranchName("浦东支行");

        UserBankAccount ignored = new UserBankAccount();
        ignored.setId(10L);
        ignored.setUserId(1L);
        ignored.setStatus(1);
        ignored.setAccountName("李四");
        ignored.setAccountNo("95588000001111");
        ignored.setBankName("建设银行");

        when(userBankAccountMapper.selectList(any())).thenReturn(List.of(first, second, ignored));

        List<ExpenseCreatePayeeAccountOptionVO> result = service.listPayeeAccountOptions(1L, "6222", "EMPLOYEE", "张三", null);

        assertEquals(2, result.size());
        assertEquals(List.of("USER_ACCOUNT:8", "USER_ACCOUNT:9"), result.stream().map(ExpenseCreatePayeeAccountOptionVO::getValue).toList());
        assertTrue(result.stream().allMatch(item -> "张三".equals(item.getOwnerName())));
        assertTrue(result.stream().allMatch(item -> "1".equals(item.getOwnerCode())));
        verify(userBankAccountMapper).selectList(any());
    }

    @Test
    void listPayeeAccountOptionsEnterpriseModeUsesCounterpartyOnly() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setStatus(1);
        currentUser.setCompanyId("COMPANY_A");

        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN_A");
        vendor.setCVenName("广州供应商");
        vendor.setCompanyId("COMPANY_A");
        vendor.setCVenBank("中国银行");
        vendor.setCVenAccount("6222000011112222");
        vendor.setReceiptAccountName("广州供应商");
        vendor.setReceiptBranchName("中国银行广州分行");

        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(financeVendorMapper.selectOne(any())).thenReturn(vendor);

        List<ExpenseCreatePayeeAccountOptionVO> result = service.listPayeeAccountOptions(1L, null, "ENTERPRISE", null, "VEN_A");

        assertEquals(1, result.size());
        assertEquals("VENDOR:VEN_A", result.get(0).getValue());
        assertEquals("VEN_A", result.get(0).getOwnerCode());
        assertEquals("广州供应商", result.get(0).getOwnerName());
        assertEquals("中国银行", result.get(0).getBankName());
    }

    private ProcessDocumentInstance copyInstance(ProcessDocumentInstance source) {
        ProcessDocumentInstance target = new ProcessDocumentInstance();
        target.setId(source.getId());
        target.setDocumentCode(source.getDocumentCode());
        target.setDocumentTitle(source.getDocumentTitle());
        target.setDocumentReason(source.getDocumentReason());
        target.setTemplateName(source.getTemplateName());
        target.setTemplateType(source.getTemplateType());
        target.setStatus(source.getStatus());
        target.setTotalAmount(source.getTotalAmount());
        target.setSubmitterUserId(source.getSubmitterUserId());
        target.setSubmitterName(source.getSubmitterName());
        target.setCurrentNodeKey(source.getCurrentNodeKey());
        target.setCurrentNodeName(source.getCurrentNodeName());
        target.setCurrentTaskType(source.getCurrentTaskType());
        target.setCreatedAt(source.getCreatedAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setTemplateSnapshotJson(source.getTemplateSnapshotJson());
        target.setFormSchemaSnapshotJson(source.getFormSchemaSnapshotJson());
        target.setFormDataJson(source.getFormDataJson());
        target.setFlowSnapshotJson(source.getFlowSnapshotJson());
        target.setApprovalFlowCode(source.getApprovalFlowCode());
        return target;
    }
}
