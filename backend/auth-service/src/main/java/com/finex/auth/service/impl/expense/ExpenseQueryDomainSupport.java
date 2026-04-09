package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseQueryDomainSupport {

    private final ExpenseDocumentMutationSupport expenseDocumentMutationSupport;
    private final ExpenseDocumentTemplateSupport expenseDocumentTemplateSupport;

    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return expenseDocumentMutationSupport.listExpenseSummaries(userId);
    }

    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        return expenseDocumentMutationSupport.listQueryDocumentSummaries(userId);
    }

    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        return expenseDocumentMutationSupport.listOutstandingDocuments(userId, kind);
    }

    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        return expenseDocumentMutationSupport.getDocumentDetail(userId, documentCode, allowCrossView);
    }

    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseDocumentMutationSupport.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        return expenseDocumentMutationSupport.recallDocument(userId, username, documentCode);
    }

    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        return expenseDocumentMutationSupport.commentOnDocument(userId, username, documentCode, dto, allowCrossView);
    }

    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        return expenseDocumentMutationSupport.remindDocument(userId, username, documentCode, dto);
    }

    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        return expenseDocumentMutationSupport.getDocumentNavigation(userId, documentCode, approvalViewer);
    }

    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseDocumentTemplateSupport.getDocumentEditContext(userId, documentCode);
    }
}
