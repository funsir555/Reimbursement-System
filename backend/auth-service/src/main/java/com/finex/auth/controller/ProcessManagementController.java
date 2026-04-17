// 这里是 ProcessManagementController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

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
import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
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
import com.finex.auth.interceptor.TemplateSaveTraceInterceptor;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ProcessManagementService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 这是 ProcessManagementController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@Slf4j
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

    // 处理 overview 请求。
    @GetMapping("/overview")
    public Result<ProcessCenterOverviewVO> overview(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getOverview());
    }

    // 处理 templateTypes 请求。
    @GetMapping("/template-types")
    public Result<List<ProcessTemplateTypeVO>> templateTypes(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getTemplateTypes());
    }

    // 处理 formOptions 请求。
    @GetMapping("/form-options")
    public Result<ProcessTemplateFormOptionsVO> formOptions(
            @RequestParam String templateType,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFormOptions(templateType));
    }

    // 处理 templateDetail 请求。
    @GetMapping("/templates/{id}")
    public Result<ProcessTemplateDetailVO> templateDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getTemplateDetail(id));
    }

    // 处理 createTemplate 请求。
    @PostMapping("/templates")
    public Result<ProcessTemplateSaveResultVO> createTemplate(
            @Valid @RequestBody ProcessTemplateSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success(
                "\u6a21\u677f\u5df2\u4fdd\u5b58",
                traceCreateTemplate(request, dto)
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
                "\u6a21\u677f\u5df2\u66f4\u65b0",
                traceUpdateTemplate(id, request, dto)
        );
    }

    @PostMapping("/templates/{id}/copy")
    public Result<ProcessTemplateSaveResultVO> copyTemplate(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success(
                "\u6a21\u677f\u526f\u672c\u5df2\u521b\u5efa",
                processManagementService.copyTemplate(id, getCurrentUsername(request))
        );
    }

    @DeleteMapping("/templates/{id}")
    public Result<Boolean> deleteTemplate(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("\u6a21\u677f\u5df2\u5220\u9664", processManagementService.deleteTemplate(id));
    }

    @GetMapping("/custom-archives")
    public Result<List<ProcessCustomArchiveSummaryVO>> listCustomArchives(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listCustomArchives());
    }

    // 处理 customArchiveMeta 请求。
    @GetMapping("/custom-archives/meta")
    public Result<ProcessCustomArchiveMetaVO> customArchiveMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getCustomArchiveMeta());
    }

    // 处理 customArchiveDetail 请求。
    @GetMapping("/custom-archives/{id}")
    public Result<ProcessCustomArchiveDetailVO> customArchiveDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getCustomArchiveDetail(id));
    }

    // 处理 createCustomArchive 请求。
    @PostMapping("/custom-archives")
    public Result<ProcessCustomArchiveDetailVO> createCustomArchive(
            @Valid @RequestBody ProcessCustomArchiveSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("鑷畾涔夋。妗堜繚瀛樻垚鍔?", processManagementService.createCustomArchive(dto));
    }

    // 处理 updateCustomArchive 请求。
    @PutMapping("/custom-archives/{id}")
    public Result<ProcessCustomArchiveDetailVO> updateCustomArchive(
            @PathVariable Long id,
            @Valid @RequestBody ProcessCustomArchiveSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("鑷畾涔夋。妗堟洿鏂版垚鍔?", processManagementService.updateCustomArchive(id, dto));
    }

    // 处理 updateCustomArchiveStatus 请求。
    @PatchMapping("/custom-archives/{id}/status")
    public Result<Boolean> updateCustomArchiveStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessCustomArchiveStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("鑷畾涔夋。妗堢姸鎬佹洿鏂版垚鍔?", processManagementService.updateCustomArchiveStatus(id, dto.getStatus()));
    }

    // 处理 deleteCustomArchive 请求。
    @DeleteMapping("/custom-archives/{id}")
    public Result<Boolean> deleteCustomArchive(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("鑷畾涔夋。妗堝垹闄ゆ垚鍔?", processManagementService.deleteCustomArchive(id));
    }

    // 处理 resolveCustomArchive 请求。
    @PostMapping("/custom-archives/resolve")
    public Result<ProcessCustomArchiveResolveResultVO> resolveCustomArchive(
            @Valid @RequestBody ProcessCustomArchiveResolveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.resolveCustomArchive(dto));
    }

    // 处理 listExpenseTypeTree 请求。
    @GetMapping("/expense-types/tree")
    public Result<List<ProcessExpenseTypeTreeVO>> listExpenseTypeTree(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listExpenseTypeTree());
    }

    // 处理 expenseTypeMeta 请求。
    @GetMapping("/expense-types/meta")
    public Result<ProcessExpenseTypeMetaVO> expenseTypeMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getExpenseTypeMeta());
    }

    // 处理 expenseTypeDetail 请求。
    @GetMapping("/expense-types/{id}")
    public Result<ProcessExpenseTypeDetailVO> expenseTypeDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getExpenseTypeDetail(id));
    }

    // 处理 createExpenseType 请求。
    @PostMapping("/expense-types")
    public Result<ProcessExpenseTypeDetailVO> createExpenseType(
            @Valid @RequestBody ProcessExpenseTypeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("Expense type saved", processManagementService.createExpenseType(dto));
    }

    // 处理 updateExpenseType 请求。
    @PutMapping("/expense-types/{id}")
    public Result<ProcessExpenseTypeDetailVO> updateExpenseType(
            @PathVariable Long id,
            @Valid @RequestBody ProcessExpenseTypeSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("Expense type updated", processManagementService.updateExpenseType(id, dto));
    }

    // 处理 updateExpenseTypeStatus 请求。
    @PatchMapping("/expense-types/{id}/status")
    public Result<Boolean> updateExpenseTypeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessExpenseTypeStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("Expense type status updated", processManagementService.updateExpenseTypeStatus(id, dto.getStatus()));
    }

    // 处理 deleteExpenseType 请求。
    @DeleteMapping("/expense-types/{id}")
    public Result<Boolean> deleteExpenseType(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("Expense type deleted", processManagementService.deleteExpenseType(id));
    }

    // 处理 listExpenseDetailDesigns 请求。
    @GetMapping("/expense-detail-designs")
    public Result<List<ProcessExpenseDetailDesignSummaryVO>> listExpenseDetailDesigns(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listExpenseDetailDesigns());
    }

    // 处理 expenseDetailDesignDetail 请求。
    @GetMapping("/expense-detail-designs/{id}")
    public Result<ProcessExpenseDetailDesignDetailVO> expenseDetailDesignDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getExpenseDetailDesignDetail(id));
    }

    // 处理 createExpenseDetailDesign 请求。
    @PostMapping("/expense-detail-designs")
    public Result<ProcessExpenseDetailDesignDetailVO> createExpenseDetailDesign(
            @Valid @RequestBody ProcessExpenseDetailDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("璐圭敤鏄庣粏琛ㄥ崟淇濆瓨鎴愬姛", processManagementService.createExpenseDetailDesign(dto));
    }

    // 处理 updateExpenseDetailDesign 请求。
    @PutMapping("/expense-detail-designs/{id}")
    public Result<ProcessExpenseDetailDesignDetailVO> updateExpenseDetailDesign(
            @PathVariable Long id,
            @Valid @RequestBody ProcessExpenseDetailDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("璐圭敤鏄庣粏琛ㄥ崟鏇存柊鎴愬姛", processManagementService.updateExpenseDetailDesign(id, dto));
    }

    // 处理 deleteExpenseDetailDesign 请求。
    @DeleteMapping("/expense-detail-designs/{id}")
    public Result<Boolean> deleteExpenseDetailDesign(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("璐圭敤鏄庣粏琛ㄥ崟鍒犻櫎鎴愬姛", processManagementService.deleteExpenseDetailDesign(id));
    }

    // 处理 listFormDesigns 请求。
    @GetMapping("/form-designs")
    public Result<List<ProcessFormDesignSummaryVO>> listFormDesigns(
            @RequestParam(required = false) String templateType,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listFormDesigns(templateType));
    }

    // 处理 formDesignDetail 请求。
    @GetMapping("/form-designs/{id}")
    public Result<ProcessFormDesignDetailVO> formDesignDetail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFormDesignDetail(id));
    }

    // 处理 createFormDesign 请求。
    @PostMapping("/form-designs")
    public Result<ProcessFormDesignDetailVO> createFormDesign(
            @Valid @RequestBody ProcessFormDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("琛ㄥ崟璁捐淇濆瓨鎴愬姛", processManagementService.createFormDesign(dto));
    }

    // 处理 updateFormDesign 请求。
    @PutMapping("/form-designs/{id}")
    public Result<ProcessFormDesignDetailVO> updateFormDesign(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFormDesignSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("琛ㄥ崟璁捐鏇存柊鎴愬姛", processManagementService.updateFormDesign(id, dto));
    }

    // 处理 deleteFormDesign 请求。
    @DeleteMapping("/form-designs/{id}")
    public Result<Boolean> deleteFormDesign(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("琛ㄥ崟璁捐鍒犻櫎鎴愬姛", processManagementService.deleteFormDesign(id));
    }

    // 处理 listFlows 请求。
    @GetMapping("/flows")
    public Result<List<ProcessFlowSummaryVO>> listFlows(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.listFlows());
    }

    // 处理 flowMeta 请求。
    @GetMapping("/flows/meta")
    public Result<ProcessFlowMetaVO> flowMeta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFlowMeta());
    }

    // 处理 flowDetail 请求。
    @GetMapping("/flows/{id}")
    public Result<ProcessFlowDetailVO> flowDetail(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_VIEW);
        return Result.success(processManagementService.getFlowDetail(id));
    }

    // 处理 createFlow 请求。
    @PostMapping("/flows")
    public Result<ProcessFlowDetailVO> createFlow(
            @Valid @RequestBody ProcessFlowSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("娴佺▼鍒涘缓鎴愬姛", processManagementService.createFlow(dto));
    }

    // 处理 updateFlow 请求。
    @PutMapping("/flows/{id}")
    public Result<ProcessFlowDetailVO> updateFlow(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFlowSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_EDIT);
        return Result.success("娴佺▼淇濆瓨鎴愬姛", processManagementService.updateFlow(id, dto));
    }

    // 处理 publishFlow 请求。
    @PostMapping("/flows/{id}/publish")
    public Result<ProcessFlowDetailVO> publishFlow(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_CREATE, PROCESS_EDIT, PROCESS_PUBLISH);
        return Result.success("娴佺▼鍙戝竷鎴愬姛", processManagementService.publishFlow(id));
    }

    // 处理 updateFlowStatus 请求。
    @PatchMapping("/flows/{id}/status")
    public Result<Boolean> updateFlowStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProcessFlowStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROCESS_DISABLE, PROCESS_PUBLISH);
        return Result.success("娴佺▼鐘舵€佹洿鏂版垚鍔?", processManagementService.updateFlowStatus(id, dto.getStatus()));
    }

    // 处理 createFlowScene 请求。
    @PostMapping("/flow-scenes")
    public Result<ProcessFlowSceneVO> createFlowScene(
            @Valid @RequestBody ProcessFlowSceneSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROCESS_CREATE);
        return Result.success("娴佺▼鍦烘櫙鍒涘缓鎴愬姛", processManagementService.createFlowScene(dto));
    }

    // 处理 resolveFlowApprovers 请求。
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
        throw new IllegalStateException("鏃犳硶鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "娴佺▼绠＄悊鍛?";
    }
    private ProcessTemplateSaveResultVO traceCreateTemplate(HttpServletRequest request, ProcessTemplateSaveDTO dto) {
        String traceId = resolveTemplateSaveTraceId(request);
        Long userId = getCurrentUserId(request);
        String username = getCurrentUsername(request);
        long startedAt = System.nanoTime();
        log.info(
                "[TemplateSaveTrace][{}][controller] createTemplate start userId={} username={} {}",
                traceId,
                userId,
                username,
                summarizeTemplatePayload(dto)
        );
        try {
            ProcessTemplateSaveResultVO result = processManagementService.saveTemplate(dto, username);
            log.info(
                    "[TemplateSaveTrace][{}][controller] createTemplate success templateCode={} costMs={}",
                    traceId,
                    result.getTemplateCode(),
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "[TemplateSaveTrace][{}][controller] createTemplate failed after {}ms: {}",
                    traceId,
                    elapsedMillis(startedAt),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private ProcessTemplateSaveResultVO traceUpdateTemplate(Long id, HttpServletRequest request, ProcessTemplateSaveDTO dto) {
        String traceId = resolveTemplateSaveTraceId(request);
        Long userId = getCurrentUserId(request);
        String username = getCurrentUsername(request);
        long startedAt = System.nanoTime();
        log.info(
                "[TemplateSaveTrace][{}][controller] updateTemplate start templateId={} userId={} username={} {}",
                traceId,
                id,
                userId,
                username,
                summarizeTemplatePayload(dto)
        );
        try {
            ProcessTemplateSaveResultVO result = processManagementService.updateTemplate(id, dto, username);
            log.info(
                    "[TemplateSaveTrace][{}][controller] updateTemplate success templateId={} templateCode={} costMs={}",
                    traceId,
                    id,
                    result.getTemplateCode(),
                    elapsedMillis(startedAt)
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "[TemplateSaveTrace][{}][controller] updateTemplate failed templateId={} after {}ms: {}",
                    traceId,
                    id,
                    elapsedMillis(startedAt),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private String resolveTemplateSaveTraceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TemplateSaveTraceInterceptor.TRACE_ATTRIBUTE);
        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }
        String traceHeader = request.getHeader(TemplateSaveTraceInterceptor.TRACE_HEADER);
        if (traceHeader != null && !traceHeader.isBlank()) {
            return traceHeader.trim();
        }
        return "no-trace-id";
    }

    private String summarizeTemplatePayload(ProcessTemplateSaveDTO dto) {
        return "templateType=" + dto.getTemplateType()
                + ", templateName=" + dto.getTemplateName()
                + ", formDesign=" + dto.getFormDesign()
                + ", approvalFlow=" + dto.getApprovalFlow()
                + ", expenseDetailDesign=" + dto.getExpenseDetailDesign()
                + ", enabled=" + dto.getEnabled();
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
