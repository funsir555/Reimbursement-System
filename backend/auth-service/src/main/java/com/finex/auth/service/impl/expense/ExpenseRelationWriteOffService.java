// 业务域：报销单录入、流转与查询
// 文件角色：业务支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentRelationBindingVO;
import com.finex.auth.dto.ExpenseDocumentWriteOffBindingVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ExpenseRelationWriteOffService：业务支撑类。
 * 封装 报销单关联写入Off这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
public class ExpenseRelationWriteOffService {

    private final ExpenseWriteOffAmountSupport amountSupport;
    private final ExpenseDocumentRelationQuerySupport querySupport;
    private final ExpenseRelationBindingMutationSupport mutationSupport;
    private final ExpenseDashboardWriteOffSupport dashboardWriteOffSupport;

    public ExpenseRelationWriteOffService(
            ProcessDocumentInstanceMapper processDocumentInstanceMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            ProcessDocumentRelationMapper processDocumentRelationMapper,
            ProcessDocumentWriteOffMapper processDocumentWriteOffMapper,
            ObjectMapper objectMapper
    ) {
        ExpenseRelationWriteOffSupportContext context = new ExpenseRelationWriteOffSupportContext(
                processDocumentInstanceMapper,
                processDocumentExpenseDetailMapper,
                processDocumentRelationMapper,
                processDocumentWriteOffMapper,
                objectMapper
        );
        this.amountSupport = new ExpenseWriteOffAmountSupport(context);
        this.querySupport = new ExpenseDocumentRelationQuerySupport(context, this.amountSupport);
        this.mutationSupport = new ExpenseRelationBindingMutationSupport(context, this.amountSupport);
        this.dashboardWriteOffSupport = new ExpenseDashboardWriteOffSupport(context, this.amountSupport);
    }

    /**
     * 组装OutstandingAmount映射。
     */
    public Map<String, BigDecimal> buildOutstandingAmountMap(List<ProcessDocumentInstance> instances, String kind) {
        return amountSupport.buildOutstandingAmountMap(instances, kind);
    }

    public List<ExpenseDocumentRelationBindingVO> loadRelatedDocumentBindings(String documentCode) {
        return querySupport.loadRelatedDocumentBindings(documentCode);
    }

    public List<ExpenseDocumentWriteOffBindingVO> loadWriteOffDocumentBindings(String documentCode) {
        return querySupport.loadWriteOffDocumentBindings(documentCode);
    }

    /**
     * 获取单据Picker。
     */
    public ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    ) {
        return querySupport.getDocumentPicker(userId, relationType, templateTypes, keyword, page, pageSize, excludeDocumentCode, allowCrossView);
    }

    /**
     * 获取首页看板写入OffSourceReportPicker。
     */
    public ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    ) {
        return querySupport.getDashboardWriteOffSourceReportPicker(userId, targetDocumentCode, keyword, page, pageSize);
    }

    /**
     * 处理报销单关联写入Off中的这一步。
     */
    public boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        return dashboardWriteOffSupport.bindDashboardWriteOff(userId, targetDocumentCode, sourceReportDocumentCode);
    }

    /**
     * 同步单据业务关联。
     */
    public void syncDocumentBusinessRelations(
            String documentCode,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        mutationSupport.syncDocumentBusinessRelations(documentCode, formDesign, formData);
    }

    /**
     * 处理报销单关联写入Off中的这一步。
     */
    public void finalizeEffectiveWriteOffs(String documentCode) {
        dashboardWriteOffSupport.finalizeEffectiveWriteOffs(documentCode);
    }

    /**
     * 处理报销单关联写入Off中的这一步。
     */
    public void voidActiveRelations(String documentCode) {
        mutationSupport.voidActiveRelations(documentCode);
    }

    /**
     * 处理报销单关联写入Off中的这一步。
     */
    public void voidPendingWriteOffs(String documentCode) {
        mutationSupport.voidPendingWriteOffs(documentCode);
    }

    /**
     * 加载PrepayReportAmount映射。
     */
    public Map<String, BigDecimal> loadPrepayReportAmountMap(List<String> documentCodes) {
        return amountSupport.loadPrepayReportAmountMap(documentCodes);
    }

    /**
     * 加载Effective写入OffAmount映射。
     */
    public Map<String, BigDecimal> loadEffectiveWriteOffAmountMap(List<String> targetDocumentCodes) {
        return amountSupport.loadEffectiveWriteOffAmountMap(targetDocumentCodes);
    }

    /**
     * 解析写入OffSourceKind。
     */
    public String resolveWriteOffSourceKind(
            ProcessDocumentInstance target,
            Map<String, BigDecimal> prepayAmountMap
    ) {
        return amountSupport.resolveWriteOffSourceKind(target, prepayAmountMap);
    }

    /**
     * 解析当前可用写入OffAmount。
     */
    public BigDecimal resolveCurrentAvailableWriteOffAmount(
            ProcessDocumentInstance target,
            String writeOffSourceKind,
            Map<String, BigDecimal> prepayAmountMap,
            Map<String, BigDecimal> effectiveAmountMap
    ) {
        return amountSupport.resolveCurrentAvailableWriteOffAmount(target, writeOffSourceKind, prepayAmountMap, effectiveAmountMap);
    }
}
