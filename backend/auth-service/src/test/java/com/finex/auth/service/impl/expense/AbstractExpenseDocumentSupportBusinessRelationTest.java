package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportBusinessRelationTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ObjectMapper objectMapper;
    private AbstractExpenseDocumentSupport support;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(support, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(support, "processDocumentInstanceMapper", processDocumentInstanceMapper);
        ReflectionTestUtils.setField(support, "processDocumentExpenseDetailMapper", processDocumentExpenseDetailMapper);
        ReflectionTestUtils.setField(support, "processDocumentRelationMapper", processDocumentRelationMapper);
        ReflectionTestUtils.setField(support, "processDocumentWriteOffMapper", processDocumentWriteOffMapper);
    }

    @Test
    void syncDocumentBusinessRelationsReusesExistingRelationForSameTarget() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("relatedDocs", "related-document", List.of("report", "application", "contract", "loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("relatedDocs", List.of(Map.of(
                "documentCode", "DOC-APP-001",
                "documentTitle", "application-doc"
        )));
        ProcessDocumentInstance source = createDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(300), 2L, "DRAFT");
        ProcessDocumentInstance target = createDocument("DOC-APP-001", "application", "application-doc", BigDecimal.valueOf(120), 2L, "PENDING_PAYMENT");
        ProcessDocumentRelation existingRelation = new ProcessDocumentRelation();
        existingRelation.setId(11L);
        existingRelation.setSourceDocumentCode("DOC-SOURCE-001");
        existingRelation.setSourceFieldKey("relatedDocs");
        existingRelation.setTargetDocumentCode("DOC-APP-001");
        existingRelation.setTargetTemplateType("application");
        existingRelation.setStatus("VOID");
        existingRelation.setSortOrder(9);
        existingRelation.setCreatedAt(LocalDateTime.of(2026, 4, 23, 14, 1, 22));

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of(existingRelation));
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(target));

        ReflectionTestUtils.invokeMethod(support, "syncDocumentBusinessRelations", "DOC-SOURCE-001", formDesign, formData);

        verify(processDocumentRelationMapper, never()).insert(any(ProcessDocumentRelation.class));
        verify(processDocumentRelationMapper).updateById(existingRelation);
        assertEquals("ACTIVE", existingRelation.getStatus());
        assertEquals(1, existingRelation.getSortOrder());
        assertEquals("application", existingRelation.getTargetTemplateType());
    }

    @Test
    void syncDocumentBusinessRelationsReusesExistingWriteOffForSameTarget() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("writeoffDocs", List.of(Map.of(
                "documentCode", "DOC-LOAN-001",
                "documentTitle", "loan-doc",
                "writeOffAmount", 120
        )));
        ProcessDocumentInstance source = createDocument("DOC-SOURCE-001", "report", "source-doc", BigDecimal.valueOf(300), 2L, "DRAFT");
        ProcessDocumentInstance target = createDocument("DOC-LOAN-001", "loan", "loan-doc", BigDecimal.valueOf(500), 2L, "PAYMENT_FINISHED");
        ProcessDocumentWriteOff existingWriteOff = new ProcessDocumentWriteOff();
        existingWriteOff.setId(21L);
        existingWriteOff.setSourceDocumentCode("DOC-SOURCE-001");
        existingWriteOff.setSourceFieldKey("writeoffDocs");
        existingWriteOff.setTargetDocumentCode("DOC-LOAN-001");
        existingWriteOff.setTargetTemplateType("loan");
        existingWriteOff.setWriteoffSourceKind("LOAN");
        existingWriteOff.setRequestedAmount(BigDecimal.valueOf(80));
        existingWriteOff.setEffectiveAmount(null);
        existingWriteOff.setAvailableSnapshotAmount(BigDecimal.valueOf(400));
        existingWriteOff.setRemainingSnapshotAmount(BigDecimal.valueOf(320));
        existingWriteOff.setStatus("VOID");
        existingWriteOff.setCreatedAt(LocalDateTime.of(2026, 4, 23, 14, 1, 22));

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(existingWriteOff), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(target));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(support, "syncDocumentBusinessRelations", "DOC-SOURCE-001", formDesign, formData);

        verify(processDocumentWriteOffMapper, never()).insert(any(ProcessDocumentWriteOff.class));
        verify(processDocumentWriteOffMapper).updateById(existingWriteOff);
        assertEquals("PENDING_EFFECTIVE", existingWriteOff.getStatus());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(existingWriteOff.getRequestedAmount()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(existingWriteOff.getAvailableSnapshotAmount()));
        assertEquals(0, BigDecimal.valueOf(380).compareTo(existingWriteOff.getRemainingSnapshotAmount()));
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
}
