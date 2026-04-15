package com.finex.auth.controller;

import com.finex.auth.dto.FinanceDepartmentArchiveMetaVO;
import com.finex.auth.dto.FinanceDepartmentQueryDTO;
import com.finex.auth.dto.FinanceDepartmentVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FinanceDepartmentArchiveService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/finance/archives/departments")
@RequiredArgsConstructor
public class FinanceDepartmentArchiveController {

    private static final String DEPARTMENT_VIEW = "finance:archives:departments:view";

    private final FinanceDepartmentArchiveService financeDepartmentArchiveService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceDepartmentArchiveMetaVO> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DEPARTMENT_VIEW);
        return Result.success(financeDepartmentArchiveService.getMeta());
    }

    @PostMapping("/query")
    public Result<List<FinanceDepartmentVO>> query(
            @RequestBody(required = false) FinanceDepartmentQueryDTO query,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), DEPARTMENT_VIEW);
        return Result.success(financeDepartmentArchiveService.queryDepartments(
                query == null ? new FinanceDepartmentQueryDTO() : query
        ));
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
}
