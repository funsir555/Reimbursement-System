// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

/**
 * ExpenseDocumentMutationDomainSupport：领域规则支撑类。
 * 承接 报销单单据的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
class ExpenseDocumentMutationDomainSupport {

    private final AbstractExpenseDocumentSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseDocumentMutationDomainSupport(AbstractExpenseDocumentSupport support) {
        this.support = support;
    }

    /**
     * 提交单据。
     */
    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return support.submitDocument(userId, username, dto);
    }

    /**
     * 重新提交单据。
     */
    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return support.resubmitDocument(userId, username, documentCode, dto);
    }

    /**
     * 组装变更上下文。
     */
    AbstractExpenseDocumentSupport.DocumentMutationContext buildMutationContext(
            ProcessDocumentInstance instance,
            ExpenseDocumentUpdateDTO dto,
            boolean resetRuntime
    ) {
        return support.buildMutationContext(instance, dto, resetRuntime);
    }

    void applyDocumentMutation(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext context,
            boolean resetRuntime
    ) {
        support.applyDocumentMutation(instance, context, resetRuntime);
    }
}
