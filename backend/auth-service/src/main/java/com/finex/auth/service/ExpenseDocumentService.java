package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseSummaryVO;

import java.util.List;

public interface ExpenseDocumentService {

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates();

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode);

    List<ExpenseCreateVendorOptionVO> listVendorOptions(String keyword);

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(String keyword);

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(String keyword);

    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto);

    List<ExpenseSummaryVO> listExpenseSummaries(Long userId);

    ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView);

    List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId);

    ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);
}
