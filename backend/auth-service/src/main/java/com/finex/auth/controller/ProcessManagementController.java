package com.finex.auth.controller;

import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveStatusDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeStatusDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowStatusDTO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ProcessManagementService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/process-management")
@RequiredArgsConstructor
public class ProcessManagementController {

    private static final String PROCESS_VIEW = "expense:process_management:view";
    private static final String PROCESS_CREATE = "expense:process_management:create";
    private static final String PROCESS_EDIT = "expense:process_management:edit";
    private static final String PROCESS_DISABLE = "expense:process_management:disable";
    private static final String PROCESS_PUBLISH = "expense:process_management:publish";

    private final ProcessManagementService processManagementService;
    private final AccessControlService accessControlService;

    @GetMapping("/overview")
    public Result<ProcessCenterOverviewVO> overview(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getOverview());
    }

    @GetMapping("/template-types")
    public Result<List<ProcessTemplateTypeVO>> templateTypes(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getTemplateTypes());
    }

    @GetMapping("/form-options")
    public Result<ProcessTemplateFormOptionsVO> formOptions(
            @RequestParam String templateType,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFormOptions(templateType));
    }

    @GetMapping("/templates/{id}")
    public Result<ProcessTemplateDetailVO> templateDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getTemplateDetail(id));
    }

    @PostMapping("/templates")
    public Result<ProcessTemplateSaveResultVO> createTemplate(
            @Valid @RequestBody ProcessTemplateSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success(
                "模板保存成功",
                processManagementService.saveTemplate(dto, getCurrentUsername(request))
        );
    }

    @PutMapping("/templates/{id}")
    public Result<ProcessTemplateSaveResultVO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ProcessTemplateSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success(
                "模板更新成功",
                processManagementService.updateTemplate(id, dto, getCurrentUsername(request))
        );
    }

    @DeleteMapping("/templates/{id}")
    public Result<Boolean> deleteTemplate(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("妯℃澘鍒犻櫎鎴愬姛", processManagementService.deleteTemplate(id));
    }

    @GetMapping("/custom-archives")
    public Result<List<ProcessCustomArchiveSummaryVO>> listCustomArchives(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listCustomArchives());
    }

    @GetMapping("/custom-archives/meta")
    public Result<ProcessCustomArchiveMetaVO> customArchiveMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getCustomArchiveMeta());
    }

    @GetMapping("/custom-archives/{id}")
    public Result<ProcessCustomArchiveDetailVO> customArchiveDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getCustomArchiveDetail(id));
    }

    @PostMapping("/custom-archives")
    public Result<ProcessCustomArchiveDetailVO> createCustomArchive(
            @Valid @RequestBody ProcessCustomArchiveSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("自定义档案保存成功", processManagementService.createCustomArchive(dto));
    }

    @PutMapping("/custom-archives/{id}")
    public Result<ProcessCustomArchiveDetailVO> updateCustomArchive(
            @PathVariable Long id,
            @Valid @RequestBody ProcessCustomArchiveSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("自定义档案更新成功", processManagementService.updateCustomArchive(id, dto));
    }

    @PatchMapping("/custom-archives/{id}/status")
    public Result<Boolean> updateCustomArchiveStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessCustomArchiveStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("自定义档案状态更新成功", processManagementService.updateCustomArchiveStatus(id, dto.getStatus()));
    }

    @DeleteMapping("/custom-archives/{id}")
    public Result<Boolean> deleteCustomArchive(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("自定义档案删除成功", processManagementService.deleteCustomArchive(id));
    }

    @PostMapping("/custom-archives/resolve")
    public Result<ProcessCustomArchiveResolveResultVO> resolveCustomArchive(
            @Valid @RequestBody ProcessCustomArchiveResolveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.resolveCustomArchive(dto));
    }

    @GetMapping("/expense-types/tree")
    public Result<List<ProcessExpenseTypeTreeVO>> listExpenseTypeTree(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listExpenseTypeTree());
    }

    @GetMapping("/expense-types/meta")
    public Result<ProcessExpenseTypeMetaVO> expenseTypeMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getExpenseTypeMeta());
    }

    @GetMapping("/expense-types/{id}")
    public Result<ProcessExpenseTypeDetailVO> expenseTypeDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getExpenseTypeDetail(id));
    }

    @PostMapping("/expense-types")
    public Result<ProcessExpenseTypeDetailVO> createExpenseType(
            @Valid @RequestBody ProcessExpenseTypeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("Expense type saved", processManagementService.createExpenseType(dto));
    }

    @PutMapping("/expense-types/{id}")
    public Result<ProcessExpenseTypeDetailVO> updateExpenseType(
            @PathVariable Long id,
            @Valid @RequestBody ProcessExpenseTypeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("Expense type updated", processManagementService.updateExpenseType(id, dto));
    }

    @PatchMapping("/expense-types/{id}/status")
    public Result<Boolean> updateExpenseTypeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessExpenseTypeStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("Expense type status updated", processManagementService.updateExpenseTypeStatus(id, dto.getStatus()));
    }

    @DeleteMapping("/expense-types/{id}")
    public Result<Boolean> deleteExpenseType(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("Expense type deleted", processManagementService.deleteExpenseType(id));
    }

    @GetMapping("/form-designs")
    public Result<List<ProcessFormDesignSummaryVO>> listFormDesigns(
            @RequestParam(required = false) String templateType,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listFormDesigns(templateType));
    }

    @GetMapping("/form-designs/{id}")
    public Result<ProcessFormDesignDetailVO> formDesignDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFormDesignDetail(id));
    }

    @PostMapping("/form-designs")
    public Result<ProcessFormDesignDetailVO> createFormDesign(
            @Valid @RequestBody ProcessFormDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("表单设计保存成功", processManagementService.createFormDesign(dto));
    }

    @PutMapping("/form-designs/{id}")
    public Result<ProcessFormDesignDetailVO> updateFormDesign(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFormDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("表单设计更新成功", processManagementService.updateFormDesign(id, dto));
    }

    @DeleteMapping("/form-designs/{id}")
    public Result<Boolean> deleteFormDesign(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("表单设计删除成功", processManagementService.deleteFormDesign(id));
    }

    @GetMapping("/flows")
    public Result<List<ProcessFlowSummaryVO>> listFlows(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listFlows());
    }

    @GetMapping("/flows/meta")
    public Result<ProcessFlowMetaVO> flowMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFlowMeta());
    }

    @GetMapping("/flows/{id}")
    public Result<ProcessFlowDetailVO> flowDetail(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFlowDetail(id));
    }

    @PostMapping("/flows")
    public Result<ProcessFlowDetailVO> createFlow(
            @Valid @RequestBody ProcessFlowSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("流程创建成功", processManagementService.createFlow(dto));
    }

    @PutMapping("/flows/{id}")
    public Result<ProcessFlowDetailVO> updateFlow(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFlowSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("流程保存成功", processManagementService.updateFlow(id, dto));
    }

    @PostMapping("/flows/{id}/publish")
    public Result<ProcessFlowDetailVO> publishFlow(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_CREATE, PROCESS_EDIT, PROCESS_PUBLISH);
        return Result.success("流程发布成功", processManagementService.publishFlow(id));
    }

    @PatchMapping("/flows/{id}/status")
    public Result<Boolean> updateFlowStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFlowStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("流程状态更新成功", processManagementService.updateFlowStatus(id, dto.getStatus()));
    }

    @PostMapping("/flow-scenes")
    public Result<ProcessFlowSceneVO> createFlowScene(
            @Valid @RequestBody ProcessFlowSceneSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("流程场景创建成功", processManagementService.createFlowScene(dto));
    }

    @PostMapping("/flows/resolve-approvers")
    public Result<ProcessFlowResolveApproversVO> resolveFlowApprovers(
            @Valid @RequestBody ProcessFlowResolveApproversDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.resolveFlowApprovers(dto));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("无法获取当前登录用户");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "流程管理员";
    }
}
