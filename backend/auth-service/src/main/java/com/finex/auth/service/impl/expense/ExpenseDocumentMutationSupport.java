// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreatePayeeAccountOptionVO;
import com.finex.auth.dto.ExpenseCreatePayeeOptionVO;
import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.dto.ExpenseCreateTemplateSummaryVO;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ExpenseDocumentMutationSupport：通用支撑类。
 * 封装 报销单单据这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
public class ExpenseDocumentMutationSupport {

    private final ExpenseDocumentReadSupport readSupport;
    private final ExpenseDocumentActionLogSupport actionLogSupport;
    private final ExpenseDocumentTemplateDomainSupport templateDomainSupport;
    private final ExpenseDocumentMutationDomainSupport mutationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ExpenseDocumentMutationSupport(
            ExpenseDocumentReadSupport readSupport,
            ExpenseDocumentActionLogSupport actionLogSupport,
            ExpenseDocumentTemplateDomainSupport templateDomainSupport,
            ExpenseDocumentMutationDomainSupport mutationDomainSupport
    ) {
        this.readSupport = readSupport;
        this.actionLogSupport = actionLogSupport;
        this.templateDomainSupport = templateDomainSupport;
        this.mutationDomainSupport = mutationDomainSupport;
    }

    /**
     * 查询可用模板列表。
     */
    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return templateDomainSupport.listAvailableTemplates();
    }

    /**
     * 获取模板明细。
     */
    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return templateDomainSupport.getTemplateDetail(userId, templateCode);
    }

    /**
     * 查询供应商选项。
     */
    List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return templateDomainSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    /**
     * 查询收款方选项。
     */
    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return templateDomainSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    /**
     * 查询收款方账户选项。
     */
    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return templateDomainSupport.listPayeeAccountOptions(userId, keyword, linkageMode, payeeName, counterpartyCode);
    }

    /**
     * 提交单据。
     */
    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return mutationDomainSupport.submitDocument(userId, username, dto);
    }

    /**
     * 重新提交单据。
     */
    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        return mutationDomainSupport.resubmitDocument(userId, username, documentCode, dto);
    }

    ProcessDocumentInstance requireDocument(String documentCode) {
        return readSupport.requireDocument(documentCode);
    }

    void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        readSupport.requireSubmitter(instance, userId);
    }

    void assertCanViewDocument(ProcessDocumentInstance instance, Long userId, boolean allowCrossView) {
        readSupport.assertCanViewDocument(instance, userId, allowCrossView);
    }

    Map<String, Object> readFormData(String json) {
        return readSupport.readFormData(json);
    }

    /**
     * 加载报销单明细。
     */
    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return readSupport.loadExpenseDetails(documentCode);
    }

    ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        return readSupport.requireExpenseDetail(documentCode, detailNo);
    }

    ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        return readSupport.toRuntimeExpenseDetailDTO(detail);
    }

    /**
     * 组装单据明细。
     */
    ExpenseDocumentDetailVO buildDocumentDetail(ProcessDocumentInstance instance) {
        return readSupport.buildDocumentDetail(instance);
    }

    void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long operatorUserId,
            String operatorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        actionLogSupport.appendLog(documentCode, nodeKey, nodeName, actionType, operatorUserId, operatorName, actionComment, payload);
    }

    /**
     * 组装变更上下文。
     */
    AbstractExpenseDocumentSupport.DocumentMutationContext buildMutationContext(
            ProcessDocumentInstance instance,
            ExpenseDocumentUpdateDTO dto,
            boolean resetRuntime
    ) {
        return mutationDomainSupport.buildMutationContext(instance, dto, resetRuntime);
    }

    void applyDocumentMutation(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext context,
            boolean resetRuntime
    ) {
        mutationDomainSupport.applyDocumentMutation(instance, context, resetRuntime);
    }
}
