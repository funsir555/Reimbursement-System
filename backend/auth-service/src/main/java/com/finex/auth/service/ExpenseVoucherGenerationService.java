// 业务域：报销凭证生成与推送
// 文件角色：service 接口
// 上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。
// 风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。

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

/**
 * ExpenseVoucherGenerationService：service 接口。
 * 定义报销单凭证Generation这块对外提供的业务入口能力。
 * 改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。
 */
public interface ExpenseVoucherGenerationService {

    /**
     * 获取元数据。
     */
    ExpenseVoucherGenerationMetaVO getMeta(Long currentUserId);

    /**
     * 获取模板Policies。
     */
    ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> getTemplatePolicies(
            String companyId,
            String templateCode,
            Integer enabled,
            Integer page,
            Integer pageSize
    );

    /**
     * 获取科目映射。
     */
    ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> getSubjectMappings(
            String companyId,
            String templateCode,
            String expenseTypeCode,
            Integer enabled,
            Integer page,
            Integer pageSize
    );

    /**
     * 创建模板Policy。
     */
    ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername);

    /**
     * 更新模板Policy。
     */
    ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername);

    /**
     * 创建科目映射。
     */
    ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername);

    /**
     * 更新科目映射。
     */
    ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername);

    /**
     * 获取推送单据。
     */
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

    /**
     * 推送单据。
     */
    ExpenseVoucherPushBatchResultVO pushDocuments(ExpenseVoucherPushDTO dto, Long currentUserId, String currentUsername);

    /**
     * 获取Generated凭证。
     */
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

    /**
     * 获取Generated凭证明细。
     */
    ExpenseVoucherGeneratedRecordDetailVO getGeneratedVoucherDetail(Long id);
}
