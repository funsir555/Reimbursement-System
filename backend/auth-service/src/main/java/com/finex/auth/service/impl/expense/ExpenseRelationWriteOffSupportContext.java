package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;

final class ExpenseRelationWriteOffSupportContext {

    final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    final ProcessDocumentRelationMapper processDocumentRelationMapper;
    final ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    final ObjectMapper objectMapper;

    ExpenseRelationWriteOffSupportContext(
            ProcessDocumentInstanceMapper processDocumentInstanceMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            ProcessDocumentRelationMapper processDocumentRelationMapper,
            ProcessDocumentWriteOffMapper processDocumentWriteOffMapper,
            ObjectMapper objectMapper
    ) {
        this.processDocumentInstanceMapper = processDocumentInstanceMapper;
        this.processDocumentExpenseDetailMapper = processDocumentExpenseDetailMapper;
        this.processDocumentRelationMapper = processDocumentRelationMapper;
        this.processDocumentWriteOffMapper = processDocumentWriteOffMapper;
        this.objectMapper = objectMapper;
    }
}
