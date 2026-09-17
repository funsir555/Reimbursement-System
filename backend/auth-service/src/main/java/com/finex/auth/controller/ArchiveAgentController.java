// 这里是 ArchiveAgentController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentToggleStatusDTO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.ArchiveAgentService;
import com.finex.auth.support.archiveagent.ArchiveAgentSupport;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 这是 ArchiveAgentController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping({"/auth/archives/agents", "/auth/agent/workbench"})
@RequiredArgsConstructor
public class ArchiveAgentController {

    private static final String AGENT_VIEW = "agent:workbench:view";
    private static final String AGENT_CREATE = "agent:workbench:create";
    private static final String AGENT_EDIT = "agent:workbench:edit";
    private static final String AGENT_DELETE = "agent:workbench:delete";
    private static final String AGENT_RUN = "agent:workbench:run";
    private static final String AGENT_PUBLISH = "agent:workbench:publish";
    private static final String AGENT_LOGS = "agent:workbench:view_logs";
    private static final String LEGACY_AGENT_VIEW = "agents:view";
    private static final String LEGACY_AGENT_CREATE = "agents:create";
    private static final String LEGACY_AGENT_EDIT = "agents:edit";
    private static final String LEGACY_AGENT_DELETE = "agents:delete";
    private static final String LEGACY_AGENT_RUN = "agents:run";
    private static final String LEGACY_AGENT_PUBLISH = "agents:publish";
    private static final String LEGACY_AGENT_LOGS = "agents:view_logs";

    private final ArchiveAgentService archiveAgentService;
    private final AccessControlService accessControlService;

    // 处理 listAgents 请求。
    @GetMapping
    public Result<?> listAgents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        requireAnyPermission(getCurrentUserId(request), AGENT_VIEW, LEGACY_AGENT_VIEW);
        return Result.success(archiveAgentService.listAgents(getCurrentUserId(request), keyword, status));
    }

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<?> meta(HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_VIEW, LEGACY_AGENT_VIEW);
        return Result.success(archiveAgentService.getMeta());
    }

    // 处理 detail 请求。
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_VIEW, LEGACY_AGENT_VIEW);
        return Result.success(archiveAgentService.getAgentDetail(getCurrentUserId(request), id));
    }

    // 处理 create 请求。
    @PostMapping
    public Result<?> create(@Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_CREATE, LEGACY_AGENT_CREATE);
        return Result.success("Agent 创建成功", archiveAgentService.createAgent(getCurrentUserId(request), getCurrentUsername(request), dto));
    }

    // 处理 update 请求。
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_EDIT, LEGACY_AGENT_EDIT);
        return Result.success("Agent 更新成功", archiveAgentService.updateAgent(getCurrentUserId(request), id, getCurrentUsername(request), dto));
    }

    // 处理 publish 请求。
    @PostMapping("/{id}/publish")
    public Result<?> publish(@PathVariable Long id, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_PUBLISH, LEGACY_AGENT_PUBLISH);
        return Result.success("Agent 发布成功", archiveAgentService.publishAgent(getCurrentUserId(request), id, getCurrentUsername(request)));
    }

    // 处理 run 请求。
    @PostMapping("/{id}/run")
    public Result<?> run(@PathVariable Long id, @RequestBody(required = false) ArchiveAgentRunDTO dto, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_RUN, LEGACY_AGENT_RUN);
        return Result.success("Agent 运行请求已提交", archiveAgentService.runAgent(getCurrentUserId(request), id, dto == null ? new ArchiveAgentRunDTO() : dto));
    }

    // 处理 runs 请求。
    @GetMapping("/{id}/runs")
    public Result<?> runs(@PathVariable Long id, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_LOGS, LEGACY_AGENT_LOGS);
        return Result.success(archiveAgentService.listRuns(getCurrentUserId(request), id));
    }

    // 处理 runDetail 请求。
    @GetMapping("/runs/{runId}")
    public Result<?> runDetail(@PathVariable Long runId, HttpServletRequest request) {
        requireAnyPermission(getCurrentUserId(request), AGENT_LOGS, LEGACY_AGENT_LOGS);
        return Result.success(archiveAgentService.getRunDetail(getCurrentUserId(request), runId));
    }

    // 处理 toggleStatus 请求。
    @PostMapping("/{id}/toggle-status")
    public Result<?> toggleStatus(@PathVariable Long id, @Valid @RequestBody ArchiveAgentToggleStatusDTO dto, HttpServletRequest request) {
        requireToggleStatusPermission(getCurrentUserId(request), dto.getStatus());
        return Result.success("Agent 状态更新成功", archiveAgentService.updateAgentStatus(getCurrentUserId(request), id, dto.getStatus()));
    }

    // 删除 Agent 及其专属配置和运行数据。
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        requireAnyPermission(currentUserId, AGENT_DELETE, LEGACY_AGENT_DELETE);
        archiveAgentService.deleteAgent(currentUserId, id);
        return Result.success("Agent 已删除", null);
    }

    private void requireToggleStatusPermission(Long currentUserId, String status) {
        if (ArchiveAgentSupport.AGENT_STATUS_ARCHIVED.equals(ArchiveAgentSupport.normalizeStatus(status))) {
            requireAnyPermission(currentUserId, AGENT_DELETE, LEGACY_AGENT_DELETE);
            return;
        }
        requireAnyPermission(currentUserId, AGENT_EDIT, LEGACY_AGENT_EDIT);
    }

    private void requireAnyPermission(Long currentUserId, String... permissionCodes) {
        accessControlService.requireAnyPermission(currentUserId, permissionCodes);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new SecurityException("未找到当前登录用户");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
