package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseActionUserOptionVO;
import com.finex.auth.dto.ExpenseApprovalActionDTO;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentCommentDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentEditContextVO;
import com.finex.auth.dto.ExpenseDocumentNavigationVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentReminderDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.ExpenseTaskAddSignDTO;
import com.finex.auth.dto.ExpenseTaskTransferDTO;

import java.util.List;

public interface ExpenseDocumentService {

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates();

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode);

    List<ExpenseCreateVendorOptionVO> listVendorOptions(String keyword);

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(String keyword);

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(String keyword);

    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto);

    List<ExpenseSummaryVO> listExpenseSummaries(Long userId);

    List<ExpenseSummaryVO> listQueryDocumentSummaries(Long userId);

    ExpenseDocumentDetailVO getDocumentDetail(Long userId, String documentCode, boolean allowCrossView);

    ExpenseDetailInstanceDetailVO getExpenseDetail(Long userId, String documentCode, String detailNo, boolean allowCrossView);

    ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    );

    List<ExpenseApprovalPendingItemVO> listPendingApprovals(Long userId);

    ExpenseDocumentDetailVO recallDocument(Long userId, String username, String documentCode);

    ExpenseDocumentDetailVO commentOnDocument(Long userId, String username, String documentCode, ExpenseDocumentCommentDTO dto, boolean allowCrossView);

    ExpenseDocumentDetailVO remindDocument(Long userId, String username, String documentCode, ExpenseDocumentReminderDTO dto);

    ExpenseDocumentNavigationVO getDocumentNavigation(Long userId, String documentCode, boolean approvalViewer);

    ExpenseDocumentEditContextVO getDocumentEditContext(Long userId, String documentCode);

    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto);

    ExpenseDocumentDetailVO approveTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    ExpenseDocumentDetailVO rejectTask(Long userId, String username, Long taskId, ExpenseApprovalActionDTO dto);

    ExpenseDocumentEditContextVO getTaskModifyContext(Long userId, Long taskId);

    ExpenseDocumentDetailVO modifyTaskDocument(Long userId, String username, Long taskId, ExpenseDocumentUpdateDTO dto);

    ExpenseDocumentDetailVO transferTask(Long userId, String username, Long taskId, ExpenseTaskTransferDTO dto);

    ExpenseDocumentDetailVO addSignTask(Long userId, String username, Long taskId, ExpenseTaskAddSignDTO dto);

    List<ExpenseActionUserOptionVO> searchActionUsers(Long userId, String keyword);
}
