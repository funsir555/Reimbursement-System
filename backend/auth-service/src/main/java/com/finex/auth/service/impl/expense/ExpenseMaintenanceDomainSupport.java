// 业务域：报销单录入、流转与查询
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ExpenseMaintenanceDomainSupport：领域规则支撑类。
 * 承接 报销单维护的核心业务规则。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseMaintenanceDomainSupport {

    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";

    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    /**
     * 修复Misapproved单据按RootContainerBug。
     */
    public List<String> repairMisapprovedDocumentsByRootContainerBug() {
        List<ProcessDocumentInstance> approvedDocuments = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .orderByAsc(ProcessDocumentInstance::getId)
        );
        if (approvedDocuments.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> repairedDocumentCodes = new ArrayList<>();
        for (ProcessDocumentInstance instance : approvedDocuments) {
            RawFlowSnapshotSignature signature = expenseWorkflowRuntimeSupport.inspectRawFlowSnapshot(instance.getFlowSnapshotJson());
            if (!signature.hasApprovalNode() || !signature.hasBlankRootNode() || signature.hasNullRootNode()) {
                continue;
            }
            if (!expenseWorkflowRuntimeSupport.isMisapprovedByBlankRootBug(instance.getDocumentCode())) {
                continue;
            }
            expenseWorkflowRuntimeSupport.rebuildMisapprovedRuntime(instance);
            repairedDocumentCodes.add(instance.getDocumentCode());
        }
        return repairedDocumentCodes;
    }
}
