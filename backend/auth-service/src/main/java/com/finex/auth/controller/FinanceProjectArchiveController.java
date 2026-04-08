package com.finex.auth.controller;

import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceProjectArchiveService;
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

import java.util.List;

@RestController
@RequestMapping("/auth/finance/archives/projects")
@RequiredArgsConstructor
public class FinanceProjectArchiveController {

    private static final String PROJECT_VIEW = "finance:archives:projects:view";
    private static final String PROJECT_CREATE = "finance:archives:projects:create";
    private static final String PROJECT_EDIT = "finance:archives:projects:edit";
    private static final String PROJECT_DISABLE = "finance:archives:projects:disable";
    private static final String PROJECT_CLOSE = "finance:archives:projects:close";

    private final FinanceProjectArchiveService financeProjectArchiveService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceProjectArchiveMetaVO> meta(@RequestParam String companyId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_VIEW);
        return Result.success(financeProjectArchiveService.getMeta(companyId));
    }

    @GetMapping("/classes")
    public Result<List<FinanceProjectClassSummaryVO>> listProjectClasses(
            @RequestParam String companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_VIEW);
        return Result.success(financeProjectArchiveService.listProjectClasses(companyId, keyword, status));
    }

    @PostMapping("/classes")
    public Result<FinanceProjectClassSummaryVO> createProjectClass(
            @RequestParam String companyId,
            @Valid @RequestBody FinanceProjectClassSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROJECT_CREATE, PROJECT_EDIT);
        return Result.success(
                "项目分类创建成功",
                financeProjectArchiveService.createProjectClass(companyId, dto, getCurrentUsername(request))
        );
    }

    @PutMapping("/classes/{projectClassCode}")
    public Result<FinanceProjectClassSummaryVO> updateProjectClass(
            @RequestParam String companyId,
            @PathVariable String projectClassCode,
            @Valid @RequestBody FinanceProjectClassSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_EDIT);
        return Result.success(
                "项目分类更新成功",
                financeProjectArchiveService.updateProjectClass(companyId, projectClassCode, dto, getCurrentUsername(request))
        );
    }

    @PostMapping("/classes/{projectClassCode}/status")
    public Result<Boolean> updateProjectClassStatus(
            @RequestParam String companyId,
            @PathVariable String projectClassCode,
            @Valid @RequestBody FinanceProjectStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROJECT_DISABLE, PROJECT_EDIT);
        return Result.success(
                "项目分类状态更新成功",
                financeProjectArchiveService.updateProjectClassStatus(companyId, projectClassCode, dto, getCurrentUsername(request))
        );
    }

    @GetMapping
    public Result<List<FinanceProjectSummaryVO>> listProjects(
            @RequestParam String companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectClassCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer bclose,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_VIEW);
        return Result.success(financeProjectArchiveService.listProjects(companyId, keyword, projectClassCode, status, bclose));
    }

    @GetMapping("/{projectCode}")
    public Result<FinanceProjectDetailVO> getProjectDetail(
            @RequestParam String companyId,
            @PathVariable String projectCode,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_VIEW);
        return Result.success(financeProjectArchiveService.getProjectDetail(companyId, projectCode));
    }

    @PostMapping
    public Result<FinanceProjectDetailVO> createProject(
            @RequestParam String companyId,
            @Valid @RequestBody FinanceProjectSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROJECT_CREATE, PROJECT_EDIT);
        return Result.success(
                "项目档案创建成功",
                financeProjectArchiveService.createProject(companyId, dto, getCurrentUsername(request))
        );
    }

    @PutMapping("/{projectCode}")
    public Result<FinanceProjectDetailVO> updateProject(
            @RequestParam String companyId,
            @PathVariable String projectCode,
            @Valid @RequestBody FinanceProjectSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), PROJECT_EDIT);
        return Result.success(
                "项目档案更新成功",
                financeProjectArchiveService.updateProject(companyId, projectCode, dto, getCurrentUsername(request))
        );
    }

    @PostMapping("/{projectCode}/status")
    public Result<Boolean> updateProjectStatus(
            @RequestParam String companyId,
            @PathVariable String projectCode,
            @Valid @RequestBody FinanceProjectStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROJECT_DISABLE, PROJECT_EDIT);
        return Result.success(
                "项目档案状态更新成功",
                financeProjectArchiveService.updateProjectStatus(companyId, projectCode, dto, getCurrentUsername(request))
        );
    }

    @PostMapping("/{projectCode}/close")
    public Result<Boolean> updateProjectCloseStatus(
            @RequestParam String companyId,
            @PathVariable String projectCode,
            @Valid @RequestBody FinanceProjectCloseDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), PROJECT_CLOSE, PROJECT_EDIT);
        return Result.success(
                "项目档案封存状态更新成功",
                financeProjectArchiveService.updateProjectCloseStatus(companyId, projectCode, dto, getCurrentUsername(request))
        );
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("当前用户信息缺失");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
