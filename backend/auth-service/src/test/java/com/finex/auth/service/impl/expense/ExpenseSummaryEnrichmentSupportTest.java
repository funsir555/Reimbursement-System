package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSummaryEnrichmentSupportTest {

    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentTemplateMapper templateMapper;
    @Mock
    private ProcessTemplateScopeMapper processTemplateScopeMapper;
    @Mock
    private ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    @Mock
    private ProcessCustomArchiveItemMapper customArchiveItemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private FinanceVendorMapper financeVendorMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;

    private ObjectMapper objectMapper;
    private ExpenseSummaryEnrichmentSupport enrichmentSupport;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ExpenseSummarySupportContext context = new ExpenseSummarySupportContext(
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                templateMapper,
                processTemplateScopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                userMapper,
                systemCompanyMapper,
                financeVendorMapper,
                systemDepartmentMapper,
                objectMapper
        );
        ExpenseSummaryLookupSupport lookupSupport = new ExpenseSummaryLookupSupport(context);
        ExpenseSummarySnapshotSupport snapshotSupport = new ExpenseSummarySnapshotSupport(context);
        enrichmentSupport = new ExpenseSummaryEnrichmentSupport(context, lookupSupport, snapshotSupport);
    }

    @Test
    void buildSummaryEnrichmentDataCollectsResolvedMetadataAndSubmitTime() throws Exception {
        stubSummaryLookups();
        ProcessDocumentActionLog resubmitLog = new ProcessDocumentActionLog();
        resubmitLog.setDocumentCode("DOC-001");
        resubmitLog.setActionType("RESUBMIT");
        resubmitLog.setCreatedAt(LocalDateTime.of(2026, 4, 20, 9, 30));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(resubmitLog));

        ExpenseSummaryAssembler.SummaryEnrichmentData data =
                enrichmentSupport.buildSummaryEnrichmentData(List.of(buildSummaryInstance("DOC-001", "PENDING_APPROVAL")));

        ExpenseSummaryAssembler.SummaryMetadata metadata = data.metadata("DOC-001");
        assertEquals("财务部", metadata.submitterDeptName());
        assertEquals("华南公司", metadata.paymentCompanyName());
        assertEquals("李四", metadata.payeeName());
        assertEquals("广州供应商", metadata.counterpartyName());
        assertEquals("2026-04-06", metadata.paymentDate());
        assertEquals(List.of("市场部", "销售部"), metadata.undertakeDepartmentNames());
        assertEquals(List.of("重点"), metadata.tagNames());
        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), data.submittedAt("DOC-001", buildSummaryInstance("DOC-001", "PENDING_APPROVAL")));
        assertFalse(data.draftDeletable("DOC-001"));
    }

    @Test
    void buildSummaryEnrichmentDataMarksBrandNewDraftAsDeletableAndUsesUpdatedAt() throws Exception {
        stubSummaryLookups();
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of());
        ProcessDocumentInstance draft = buildSummaryInstance("DOC-DRAFT", "DRAFT");
        draft.setCreatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        draft.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 12, 5));

        ExpenseSummaryAssembler.SummaryEnrichmentData data =
                enrichmentSupport.buildSummaryEnrichmentData(List.of(draft));

        assertEquals(LocalDateTime.of(2026, 4, 21, 12, 5), data.submittedAt("DOC-DRAFT", draft));
        assertTrue(data.draftDeletable("DOC-DRAFT"));
    }

    private void stubSummaryLookups() throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(buildSummaryExpenseDetail()));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(userMapper.selectList(any())).thenReturn(List.of(buildSubmitterUser(), buildPayeeUser()));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany()));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor()));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildDepartment(9L, "财务部"),
                buildDepartment(5L, "市场部"),
                buildDepartment(6L, "销售部")
        ));
        when(processTemplateScopeMapper.selectList(any())).thenReturn(List.of(buildTagScope(template.getId())));
        when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(buildArchiveDesign()));
        when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(buildArchiveItem()));
    }

    private ProcessDocumentTemplate createTemplate() {
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        template.setId(11L);
        template.setTemplateCode("FX202603310003");
        template.setTemplateName("report-template");
        template.setTemplateType("report");
        template.setEnabled(1);
        return template;
    }

    private ProcessTemplateScope buildTagScope(Long templateId) {
        ProcessTemplateScope scope = new ProcessTemplateScope();
        scope.setTemplateId(templateId);
        scope.setOptionType("TAG_ARCHIVE");
        scope.setOptionCode("PROCESS_TAG_OPTIONS");
        return scope;
    }

    private ProcessCustomArchiveDesign buildArchiveDesign() {
        ProcessCustomArchiveDesign archiveDesign = new ProcessCustomArchiveDesign();
        archiveDesign.setId(31L);
        archiveDesign.setArchiveCode("PROCESS_TAG_OPTIONS");
        return archiveDesign;
    }

    private ProcessCustomArchiveItem buildArchiveItem() {
        ProcessCustomArchiveItem archiveItem = new ProcessCustomArchiveItem();
        archiveItem.setArchiveId(31L);
        archiveItem.setItemCode("TAG-A");
        archiveItem.setItemName("重点");
        archiveItem.setStatus(1);
        return archiveItem;
    }

    private Map<String, Object> createBusinessComponentBlock(String fieldKey, String componentCode) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("componentCode", componentCode);
        props.put("allowedTemplateTypes", List.of("report"));

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("blockId", fieldKey);
        block.put("fieldKey", fieldKey);
        block.put("kind", "BUSINESS_COMPONENT");
        block.put("label", fieldKey);
        block.put("span", 1);
        block.put("props", props);
        return block;
    }

    private Map<String, Object> createDateBlock(String fieldKey, String label) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("controlType", "DATE");

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("blockId", fieldKey);
        block.put("fieldKey", fieldKey);
        block.put("kind", "FIELD");
        block.put("label", label);
        block.put("span", 1);
        block.put("props", props);
        return block;
    }

    private Map<String, Object> createArchiveBlock(String fieldKey, String label, String archiveCode) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("controlType", "SELECT");
        props.put("archiveCode", archiveCode);

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("blockId", fieldKey);
        block.put("fieldKey", fieldKey);
        block.put("kind", "FIELD");
        block.put("label", label);
        block.put("span", 1);
        block.put("props", props);
        return block;
    }

    private ProcessDocumentInstance buildSummaryInstance(String documentCode, String status) throws Exception {
        List<Map<String, Object>> blocks = List.of(
                createBusinessComponentBlock("payment-company-field", "payment-company"),
                createBusinessComponentBlock("payee-field", "payee"),
                createBusinessComponentBlock("counterparty-field", "counterparty"),
                createBusinessComponentBlock("undertake-field", "undertake-department"),
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
        instance.setDocumentCode(documentCode);
        instance.setTemplateCode("FX202603310003");
        instance.setTemplateName("report-template");
        instance.setTemplateType("report");
        instance.setDocumentTitle("report-title");
        instance.setDocumentReason("report-reason");
        instance.setSubmitterUserId(1L);
        instance.setSubmitterName("李四");
        instance.setCurrentNodeName("finance");
        instance.setStatus(status);
        instance.setTotalAmount(BigDecimal.valueOf(888.88));
        instance.setCreatedAt(LocalDateTime.of(2026, 4, 4, 9, 30));
        instance.setUpdatedAt(LocalDateTime.of(2026, 4, 4, 10, 0));
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
                "blocks", List.of(createBusinessComponentBlock("detail-undertake", "undertake-department"))
        )));
        detail.setFormDataJson(objectMapper.writeValueAsString(Map.of(
                "detail-undertake", "6"
        )));
        return detail;
    }

    private User buildSubmitterUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("lisi");
        user.setName("李四");
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

    private SystemDepartment buildDepartment(Long id, String name) {
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(name);
        return department;
    }
}
