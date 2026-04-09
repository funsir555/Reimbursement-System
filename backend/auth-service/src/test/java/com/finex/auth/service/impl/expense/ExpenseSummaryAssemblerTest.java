package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSummaryAssemblerTest {

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
    private ExpenseSummaryAssembler assembler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        assembler = new ExpenseSummaryAssembler(
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
    }

    @Test
    void toExpenseSummariesUsesCanonicalPaymentDateLabel() throws Exception {
        stubSummaryLookups(true);

        var result = assembler.toExpenseSummaries(List.of(buildSummaryInstance("\u652f\u4ed8\u65e5\u671f")));

        assertEquals(1, result.size());
        assertEquals("2026-04-06", result.get(0).getPaymentDate());
    }

    @Test
    void toExpenseSummariesAcceptsLegacyPaymentDateLabelAlias() throws Exception {
        stubSummaryLookups(false);

        var result = assembler.toExpenseSummaries(List.of(buildSummaryInstance(legacyUtf8AsGbk("\u652f\u4ed8\u65e5\u671f"))));

        assertEquals(1, result.size());
        assertEquals("2026-04-06", result.get(0).getPaymentDate());
    }

    @Test
    void toExpenseSummariesFallsBackToSingleDateFieldWhenLabelIsUnknown() throws Exception {
        stubSummaryLookups(false);

        var result = assembler.toExpenseSummaries(List.of(buildSummaryInstance("\u4efb\u610f\u65e5\u671f")));

        assertEquals(1, result.size());
        assertEquals("2026-04-06", result.get(0).getPaymentDate());
    }

    @Test
    void toPendingItemsAddsDeptPaymentPayeeTagAndStatusEnrichment() throws Exception {
        stubSummaryLookups(true);
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setId(10L);
        task.setDocumentCode("DOC-001");
        task.setNodeKey("NODE-1");
        task.setNodeName("finance");
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.of(2026, 4, 4, 11, 0));

        var result = assembler.toPendingItems(List.of(task), Map.of("DOC-001", buildSummaryInstance()));

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        assertEquals("PENDING_APPROVAL", result.get(0).getDocumentStatus());
        assertEquals("\u5ba1\u6279\u4e2d", result.get(0).getDocumentStatusLabel());
        assertEquals("\u534e\u5357\u516c\u53f8", result.get(0).getPaymentCompanyName());
        assertEquals("\u674e\u56db", result.get(0).getPayeeName());
        assertEquals(List.of("\u5e02\u573a\u90e8", "\u9500\u552e\u90e8"), result.get(0).getUndertakeDepartmentNames());
        assertEquals(List.of("\u91cd\u70b9"), result.get(0).getTagNames());
    }

    private void stubSummaryLookups(boolean includeTags) throws Exception {
        ProcessDocumentTemplate template = createTemplate();
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(buildSummaryExpenseDetail()));
        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(userMapper.selectList(any())).thenReturn(List.of(buildSubmitterUser(), buildPayeeUser()));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany()));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor()));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildSubmitterDept(),
                buildUndertakeDept(5L, "\u5e02\u573a\u90e8"),
                buildUndertakeDept(6L, "\u9500\u552e\u90e8")
        ));
        if (includeTags) {
            when(processTemplateScopeMapper.selectList(any())).thenReturn(List.of(buildTagScope(template.getId())));
            when(customArchiveDesignMapper.selectList(any())).thenReturn(List.of(buildArchiveDesign()));
            when(customArchiveItemMapper.selectList(any())).thenReturn(List.of(buildArchiveItem()));
        } else {
            when(processTemplateScopeMapper.selectList(any())).thenReturn(List.of());
        }
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
        archiveItem.setItemName("\u91cd\u70b9");
        archiveItem.setStatus(1);
        return archiveItem;
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
        return buildSummaryInstance("\u652f\u4ed8\u65e5\u671f");
    }

    private ProcessDocumentInstance buildSummaryInstance(String paymentDateLabel) throws Exception {
        List<Map<String, Object>> blocks = List.of(
                createBusinessComponentBlock("payment-company-field", "payment-company", List.of("report")),
                createBusinessComponentBlock("payee-field", "payee", List.of("report")),
                createBusinessComponentBlock("counterparty-field", "counterparty", List.of("report")),
                createBusinessComponentBlock("undertake-field", "undertake-department", List.of("report")),
                createDateBlock("payment-date-field", paymentDateLabel),
                createArchiveBlock("tag-field", "\u6807\u7b7e", "PROCESS_TAG_OPTIONS")
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
        instance.setTemplateName("report-template");
        instance.setTemplateType("report");
        instance.setDocumentTitle("report-title");
        instance.setDocumentReason("report-reason");
        instance.setSubmitterUserId(1L);
        instance.setSubmitterName("\u5f20\u4e09");
        instance.setCurrentNodeName("finance");
        instance.setStatus("PENDING_APPROVAL");
        instance.setTotalAmount(BigDecimal.valueOf(888.88));
        instance.setCreatedAt(LocalDateTime.of(2026, 4, 4, 9, 30));
        instance.setFormDataJson(objectMapper.writeValueAsString(formData));
        instance.setFormSchemaSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "layoutMode", "TWO_COLUMN",
                "blocks", blocks
        )));
        instance.setTemplateSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "templateTypeLabel", "\u62a5\u9500\u5355"
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

    private String legacyUtf8AsGbk(String value) {
        return new String(value.getBytes(StandardCharsets.UTF_8), Charset.forName("GBK"));
    }

    private User buildSubmitterUser() {
        User user = new User();
        user.setId(1L);
        user.setName("\u5f20\u4e09");
        user.setDeptId(9L);
        return user;
    }

    private User buildPayeeUser() {
        User user = new User();
        user.setId(2L);
        user.setName("\u674e\u56db");
        user.setDeptId(8L);
        return user;
    }

    private SystemCompany buildCompany() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY-001");
        company.setCompanyName("\u534e\u5357\u516c\u53f8");
        return company;
    }

    private FinanceVendor buildVendor() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode("VEN-001");
        vendor.setCVenName("\u5e7f\u5dde\u4f9b\u5e94\u5546");
        return vendor;
    }

    private SystemDepartment buildSubmitterDept() {
        SystemDepartment department = new SystemDepartment();
        department.setId(9L);
        department.setDeptName("\u8d22\u52a1\u90e8");
        return department;
    }

    private SystemDepartment buildUndertakeDept(Long id, String name) {
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(name);
        return department;
    }
}
