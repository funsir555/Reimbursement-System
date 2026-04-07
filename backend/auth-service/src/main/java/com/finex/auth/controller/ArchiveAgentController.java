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

    @GetMapping
    public Result<?> listAgents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.listAgents(getCurrentUserId(request), keyword, status));
    }

    @GetMapping("/meta")
    public Result<?> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.getMeta());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_VIEW);
        return Result.success(archiveAgentService.getAgentDetail(getCurrentUserId(request), id));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_CREATE);
        return Result.success("Agent 鍒涘缓鎴愬姛", archiveAgentService.createAgent(getCurrentUserId(request), getCurrentUsername(request), dto));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ArchiveAgentSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_EDIT);
        return Result.success("Agent 鏇存柊鎴愬姛", archiveAgentService.updateAgent(getCurrentUserId(request), id, getCurrentUsername(request), dto));
    }

    @PostMapping("/{id}/publish")
    public Result<?> publish(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_PUBLISH);
        return Result.success("Agent 鍙戝竷鎴愬姛", archiveAgentService.publishAgent(getCurrentUserId(request), id, getCurrentUsername(request)));
    }

    @PostMapping("/{id}/run")
    public Result<?> run(@PathVariable Long id, @RequestBody(required = false) ArchiveAgentRunDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_RUN);
        return Result.success("Agent 杩愯璇锋眰宸叉彁浜?", archiveAgentService.runAgent(getCurrentUserId(request), id, dto == null ? new ArchiveAgentRunDTO() : dto));
    }

    @GetMapping("/{id}/runs")
    public Result<?> runs(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_LOGS);
        return Result.success(archiveAgentService.listRuns(getCurrentUserId(request), id));
    }

    @GetMapping("/runs/{runId}")
    public Result<?> runDetail(@PathVariable Long runId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), AGENT_LOGS);
        return Result.success(archiveAgentService.getRunDetail(getCurrentUserId(request), runId));
    }

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
