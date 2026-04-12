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
@RequestMapping("/auth/archives/agents")
@RequiredArgsConstructor
public class ArchiveAgentController {

    private static final String AGENT_VIEW = "agents:view";
    private static final String AGENT_CREATE = "agents:create";
    private static final String AGENT_EDIT = "agents:edit";
    private static final String AGENT_DELETE = "agents:delete";
    private static final String AGENT_RUN = "agents:run";
    private static final String AGENT_PUBLISH = "agents:publish";
    private static final String AGENT_LOGS = "agents:view_logs";

    private final ArchiveAgentService archiveAgentService;
    private final AccessControlService accessControlService;

    // 处理 listAgents 请求。
    @GetMapping
    public Result<?> listAgents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.listAgents(getCurrentUserId(request), keyword, status));
    }

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<?> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.getMeta());
    }

    // 处理 detail 请求。
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.getAgentDetail(getCurrentUserId(request), id));
    }

    // 处理 create 请求。
    @PostMapping
    public Result<?> create(@Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_CREATE);
        return Result.success("Agent 鍒涘缓鎴愬姛", archiveAgentService.createAgent(getCurrentUserId(request), getCurrentUsername(request), dto));
    }

    // 处理 update 请求。
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_EDIT);
        return Result.success("Agent 鏇存柊鎴愬姛", archiveAgentService.updateAgent(getCurrentUserId(request), id, getCurrentUsername(request), dto));
    }

    // 处理 publish 请求。
    @PostMapping("/{id}/publish")
    public Result<?> publish(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_PUBLISH);
        return Result.success("Agent 鍙戝竷鎴愬姛", archiveAgentService.publishAgent(getCurrentUserId(request), id, getCurrentUsername(request)));
    }

    // 处理 run 请求。
    @PostMapping("/{id}/run")
    public Result<?> run(@PathVariable Long id, @RequestBody(required = false) ArchiveAgentRunDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_RUN);
        return Result.success("Agent 杩愯璇锋眰宸叉彁浜?", archiveAgentService.runAgent(getCurrentUserId(request), id, dto == null ? new ArchiveAgentRunDTO() : dto));
    }

    // 处理 runs 请求。
    @GetMapping("/{id}/runs")
    public Result<?> runs(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_LOGS);
        return Result.success(archiveAgentService.listRuns(getCurrentUserId(request), id));
    }

    // 处理 runDetail 请求。
    @GetMapping("/runs/{runId}")
    public Result<?> runDetail(@PathVariable Long runId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_LOGS);
        return Result.success(archiveAgentService.getRunDetail(getCurrentUserId(request), runId));
    }

    // 处理 toggleStatus 请求。
    @PostMapping("/{id}/toggle-status")
    public Result<?> toggleStatus(@PathVariable Long id, @Valid @RequestBody ArchiveAgentToggleStatusDTO dto, HttpServletRequest request) {
        requireToggleStatusPermission(getCurrentUserId(request), dto.getStatus());
        return Result.success("Agent 鐘舵€佹洿鏂版垚鍔?", archiveAgentService.updateAgentStatus(getCurrentUserId(request), id, dto.getStatus()));
    }

    private void requireToggleStatusPermission(Long currentUserId, String status) {
        if (ArchiveAgentSupport.AGENT_STATUS_ARCHIVED.equals(ArchiveAgentSupport.normalizeStatus(status))) {
            accessControlService.requirePermission(currentUserId, AGENT_DELETE);
            return;
        }
        accessControlService.requirePermission(currentUserId, AGENT_EDIT);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new SecurityException("鏈壘鍒板綋鍓嶇櫥褰曠敤鎴?");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
