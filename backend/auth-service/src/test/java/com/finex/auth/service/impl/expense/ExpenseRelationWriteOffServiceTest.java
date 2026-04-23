package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRelationWriteOffServiceTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ObjectMapper objectMapper;
    private ExpenseRelationWriteOffService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ExpenseRelationWriteOffService(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                objectMapper
        );
    }

    @Test
    void getDocumentPickerGroupsRelatedAndWriteoffDocumentsByAllowedTypes() {
        ProcessDocumentInstance report = createDocument("DOC-REPORT-001", "report", "report-doc", BigDecimal.valueOf(1200), 1L, "PENDING_PAYMENT");
        ProcessDocumentInstance application = createDocument("DOC-APP-001", "application", "application-doc", BigDecimal.valueOf(300), 1L, "PAYING");
        ProcessDocumentInstance contract = createDocument("DOC-CON-001", "contract", "contract-doc", BigDecimal.valueOf(5000), 1L, "PAYMENT_COMPLETED");
        ProcessDocumentInstance loan = createDocument("DOC-LOAN-001", "loan", "loan-doc", BigDecimal.valueOf(800), 1L, "PAYMENT_FINISHED");
        ProcessDocumentInstance approvedReport = createDocument("DOC-REPORT-002", "report", "approved-report", BigDecimal.valueOf(520), 1L, "APPROVED");
        ProcessDocumentInstance completedLoan = createDocument("DOC-LOAN-002", "loan", "completed-loan", BigDecimal.valueOf(900), 1L, "COMPLETED");
        ProcessDocumentInstance otherUserReport = createDocument("DOC-REPORT-003", "report", "other-user-report", BigDecimal.valueOf(260), 2L, "PAYMENT_FINISHED");
        ProcessDocumentExpenseDetail prepayDetail = new ProcessDocumentExpenseDetail();
        prepayDetail.setDocumentCode("DOC-REPORT-001");
        prepayDetail.setBusinessSceneMode("PREPAY_UNBILLED");
        prepayDetail.setActualPaymentAmount(BigDecimal.valueOf(300));
        prepayDetail.setFormDataJson("{\"actualPaymentAmount\":300}");

        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(
                report,
                application,
                contract,
                loan,
                approvedReport,
                completedLoan,
                otherUserReport
        ));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of(prepayDetail));
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of());

        var related = service.getDocumentPicker(1L, "RELATED", null, null, 1, 10, null, true);
        var writeoff = service.getDocumentPicker(1L, "WRITEOFF", null, null, 1, 10, null, true);

        assertEquals(List.of("report", "application", "contract", "loan"),
                related.getGroups().stream().map(item -> item.getTemplateType()).toList());
        assertEquals(List.of("report", "loan"),
                writeoff.getGroups().stream().map(item -> item.getTemplateType()).toList());
        assertEquals(List.of(1, 1, 1, 1),
                related.getGroups().stream().map(item -> item.getTotal()).toList());
        assertEquals(List.of(1, 1),
                writeoff.getGroups().stream().map(item -> item.getTotal()).toList());
        assertEquals(List.of("DOC-REPORT-001", "DOC-APP-001", "DOC-CON-001", "DOC-LOAN-001"),
                related.getGroups().stream()
                        .flatMap(group -> group.getItems().stream())
                        .map(item -> item.getDocumentCode())
                        .toList());
        assertEquals(0, BigDecimal.valueOf(300).compareTo(writeoff.getGroups().get(0).getItems().get(0).getAvailableWriteOffAmount()));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(writeoff.getGroups().get(1).getItems().get(0).getAvailableWriteOffAmount()));
    }

    @Test
    void getDashboardWriteOffSourceReportPickerFiltersBoundAndZeroBalanceSources() {
        ProcessDocumentInstance target = createApprovedDocument("DOC-TARGET-001", "loan", "loan-target", BigDecimal.valueOf(400), 1L);
        ProcessDocumentInstance availableReport = createApprovedDocument("DOC-REPORT-001", "report", "available-report", BigDecimal.valueOf(500), 1L);
        ProcessDocumentInstance boundReport = createApprovedDocument("DOC-REPORT-002", "report", "bound-report", BigDecimal.valueOf(600), 1L);
        ProcessDocumentInstance zeroReport = createApprovedDocument("DOC-REPORT-003", "report", "zero-report", BigDecimal.valueOf(400), 1L);
        ProcessDocumentWriteOff zeroBalance = createEffectiveWriteOff("DOC-REPORT-003", "DOC-OTHER-001", BigDecimal.valueOf(400));
        ProcessDocumentWriteOff bound = createEffectiveWriteOff("DOC-REPORT-002", "DOC-TARGET-001", BigDecimal.valueOf(50));

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(target);
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(availableReport, boundReport, zeroReport));
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(zeroBalance), List.of(bound));

        var result = service.getDashboardWriteOffSourceReportPicker(1L, "DOC-TARGET-001", null, 1, 10);

        assertEquals(1, result.getGroups().size());
        assertEquals(1, result.getGroups().get(0).getItems().size());
        assertEquals("DOC-REPORT-001", result.getGroups().get(0).getItems().get(0).getDocumentCode());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getGroups().get(0).getItems().get(0).getAvailableWriteOffAmount()));
    }

    @Test
    void bindDashboardWriteOffPersistsEffectiveRecord() {
        ProcessDocumentInstance target = createApprovedDocument("DOC-TARGET-001", "loan", "loan-target", BigDecimal.valueOf(300), 1L);
        ProcessDocumentInstance sourceReport = createApprovedDocument("DOC-REPORT-001", "report", "source-report", BigDecimal.valueOf(200), 1L);

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(target, sourceReport);
        when(processDocumentWriteOffMapper.selectCount(any())).thenReturn(0L);
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentWriteOffMapper.insert(any(ProcessDocumentWriteOff.class))).thenReturn(1);

        boolean bound = service.bindDashboardWriteOff(1L, "DOC-TARGET-001", "DOC-REPORT-001");

        assertEquals(true, bound);
        ArgumentCaptor<ProcessDocumentWriteOff> captor = ArgumentCaptor.forClass(ProcessDocumentWriteOff.class);
        verify(processDocumentWriteOffMapper).insert(captor.capture());
        assertEquals("DOC-REPORT-001", captor.getValue().getSourceDocumentCode());
        assertEquals("DOC-TARGET-001", captor.getValue().getTargetDocumentCode());
        assertEquals("LOAN", captor.getValue().getWriteoffSourceKind());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(captor.getValue().getEffectiveAmount()));
        assertEquals("EFFECTIVE", captor.getValue().getStatus());
    }

    @Test
    void syncDocumentBusinessRelationsPersistsRelatedAndWriteoffBusinessBindings() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("relatedDocs", "related-document", List.of("report", "application", "contract", "loan")),
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("report", "loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("relatedDocs", List.of(Map.of(
                "documentCode", "DOC-APP-001",
                "documentTitle", "application-doc"
        )));
        formData.put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-LOAN-001",
                "documentTitle", "loan-doc",
                "writeOffAmount", 120
        )));
        ProcessDocumentInstance source = createApprovedDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(600), 2L);
        ProcessDocumentInstance relatedTarget = createDocument("DOC-APP-001", "application", "application-doc", BigDecimal.valueOf(300), 2L, "PENDING_PAYMENT");
        ProcessDocumentInstance writeoffTarget = createDocument("DOC-LOAN-001", "loan", "loan-doc", BigDecimal.valueOf(500), 2L, "PAYMENT_FINISHED");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(relatedTarget, writeoffTarget));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentRelationMapper.insert(any(ProcessDocumentRelation.class))).thenReturn(1);
        when(processDocumentWriteOffMapper.insert(any(ProcessDocumentWriteOff.class))).thenReturn(1);

        service.syncDocumentBusinessRelations("DOC-SOURCE-001", formDesign, formData);

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
    void syncDocumentBusinessRelationsRejectsWriteoffTargetOutsideAllowedTemplateTypes() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-REPORT-001",
                "writeOffAmount", 88
        )));
        ProcessDocumentInstance source = createApprovedDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(260), 2L);
        ProcessDocumentInstance reportTarget = createDocument("DOC-REPORT-001", "report", "report-doc", BigDecimal.valueOf(200), 2L, "PAYMENT_COMPLETED");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(reportTarget));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.syncDocumentBusinessRelations("DOC-SOURCE-001", formDesign, formData)
        );

        assertEquals("\u6838\u9500\u5355\u636e\u7c7b\u578b\u4e0d\u5728\u5f53\u524d\u7ec4\u4ef6\u5141\u8bb8\u8303\u56f4\u5185", exception.getMessage());
        verify(processDocumentWriteOffMapper, never()).insert(any(ProcessDocumentWriteOff.class));
    }

    @Test
    void syncDocumentBusinessRelationsRejectsRelatedTargetSubmittedByAnotherUser() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("relatedDocs", "related-document", List.of("report", "application", "contract", "loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("relatedDocs", List.of(Map.of(
                "documentCode", "DOC-APP-001",
                "documentTitle", "application-doc"
        )));
        ProcessDocumentInstance source = createApprovedDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(300), 1L);
        ProcessDocumentInstance relatedTarget = createDocument("DOC-APP-001", "application", "application-doc", BigDecimal.valueOf(200), 2L, "PAYMENT_FINISHED");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(relatedTarget));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.syncDocumentBusinessRelations("DOC-SOURCE-001", formDesign, formData)
        );

        assertEquals("\u4ec5\u53ef\u5173\u8054\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e", exception.getMessage());
        verify(processDocumentRelationMapper, never()).insert(any(ProcessDocumentRelation.class));
    }

    @Test
    void syncDocumentBusinessRelationsRejectsWriteoffTargetOutsidePayableStatuses() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-LOAN-001",
                "writeOffAmount", 88
        )));
        ProcessDocumentInstance source = createApprovedDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(260), 2L);
        ProcessDocumentInstance writeoffTarget = createDocument("DOC-LOAN-001", "loan", "loan-doc", BigDecimal.valueOf(200), 2L, "APPROVED");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(writeoffTarget));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.syncDocumentBusinessRelations("DOC-SOURCE-001", formDesign, formData)
        );

        assertEquals("\u4ec5\u53ef\u9009\u62e9\u672c\u4eba\u5f85\u652f\u4ed8\u3001\u652f\u4ed8\u4e2d\u3001\u5df2\u652f\u4ed8\u6216\u5df2\u5b8c\u6210\u7684\u5355\u636e\u8fdb\u884c\u6838\u9500", exception.getMessage());
        verify(processDocumentWriteOffMapper, never()).insert(any(ProcessDocumentWriteOff.class));
    }

    @Test
    void finalizeEffectiveWriteOffsPromotesPendingRecords() {
        ProcessDocumentWriteOff pending = new ProcessDocumentWriteOff();
        pending.setId(1L);
        pending.setSourceDocumentCode("DOC-SOURCE-001");
        pending.setTargetDocumentCode("DOC-LOAN-001");
        pending.setRequestedAmount(BigDecimal.valueOf(120));
        pending.setStatus("PENDING_EFFECTIVE");
        pending.setSortOrder(1);
        ProcessDocumentInstance target = createApprovedDocument("DOC-LOAN-001", "loan", "loan-doc", BigDecimal.valueOf(500), 2L);

        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(pending), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(target));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        service.finalizeEffectiveWriteOffs("DOC-SOURCE-001");

        assertEquals("EFFECTIVE", pending.getStatus());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(pending.getEffectiveAmount()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(pending.getAvailableSnapshotAmount()));
        verify(processDocumentWriteOffMapper).updateById(pending);
    }

    @Test
    void voidPendingWriteOffsMarksPendingRowsVoid() {
        ProcessDocumentWriteOff pending = new ProcessDocumentWriteOff();
        pending.setId(1L);
        pending.setSourceDocumentCode("DOC-SOURCE-001");
        pending.setStatus("PENDING_EFFECTIVE");

        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(pending));

        service.voidPendingWriteOffs("DOC-SOURCE-001");

        assertEquals("VOID", pending.getStatus());
        verify(processDocumentWriteOffMapper).updateById(pending);
    }

    @Test
    void loadRelatedDocumentBindingsReturnsOutboundAndInboundRecords() {
        ProcessDocumentRelation outbound = new ProcessDocumentRelation();
        outbound.setSourceDocumentCode("DOC-CURRENT");
        outbound.setSourceFieldKey("relatedDocs");
        outbound.setTargetDocumentCode("DOC-TARGET");
        outbound.setTargetTemplateType("application");
        outbound.setStatus("ACTIVE");
        outbound.setSortOrder(1);
        ProcessDocumentRelation inbound = new ProcessDocumentRelation();
        inbound.setSourceDocumentCode("DOC-SOURCE");
        inbound.setSourceFieldKey("relatedDocs");
        inbound.setTargetDocumentCode("DOC-CURRENT");
        inbound.setStatus("ACTIVE");
        inbound.setSortOrder(2);
        ProcessDocumentInstance target = createApprovedDocument("DOC-TARGET", "application", "目标单据", BigDecimal.valueOf(300), 9L);
        target.setSubmitterName("目标提单人");
        ProcessDocumentInstance source = createApprovedDocument("DOC-SOURCE", "report", "来源单据", BigDecimal.valueOf(260), 10L);
        source.setSubmitterName("来源提单人");

        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of(outbound), List.of(inbound));
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(target, source));

        var bindings = service.loadRelatedDocumentBindings("DOC-CURRENT");

        assertEquals(2, bindings.size());
        assertEquals("OUTBOUND", bindings.get(0).getDirection());
        assertEquals("DOC-TARGET", bindings.get(0).getDocumentCode());
        assertEquals("目标单据", bindings.get(0).getDocumentTitle());
        assertEquals("申请单", bindings.get(0).getTemplateTypeLabel());
        assertEquals("已完成", bindings.get(0).getStatusLabel());
        assertEquals("目标提单人", bindings.get(0).getSubmitterName());
        assertEquals("INBOUND", bindings.get(1).getDirection());
        assertEquals("DOC-SOURCE", bindings.get(1).getDocumentCode());
        assertEquals("来源单据", bindings.get(1).getDocumentTitle());
        assertEquals("报销单", bindings.get(1).getTemplateTypeLabel());
        assertEquals("来源提单人", bindings.get(1).getSubmitterName());
    }

    @Test
    void loadWriteOffDocumentBindingsReturnsDirectionalAmountsAndStatuses() {
        ProcessDocumentWriteOff outboundPending = new ProcessDocumentWriteOff();
        outboundPending.setSourceDocumentCode("DOC-CURRENT");
        outboundPending.setSourceFieldKey("writeoffDocs");
        outboundPending.setTargetDocumentCode("DOC-LOAN");
        outboundPending.setTargetTemplateType("loan");
        outboundPending.setWriteoffSourceKind("LOAN");
        outboundPending.setRequestedAmount(BigDecimal.valueOf(120));
        outboundPending.setEffectiveAmount(null);
        outboundPending.setRemainingSnapshotAmount(BigDecimal.valueOf(380));
        outboundPending.setStatus("PENDING_EFFECTIVE");
        outboundPending.setSortOrder(1);
        ProcessDocumentWriteOff inboundEffective = new ProcessDocumentWriteOff();
        inboundEffective.setSourceDocumentCode("DOC-REPORT");
        inboundEffective.setSourceFieldKey("writeoffDocs");
        inboundEffective.setTargetDocumentCode("DOC-CURRENT");
        inboundEffective.setWriteoffSourceKind("PREPAY_REPORT");
        inboundEffective.setRequestedAmount(BigDecimal.valueOf(66));
        inboundEffective.setEffectiveAmount(BigDecimal.valueOf(66));
        inboundEffective.setRemainingSnapshotAmount(BigDecimal.valueOf(134));
        inboundEffective.setStatus("EFFECTIVE");
        inboundEffective.setSortOrder(2);
        ProcessDocumentWriteOff inboundVoid = new ProcessDocumentWriteOff();
        inboundVoid.setSourceDocumentCode("DOC-VOID");
        inboundVoid.setSourceFieldKey("writeoffDocs");
        inboundVoid.setTargetDocumentCode("DOC-CURRENT");
        inboundVoid.setWriteoffSourceKind("LOAN");
        inboundVoid.setRequestedAmount(BigDecimal.valueOf(30));
        inboundVoid.setEffectiveAmount(BigDecimal.ZERO);
        inboundVoid.setRemainingSnapshotAmount(BigDecimal.valueOf(70));
        inboundVoid.setStatus("VOID");
        inboundVoid.setSortOrder(3);
        ProcessDocumentInstance loan = createApprovedDocument("DOC-LOAN", "loan", "借款单据", BigDecimal.valueOf(500), 11L);
        loan.setSubmitterName("借款人");
        ProcessDocumentInstance report = createApprovedDocument("DOC-REPORT", "report", "报销来源", BigDecimal.valueOf(200), 12L);
        report.setSubmitterName("报销人");
        ProcessDocumentInstance voidSource = createApprovedDocument("DOC-VOID", "report", "作废来源", BigDecimal.valueOf(100), 13L);
        voidSource.setSubmitterName("作废人");

        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(
                List.of(outboundPending),
                List.of(inboundEffective, inboundVoid)
        );
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(loan, report, voidSource));

        var bindings = service.loadWriteOffDocumentBindings("DOC-CURRENT");

        assertEquals(3, bindings.size());
        assertEquals("OUTBOUND", bindings.get(0).getDirection());
        assertEquals("DOC-LOAN", bindings.get(0).getDocumentCode());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(bindings.get(0).getRequestedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(bindings.get(0).getEffectiveAmount()));
        assertEquals("待生效", bindings.get(0).getEffectiveStatusLabel());
        assertEquals("INBOUND", bindings.get(1).getDirection());
        assertEquals("DOC-REPORT", bindings.get(1).getDocumentCode());
        assertEquals(0, BigDecimal.valueOf(66).compareTo(bindings.get(1).getEffectiveAmount()));
        assertEquals("已生效", bindings.get(1).getEffectiveStatusLabel());
        assertEquals("INBOUND", bindings.get(2).getDirection());
        assertEquals("DOC-VOID", bindings.get(2).getDocumentCode());
        assertEquals("已作废", bindings.get(2).getEffectiveStatusLabel());
    }

    private ProcessFormDesign createFormDesignWithBlocks(List<Map<String, Object>> blocks) throws Exception {
        ProcessFormDesign formDesign = new ProcessFormDesign();
        formDesign.setFormCode("FORM-001");
        formDesign.setFormName("form");
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

    private ProcessDocumentInstance createApprovedDocument(
            String documentCode,
            String templateType,
            String documentTitle,
            BigDecimal totalAmount,
            Long submitterUserId
    ) {
        return createDocument(documentCode, templateType, documentTitle, totalAmount, submitterUserId, "COMPLETED");
    }

    private ProcessDocumentInstance createDocument(
            String documentCode,
            String templateType,
            String documentTitle,
            BigDecimal totalAmount,
            Long submitterUserId,
            String status
    ) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(documentCode);
        instance.setTemplateType(templateType);
        instance.setDocumentTitle(documentTitle);
        instance.setTemplateName(documentTitle + "-template");
        instance.setStatus(status);
        instance.setSubmitterUserId(submitterUserId);
        instance.setTotalAmount(totalAmount);
        instance.setFinishedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        instance.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        return instance;
    }

    private ProcessDocumentWriteOff createEffectiveWriteOff(String sourceDocumentCode, String targetDocumentCode, BigDecimal effectiveAmount) {
        ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
        writeOff.setSourceDocumentCode(sourceDocumentCode);
        writeOff.setTargetDocumentCode(targetDocumentCode);
        writeOff.setEffectiveAmount(effectiveAmount);
        writeOff.setStatus("EFFECTIVE");
        return writeOff;
    }
}
