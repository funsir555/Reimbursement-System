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

@Service
public class ExpenseDocumentMutationSupport {

    private final ExpenseDocumentReadSupport readSupport;
    private final ExpenseDocumentActionLogSupport actionLogSupport;
    private final ExpenseDocumentTemplateDomainSupport templateDomainSupport;
    private final ExpenseDocumentMutationDomainSupport mutationDomainSupport;

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

    List<ExpenseCreateTemplateSummaryVO> listAvailableTemplates() {
        return templateDomainSupport.listAvailableTemplates();
    }

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        return templateDomainSupport.getTemplateDetail(userId, templateCode);
    }

    List<ExpenseCreateVendorOptionVO> listVendorOptions(Long userId, String keyword, Boolean includeDisabled) {
        return templateDomainSupport.listVendorOptions(userId, keyword, includeDisabled);
    }

    List<ExpenseCreatePayeeOptionVO> listPayeeOptions(Long userId, String keyword, Boolean personalOnly) {
        return templateDomainSupport.listPayeeOptions(userId, keyword, personalOnly);
    }

    List<ExpenseCreatePayeeAccountOptionVO> listPayeeAccountOptions(
            Long userId,
            String keyword,
            String linkageMode,
            String payeeName,
            String counterpartyCode
    ) {
        return templateDomainSupport.listPayeeAccountOptions(userId, keyword, linkageMode, payeeName, counterpartyCode);
    }

    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        return mutationDomainSupport.submitDocument(userId, username, dto);
    }

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

    List<ProcessDocumentExpenseDetail> loadExpenseDetails(String documentCode) {
        return readSupport.loadExpenseDetails(documentCode);
    }

    ProcessDocumentExpenseDetail requireExpenseDetail(String documentCode, String detailNo) {
        return readSupport.requireExpenseDetail(documentCode, detailNo);
    }

    ExpenseDetailInstanceDTO toRuntimeExpenseDetailDTO(ProcessDocumentExpenseDetail detail) {
        return readSupport.toRuntimeExpenseDetailDTO(detail);
    }

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
