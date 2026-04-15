// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpenseSubmissionDomainSupport：领域规则支撑类。
 * 承接 报销单提交的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseSubmissionDomainSupport {

    private final ExpenseDocumentTemplateDomainSupport expenseDocumentTemplateDomainSupport;
    private final ExpenseDocumentMutationDomainSupport expenseDocumentMutationDomainSupport;

    /**
     * 查询可用模板列表。
     */
    public List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return expenseDocumentTemplateDomainSupport.listAvailableTemplates();
    }

    /**
     * 获取模板明细。
     */
    public ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return expenseDocumentTemplateDomainSupport.getTemplateDetail(userId, templateCode);
    }

    /**
     * 查询供应商选项。
     */
    public List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return expenseDocumentTemplateDomainSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    /**
     * 查询收款方选项。
     */
    public List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return expenseDocumentTemplateDomainSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    /**
     * 查询收款方账户选项。
     */
    public List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return expenseDocumentTemplateDomainSupport.listPayeeAccountOptions(
                userId,
                keyword,
                linkageMode,
                payeeName,
                counterpartyCode
        );
    }

    /**
     * 提交单据。
     */
    public ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return expenseDocumentMutationDomainSupport.submitDocument(userId, username, dto);
    }

    /**
     * 重新提交单据。
     */
    public ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return expenseDocumentMutationDomainSupport.resubmitDocument(userId, username, documentCode, dto);
    }
}
