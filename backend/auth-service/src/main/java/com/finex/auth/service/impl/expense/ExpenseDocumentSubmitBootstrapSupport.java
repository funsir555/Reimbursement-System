package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitResultVO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
class ExpenseDocumentSubmitBootstrapSupport {

    private static final String DOCUMENT_STATUS_DRAFT = "DRAFT";
    private static final String DOCUMENT_STATUS_PENDING = "PENDING_APPROVAL";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String LOG_SUBMIT = "SUBMIT";
    private static final String LOG_RESUBMIT = "RESUBMIT";

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDocumentMetadataSupport expenseDocumentMetadataSupport;
    private final ExpenseDocumentActionLogSupport expenseDocumentActionLogSupport;
    private final ExpenseDocumentMutationApplySupport mutationApplySupport;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final UserMapper userMapper;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    ExpenseDocumentSubmitResultVO submitDocument(Long userId, String username, ExpenseDocumentSubmitDTO dto) {
        String templateCode = dto == null ? null : dto.getTemplateCode();
        String stage = "load-template";
        String documentCode = null;
        int expenseDetailCount = 0;
        log.info("Expense submit stage={} templateCode={} userId={} detailCount={}", stage, templateCode, userId, expenseDetailCount);

        try {
            ProcessDocumentTemplate template = support.requireTemplate(templateCode);
            stage = "load-form-design";
            ProcessFormDesign formDesign = support.loadFormDesign(template.getFormDesignCode());
            stage = "load-expense-detail-design";
            ProcessExpenseDetailDesign expenseDetailDesign = support.loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
            Map<String, Object> formData = dto != null && dto.getFormData() != null
                    ? new LinkedHashMap<>(dto.getFormData())
                    : new LinkedHashMap<>();
            List<ExpenseDetailInstanceDTO> expenseDetails = support.normalizeExpenseDetails(dto == null ? null : dto.getExpenseDetails());
            expenseDetailCount = expenseDetails.size();
            stage = "validate-submit-context";
            String flowSnapshotJson = support.validateSubmitContext(template, formDesign, expenseDetailDesign, formData, expenseDetails);
            User currentUser = userId == null ? null : userMapper.selectById(userId);
            stage = "build-runtime-context";
            Map<String, Object> runtimeFlowContext = expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                    currentUser,
                    template,
                    formDesign,
                    formData,
                    expenseDetailDesign,
                    expenseDetails
            );
            runtimeFlowContext.put("manualApproverSelections", support.normalizeManualApproverSelections(
                    dto == null ? null : dto.getManualApproverSelections()
            ));
            String submitterDisplayName = support.resolveUserDisplayName(currentUser, username);
            support.validatePmNameLength(template.getTemplateName(), "\u5f53\u524d\u6a21\u677f\u540d\u79f0");
            support.validatePmNameLength(template.getFlowName(), "\u5f53\u524d\u6d41\u7a0b\u540d\u79f0");
            support.validatePmNameLength(submitterDisplayName, "\u63d0\u4ea4\u4eba\u59d3\u540d");
            String documentTitle = expenseDocumentMetadataSupport.resolveDocumentTitle(template, formData, username);

            ProcessDocumentInstance instance = new ProcessDocumentInstance();
            stage = "persist-document";
            log.info("Expense submit stage={} templateCode={} userId={} detailCount={}", stage, template.getTemplateCode(), userId, expenseDetailCount);
            instance.setDocumentCode(support.buildDocumentCode());
            documentCode = instance.getDocumentCode();
            instance.setTemplateCode(template.getTemplateCode());
            instance.setTemplateName(template.getTemplateName());
            instance.setTemplateType(template.getTemplateType());
            instance.setFormDesignCode(template.getFormDesignCode());
            instance.setApprovalFlowCode(template.getApprovalFlow());
            instance.setFlowName(template.getFlowName());
            instance.setSubmitterUserId(userId);
            instance.setSubmitterName(submitterDisplayName);
            instance.setDocumentTitle(documentTitle);
            instance.setDocumentReason(expenseDocumentMetadataSupport.resolveDocumentReason(template, formData));
            instance.setTotalAmount(support.resolveTotalAmount(formData, expenseDetails, template.getExpenseDetailModeDefault()));
            instance.setStatus(DOCUMENT_STATUS_PENDING);
            instance.setFormDataJson(support.writeJson(formData));
            instance.setTemplateSnapshotJson(support.writeJson(support.toTemplateSnapshot(template)));
            instance.setFormSchemaSnapshotJson(formDesign == null ? support.writeJson(support.defaultSchema()) : formDesign.getSchemaJson());
            instance.setFlowSnapshotJson(flowSnapshotJson);
            instance.setCreatedAt(LocalDateTime.now());
            instance.setUpdatedAt(LocalDateTime.now());
            processDocumentInstanceMapper.insert(instance);

            stage = "append-submit-log";
            expenseDocumentActionLogSupport.appendLog(
                    instance.getDocumentCode(),
                    null,
                    null,
                    LOG_SUBMIT,
                    userId,
                    submitterDisplayName,
                    null,
                    expenseDocumentMetadataSupport.buildSubmitPayload(template)
            );
            stage = "persist-expense-details";
            support.saveExpenseDetailInstances(instance.getDocumentCode(), template, expenseDetailDesign, expenseDetails);
            stage = "sync-document-relations";
            support.syncDocumentBusinessRelations(instance.getDocumentCode(), formDesign, formData);
            stage = "initialize-runtime";
            expenseWorkflowRuntimeSupport.initializeRuntime(instance, runtimeFlowContext);
            if (support.isEffectiveApprovedStatus(support.requireDocument(instance.getDocumentCode()).getStatus())) {
                support.finalizeEffectiveWriteOffs(instance.getDocumentCode());
            }

            ExpenseDocumentSubmitResultVO result = new ExpenseDocumentSubmitResultVO();
            result.setId(instance.getId());
            result.setDocumentCode(instance.getDocumentCode());
            result.setStatus(instance.getStatus());
            log.info(
                    "Expense submit stage=success templateCode={} userId={} detailCount={} documentCode={} status={}",
                    template.getTemplateCode(),
                    userId,
                    expenseDetailCount,
                    instance.getDocumentCode(),
                    instance.getStatus()
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "Expense submit failed stage={} templateCode={} userId={} detailCount={} documentCode={} cause={}",
                    stage,
                    templateCode,
                    userId,
                    expenseDetailCount,
                    documentCode,
                    ex.getClass().getSimpleName(),
                    ex
            );
            throw ex;
        }
    }

    ProcessDocumentInstance saveDraftDocument(Long userId, String documentCode, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentInstance instance = support.requireDocument(documentCode);
        support.requireSubmitter(instance, userId);
        if (!Objects.equals(support.trimToNull(instance.getStatus()), DOCUMENT_STATUS_DRAFT)) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u662f\u8349\u7a3f\u72b6\u6001");
        }
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation =
                mutationApplySupport.buildMutationContext(instance, dto, false);
        mutationApplySupport.applyDocumentMutation(instance, mutation, false);
        return support.requireDocument(documentCode);
    }

    ExpenseDocumentSubmitResultVO resubmitDocument(Long userId, String username, String documentCode, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentInstance instance = support.requireDocument(documentCode);
        support.requireSubmitter(instance, userId);
        String status = support.trimToNull(instance.getStatus());
        if (!Objects.equals(status, DOCUMENT_STATUS_DRAFT) && !Objects.equals(status, DOCUMENT_STATUS_REJECTED)) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u4e0d\u662f\u53ef\u91cd\u63d0\u72b6\u6001");
        }
        String submitterDisplayName = support.resolveUserDisplayName(userId, username);
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation =
                mutationApplySupport.buildMutationContext(instance, dto, true);
        instance.setSubmitterName(submitterDisplayName);
        mutationApplySupport.applyDocumentMutation(instance, mutation, true);
        expenseDocumentActionLogSupport.appendLog(
                instance.getDocumentCode(),
                null,
                null,
                LOG_RESUBMIT,
                userId,
                submitterDisplayName,
                null,
                Map.of("templateCode", instance.getTemplateCode(), "templateName", instance.getTemplateName())
        );
        support.syncDocumentBusinessRelations(instance.getDocumentCode(), mutation.formDesign(), mutation.formData());
        expenseWorkflowRuntimeSupport.initializeRuntime(instance, mutation.runtimeContext());
        if (support.isEffectiveApprovedStatus(support.requireDocument(instance.getDocumentCode()).getStatus())) {
            support.finalizeEffectiveWriteOffs(instance.getDocumentCode());
        }
        ExpenseDocumentSubmitResultVO result = new ExpenseDocumentSubmitResultVO();
        result.setId(instance.getId());
        result.setDocumentCode(instance.getDocumentCode());
        result.setStatus(instance.getStatus());
        return result;
    }
}
