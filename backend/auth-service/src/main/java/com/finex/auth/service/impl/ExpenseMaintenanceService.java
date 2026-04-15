// 业务域：报销单录入、流转与查询
// 文件角色：业务支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl;

import com.finex.auth.service.impl.expense.ExpenseMaintenanceDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpenseMaintenanceService：业务支撑类。
 * 封装 报销单维护这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseMaintenanceService {

    private final ExpenseMaintenanceDomainSupport expenseMaintenanceDomainSupport;

    /**
     * 修复Misapproved单据按RootContainerBug。
     */
    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        return expenseMaintenanceDomainSupport.repairMisapprovedDocumentsByRootContainerBug();
    }
}
