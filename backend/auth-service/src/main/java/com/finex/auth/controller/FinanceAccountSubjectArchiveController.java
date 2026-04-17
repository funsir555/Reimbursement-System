// 这里是 FinanceAccountSubjectArchiveController 的后端接口入口。
// 它主要负责接收请求、校验权限并调用下游 Service。
// 如果改错，最容易影响这一组接口的查询、保存或状态流转。

package com.finex.auth.controller;

import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
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

/**
 * 这是 FinanceAccountSubjectArchiveController 控制器。
 * 它主要负责接收请求、校验权限并调用下游 Service。
 * 具体业务规则以 Service 层为准。
 */
@RestController
@RequestMapping("/auth/finance/archives/account-subjects")
@RequiredArgsConstructor
public class FinanceAccountSubjectArchiveController {

    private static final String SUBJECT_VIEW = "finance:archives:account_subjects:view";
    private static final String SUBJECT_CREATE = "finance:archives:account_subjects:create";
    private static final String SUBJECT_EDIT = "finance:archives:account_subjects:edit";
    private static final String SUBJECT_DISABLE = "finance:archives:account_subjects:disable";
    private static final String SUBJECT_CLOSE = "finance:archives:account_subjects:close";

    private final FinanceAccountSubjectArchiveService financeAccountSubjectArchiveService;
    private final AccessControlService accessControlService;

    // 处理 meta 请求。
    @GetMapping("/meta")
    public Result<FinanceAccountSubjectMetaVO> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), SUBJECT_VIEW);
        return Result.success(financeAccountSubjectArchiveService.getMeta());
    }

    // 处理 listSubjects 请求。
    @GetMapping
    public Result<List<FinanceAccountSubjectSummaryVO>> listSubjects(
            @RequestParam String companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subjectCategory,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer bclose,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUBJECT_VIEW);
        return Result.success(financeAccountSubjectArchiveService.listSubjects(companyId, keyword, subjectCategory, status, bclose));
    }

    // 处理 getSubjectDetail 请求。
    @GetMapping("/{subjectCode}")
    public Result<FinanceAccountSubjectDetailVO> getSubjectDetail(
            @RequestParam String companyId,
            @PathVariable String subjectCode,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUBJECT_VIEW);
        return Result.success(financeAccountSubjectArchiveService.getSubjectDetail(companyId, subjectCode));
    }

    @GetMapping("/derived-defaults")
    public Result<FinanceAccountSubjectDerivedDefaultsVO> getDerivedDefaults(
            @RequestParam String companyId,
            @RequestParam String subjectCode,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUBJECT_VIEW);
        return Result.success(financeAccountSubjectArchiveService.getDerivedDefaults(companyId, subjectCode));
    }

    // 处理 createSubject 请求。
    @PostMapping
    public Result<FinanceAccountSubjectDetailVO> createSubject(
            @RequestParam String companyId,
            @Valid @RequestBody FinanceAccountSubjectSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SUBJECT_CREATE, SUBJECT_EDIT);
        return Result.success(
                "会计科目创建成功",
                financeAccountSubjectArchiveService.createSubject(companyId, dto, getCurrentUsername(request))
        );
    }

    // 处理 updateSubject 请求。
    @PutMapping("/{subjectCode}")
    public Result<FinanceAccountSubjectDetailVO> updateSubject(
            @RequestParam String companyId,
            @PathVariable String subjectCode,
            @Valid @RequestBody FinanceAccountSubjectSaveDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), SUBJECT_EDIT);
        return Result.success(
                "会计科目更新成功",
                financeAccountSubjectArchiveService.updateSubject(companyId, subjectCode, dto, getCurrentUsername(request))
        );
    }

    // 处理 updateStatus 请求。
    @PostMapping("/{subjectCode}/status")
    public Result<Boolean> updateStatus(
            @RequestParam String companyId,
            @PathVariable String subjectCode,
            @Valid @RequestBody FinanceAccountSubjectStatusDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SUBJECT_DISABLE, SUBJECT_EDIT);
        return Result.success(
                "会计科目状态更新成功",
                financeAccountSubjectArchiveService.updateStatus(companyId, subjectCode, dto, getCurrentUsername(request))
        );
    }

    // 处理 updateCloseStatus 请求。
    @PostMapping("/{subjectCode}/close")
    public Result<Boolean> updateCloseStatus(
            @RequestParam String companyId,
            @PathVariable String subjectCode,
            @Valid @RequestBody FinanceAccountSubjectCloseDTO dto,
            HttpServletRequest request
    ) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), SUBJECT_CLOSE, SUBJECT_EDIT);
        return Result.success(
                "会计科目封存状态更新成功",
                financeAccountSubjectArchiveService.updateCloseStatus(companyId, subjectCode, dto, getCurrentUsername(request))
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
