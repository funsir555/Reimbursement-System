package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.service.impl.expense.ExpenseQueryDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentQueryService {

    private final ExpenseQueryDomainSupport expenseQueryDomainSupport;

    public List<ExpenseSummaryVO> listExpenseSummaries(Long userId) {
        return expenseQueryDomainSupport.listExpenseSummaries(userId);
    }

    public List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId) {
        return expenseQueryDomainSupport.listQueryDocumentSummaries(userId);
    }

    public List<ExpenseSummaryVO> listOutstandingDocuments(Long userId, String kind) {
        return expenseQueryDomainSupport.listOutstandingDocuments(userId, kind);
    }

    public ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView) {
        return expenseQueryDomainSupport.getDocumentDetail(userId, documentCode, allowCrossView);
    }

    public ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView) {
        return expenseQueryDomainSupport.getExpenseDetail(userId, documentCode, detailNo, allowCrossView);
    }

    public ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode) {
        return expenseQueryDomainSupport.recallDocument(userId, username, documentCode);
    }

    public ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView) {
        return expenseQueryDomainSupport.commentOnDocument(userId, username, documentCode, dto, allowCrossView);
    }

    public ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto) {
        return expenseQueryDomainSupport.remindDocument(userId, username, documentCode, dto);
    }

    public ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer) {
        return expenseQueryDomainSupport.getDocumentNavigation(userId, documentCode, approvalViewer);
    }

    public ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode) {
        return expenseQueryDomainSupport.getDocumentEditContext(userId, documentCode);
    }
}
