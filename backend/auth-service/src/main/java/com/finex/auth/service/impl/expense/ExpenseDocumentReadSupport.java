package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class ExpenseDocumentReadSupport {

    private final AbstractExpenseDocumentSupport support;

    ExpenseDocumentReadSupport(AbstractExpenseDocumentSupport support) {
        this.support = support;
    }

    ProcessDocumentInstance requireDocument(String documentCode) {
        return support.requireDocument(documentCode);
    }

    void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        support.requireSubmitter(instance, userId);
    }

    void assertCanViewDocument(ProcessDocumentInstance instance, Long userId, boolean allowCrossView) {
        support.assertCanViewDocument(instance, userId, allowCrossView);
    }

    Map<String, Object> readFormData(String json) {
        return support.readFormData(json);
    }

    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return support.loadExpenseDetails(documentCode);
    }

    ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        return support.requireExpenseDetail(documentCode, detailNo);
    }

    ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        return support.toRuntimeExpenseDetailDTO(detail);
    }

    ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        return support.buildDocumentDetail(instance);
    }
}
