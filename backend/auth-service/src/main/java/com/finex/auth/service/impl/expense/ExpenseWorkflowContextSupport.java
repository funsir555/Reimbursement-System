// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;

import java.util.List;
import java.util.Map;

/**
 * ExpenseWorkflowContextSupport：通用支撑类。
 * 封装 报销单这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
class ExpenseWorkflowContextSupport {

    private final AbstractExpenseWorkflowSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseWorkflowContextSupport(AbstractExpenseWorkflowSupport support) {
        this.support = support;
    }

    /**
     * 组装运行时流程上下文。
     */
    Map<String, Object> buildRuntimeFlowContext(
            User currentUser,
            ProcessDocumentTemplate template,
            ProcessFormDesign formDesign,
            Map<String, Object> formData,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        return support.buildRuntimeFlowContext(currentUser, template, formDesign, formData, expenseDetailDesign, expenseDetails);
    }

    /**
     * 组装运行时上下文ForInstance。
     */
    Map<String, Object> buildRuntimeContextForInstance(ProcessDocumentInstance instance) {
        return support.buildRuntimeContextForInstance(instance);
    }

    /**
     * 校验流程Snapshot。
     */
    void validateFlowSnapshot(String snapshotJson) {
        support.validateFlowSnapshot(snapshotJson);
    }

    RawFlowSnapshotSignature inspectRawFlowSnapshot(String snapshotJson) {
        return support.inspectRawFlowSnapshot(snapshotJson);
    }
}
