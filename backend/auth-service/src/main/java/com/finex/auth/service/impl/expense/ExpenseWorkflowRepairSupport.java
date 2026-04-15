// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentInstance;

/**
 * ExpenseWorkflowRepairSupport：通用支撑类。
 * 封装 报销单修复这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
class ExpenseWorkflowRepairSupport {

    private final AbstractExpenseWorkflowSupport support;
    private final ExpenseWorkflowExecutionSupport executionSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseWorkflowRepairSupport(AbstractExpenseWorkflowSupport support, ExpenseWorkflowExecutionSupport executionSupport) {
        this.support = support;
        this.executionSupport = executionSupport;
    }

    /**
     * 判断Misapproved按BlankRootBug是否成立。
     */
    boolean isMisapprovedByBlankRootBug(String documentCode) {
        return support.isMisapprovedByBlankRootBug(documentCode);
    }

    void rebuildMisapprovedRuntime(ProcessDocumentInstance instance) {
        support.rebuildMisapprovedRuntime(instance);
    }
}
