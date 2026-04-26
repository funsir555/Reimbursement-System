package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
class ExpenseDocumentMutationApplySupport {

    private static final String DOCUMENT_STATUS_DRAFT = "DRAFT";

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDocumentMetadataSupport expenseDocumentMetadataSupport;
    private final ExpenseDocumentTaskRuntimeSupport expenseDocumentTaskRuntimeSupport;
    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;

    AbstractExpenseDocumentSupport.DocumentMutationContext buildMutationContext(
            ProcessDocumentInstance instance,
            ExpenseDocumentUpdateDTO dto,
            boolean resetRuntime
    ) {
        ProcessDocumentTemplate template = support.requireTemplateForDocument(instance.getTemplateCode());
        ProcessFormDesign formDesign = support.loadFormDesign(template.getFormDesignCode());
        ProcessExpenseDetailDesign expenseDetailDesign = support.loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
        Map<String, Object> formData = dto == null || dto.getFormData() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(dto.getFormData());
        List<ExpenseDetailInstanceDTO> expenseDetails = support.normalizeExpenseDetails(
                dto == null ? Collections.emptyList() : dto.getExpenseDetails()
        );
        support.validateExpenseDetailSubmission(template, expenseDetailDesign, expenseDetails);
        String flowSnapshotJson = resetRuntime
                ? support.validateSubmitContext(template, formDesign, expenseDetailDesign, formData, expenseDetails)
                : null;
        User submitter = support.loadActiveUser(instance.getSubmitterUserId());
        Map<String, Object> runtimeContext = resetRuntime
                ? expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                        submitter,
                        template,
                        formDesign,
                        formData,
                        expenseDetailDesign,
                        expenseDetails
                )
                : Collections.emptyMap();
        if (resetRuntime) {
            runtimeContext.put("manualApproverSelections", support.normalizeManualApproverSelections(
                    dto == null ? null : dto.getManualApproverSelections()
            ));
            runtimeContext.putAll(support.resolveRejectRuntimeMetadata(instance));
        }
        support.validatePmNameLength(template.getTemplateName(), "\u5f53\u524d\u6a21\u677f\u540d\u79f0");
        support.validatePmNameLength(template.getFlowName(), "\u5f53\u524d\u6d41\u7a0b\u540d\u79f0");
        support.validatePmNameLength(instance.getSubmitterName(), "\u63d0\u4ea4\u4eba\u59d3\u540d");
        String documentTitle = expenseDocumentMetadataSupport.resolveDocumentTitle(template, formData, instance.getSubmitterName());
        return new AbstractExpenseDocumentSupport.DocumentMutationContext(
                template,
                formDesign,
                expenseDetailDesign,
                formData,
                expenseDetails,
                flowSnapshotJson,
                runtimeContext,
                documentTitle,
                expenseDocumentMetadataSupport.resolveDocumentReason(template, formData),
                support.resolveTotalAmount(formData, expenseDetails, template.getExpenseDetailModeDefault())
        );
    }

    void applyDocumentMutation(
            ProcessDocumentInstance instance,
            AbstractExpenseDocumentSupport.DocumentMutationContext context,
            boolean resetRuntime
    ) {
        LocalDateTime now = LocalDateTime.now();
        support.validatePmNameLength(context.template().getTemplateName(), "\u5f53\u524d\u6a21\u677f\u540d\u79f0");
        support.validatePmNameLength(context.template().getFlowName(), "\u5f53\u524d\u6d41\u7a0b\u540d\u79f0");
        support.validatePmTitleLength(context.documentTitle(), "\u5355\u636e\u6807\u9898");
        if (resetRuntime) {
            expenseDocumentTaskRuntimeSupport.cancelOpenTasks(
                    expenseDocumentTaskRuntimeSupport.loadOpenTasks(instance.getDocumentCode()),
                    null,
                    now
            );
            instance.setStatus(DOCUMENT_STATUS_DRAFT);
            instance.setCurrentNodeKey(null);
            instance.setCurrentNodeName(null);
            instance.setCurrentTaskType(null);
            instance.setFinishedAt(null);
            instance.setTemplateName(context.template().getTemplateName());
            instance.setTemplateType(context.template().getTemplateType());
            instance.setFormDesignCode(context.template().getFormDesignCode());
            instance.setApprovalFlowCode(context.template().getApprovalFlow());
            instance.setFlowName(context.template().getFlowName());
            instance.setTemplateSnapshotJson(support.writeJson(support.toTemplateSnapshot(context.template())));
            instance.setFormSchemaSnapshotJson(
                    context.formDesign() == null ? support.writeJson(support.defaultSchema()) : context.formDesign().getSchemaJson()
            );
            instance.setFlowSnapshotJson(
                    context.flowSnapshotJson() == null ? support.resolveFlowSnapshotJson(context.template()) : context.flowSnapshotJson()
            );
        }
        instance.setDocumentTitle(context.documentTitle());
        instance.setDocumentReason(context.documentReason());
        instance.setTotalAmount(context.totalAmount());
        instance.setFormDataJson(support.writeJson(context.formData()));
        instance.setUpdatedAt(now);
        processDocumentInstanceMapper.updateById(instance);
        if (resetRuntime) {
            support.persistDocumentRuntimeState(instance, instance.getStatus(), null, null, null, null, now);
        }
        replaceExpenseDetailInstances(
                instance.getDocumentCode(),
                context.template(),
                context.expenseDetailDesign(),
                context.expenseDetails()
        );
    }

    private void replaceExpenseDetailInstances(
            String documentCode,
            ProcessDocumentTemplate template,
            ProcessExpenseDetailDesign expenseDetailDesign,
            List<ExpenseDetailInstanceDTO> expenseDetails
    ) {
        processDocumentExpenseDetailMapper.delete(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .eq(ProcessDocumentExpenseDetail::getDocumentCode, documentCode)
        );
        support.saveExpenseDetailInstances(documentCode, template, expenseDetailDesign, expenseDetails);
    }
}
