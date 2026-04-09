package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseApprovalDomainSupport {

    private final ExpenseDocumentMutationSupport expenseDocumentMutationSupport;

    public List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId) {
        return expenseDocumentMutationSupport.listPendingApprovals(userId);
    }

    public ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseDocumentMutationSupport.approveTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto) {
        return expenseDocumentMutationSupport.rejectTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId) {
        return expenseDocumentMutationSupport.getTaskModifyContext(userId, taskId);
    }

    public ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentMutationSupport.modifyTaskDocument(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto) {
        return expenseDocumentMutationSupport.transferTask(userId, username, taskId, dto);
    }

    public ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto) {
        return expenseDocumentMutationSupport.addSignTask(userId, username, taskId, dto);
    }

    public List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword) {
        return expenseDocumentMutationSupport.searchActionUsers(userId, keyword);
    }
}
