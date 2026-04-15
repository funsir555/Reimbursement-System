// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ExpenseDocumentReadSupport：通用支撑类。
 * 封装 报销单单据Read这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
class ExpenseDocumentReadSupport {

    private final AbstractExpenseDocumentSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseDocumentReadSupport(AbstractExpenseDocumentSupport support) {
        this.support = support;
    }

    ProcessDocumentInstance requireDocument(String documentCode) {
        return support.requireDocument(documentCode);
    }

    void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        support.requireSubmitter(instance, userId);
    }

    void assertCanViewDocument(ProcessDocumentInstance instance, Long userId, boolean allowCrossView) {
        support.assertCanViewDocument(instance, userId, allowCrossView);
    }

    Map<String, Object> readFormData(String json) {
        return support.readFormData(json);
    }

    /**
     * 加载报销单明细。
     */
    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return support.loadExpenseDetails(documentCode);
    }

    ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        return support.requireExpenseDetail(documentCode, detailNo);
    }

    ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        return support.toRuntimeExpenseDetailDTO(detail);
    }

    /**
     * 组装单据明细。
     */
    ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        return support.buildDocumentDetail(instance);
    }
}
