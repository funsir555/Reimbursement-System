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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRelationBindingMutationSupportTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private ProcessDocumentRelationMapper processDocumentRelationMapper;
    @Mock
    private ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;

    private ObjectMapper objectMapper;
    private ExpenseRelationBindingMutationSupport mutationSupport;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ExpenseRelationWriteOffSupportContext context = new ExpenseRelationWriteOffSupportContext(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                objectMapper
        );
        mutationSupport = new ExpenseRelationBindingMutationSupport(context, new ExpenseWriteOffAmountSupport(context));
    }

    @Test
    void syncDocumentBusinessRelationsPersistsRelatedAndWriteoffBindings() throws Exception {
        ProcessFormDesign formDesign = createFormDesignWithBlocks(List.of(
                createBusinessComponentBlock("relatedDocs", "related-document", List.of("report", "application", "contract", "loan")),
                createBusinessComponentBlock("writeoffDocs", "writeoff-document", List.of("report", "loan"))
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("relatedDocs", List.of(Map.of("documentCode", "DOC-APP-001")));
        formData.put("writeoffDocs", List.of(Map.of("documentCode", "DOC-LOAN-001", "writeOffAmount", 120)));

        ProcessDocumentInstance source = createDocument("DOC-SOURCE-001", "report", BigDecimal.valueOf(600), 2L, "COMPLETED");
        ProcessDocumentInstance relatedTarget = createDocument("DOC-APP-001", "application", BigDecimal.valueOf(300), 2L, "PENDING_PAYMENT");
        ProcessDocumentInstance writeoffTarget = createDocument("DOC-LOAN-001", "loan", BigDecimal.valueOf(500), 2L, "PAYMENT_FINISHED");

        when(processDocumentInstanceMapper.selectOne(any())).thenReturn(source);
        when(processDocumentRelationMapper.selectList(any())).thenReturn(List.of());
        when(processDocumentWriteOffMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(processDocumentInstanceMapper.selectList(any())).thenReturn(List.of(relatedTarget, writeoffTarget));
        when(processDocumentExpenseDetailMapper.selectList(any())).thenReturn(List.of());

        mutationSupport.syncDocumentBusinessRelations("DOC-SOURCE-001", formDesign, formData);

        ArgumentCaptor<ProcessDocumentRelation> relationCaptor = ArgumentCaptor.forClass(ProcessDocumentRelation.class);
        verify(processDocumentRelationMapper).insert(relationCaptor.capture());
        assertEquals("relatedDocs", relationCaptor.getValue().getSourceFieldKey());
        assertEquals("DOC-APP-001", relationCaptor.getValue().getTargetDocumentCode());

        ArgumentCaptor<ProcessDocumentWriteOff> writeOffCaptor = ArgumentCaptor.forClass(ProcessDocumentWriteOff.class);
        verify(processDocumentWriteOffMapper).insert(writeOffCaptor.capture());
        assertEquals("writeoffDocs", writeOffCaptor.getValue().getSourceFieldKey());
        assertEquals("DOC-LOAN-001", writeOffCaptor.getValue().getTargetDocumentCode());
        assertEquals("PENDING_EFFECTIVE", writeOffCaptor.getValue().getStatus());
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

    private Map<String, Object> createBusinessComponentBlock(String fieldKey, String componentCode, List<String> allowedTemplateTypes) {
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
            BigDecimal totalAmount,
            Long submitterUserId,
            String status
    ) {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode(documentCode);
        instance.setTemplateType(templateType);
        instance.setDocumentTitle(documentCode);
        instance.setTemplateName(documentCode + "-template");
        instance.setStatus(status);
        instance.setSubmitterUserId(submitterUserId);
        instance.setTotalAmount(totalAmount);
        instance.setFinishedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        instance.setUpdatedAt(LocalDateTime.of(2026, 4, 8, 18, 0));
        return instance;
    }
}
