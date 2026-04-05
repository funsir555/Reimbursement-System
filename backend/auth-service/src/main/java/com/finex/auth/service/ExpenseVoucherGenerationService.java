package com.finex.auth.service;

import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordVO;
import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherPushDocumentVO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingSaveDTO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingVO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;

public interface ExpenseVoucherGenerationService {

    ExpenseVoucherGenerationMetaVO getMeta(Long currentUserId);

    ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> getTemplatePolicies(
            String companyId,
            String templateCode,
            Integer enabled,
            Integer page,
            Integer pageSize
    );

    ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> getSubjectMappings(
            String companyId,
            String templateCode,
            String expenseTypeCode,
            Integer enabled,
            Integer page,
            Integer pageSize
    );

    ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername);

    ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername);

    ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername);

    ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername);

    ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO> getPushDocuments(
            String companyId,
            String templateCode,
            String keyword,
            String pushStatus,
            String dateFrom,
            String dateTo,
            Integer page,
            Integer pageSize
    );

    ExpenseVoucherPushBatchResultVO pushDocuments(ExpenseVoucherPushDTO dto, Long currentUserId, String currentUsername);

    ExpenseVoucherPageVO<ExpenseVoucherGeneratedRecordVO> getGeneratedVouchers(
            String companyId,
            String templateCode,
            String documentCode,
            String voucherNo,
            String pushStatus,
            String dateFrom,
            String dateTo,
            Integer page,
            Integer pageSize
    );

    ExpenseVoucherGeneratedRecordDetailVO getGeneratedVoucherDetail(Long id);
}
