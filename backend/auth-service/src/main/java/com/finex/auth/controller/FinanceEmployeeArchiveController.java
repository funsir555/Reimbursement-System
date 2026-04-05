package com.finex.auth.controller;

import com.finex.auth.dto.EmployeeQueryDTO;
import com.finex.auth.dto.EmployeeVO;
import com.finex.auth.dto.FinanceEmployeeArchiveMetaVO;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.SystemSettingsService;
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
@RequestMapping("/auth/finance/archives/employees")
@RequiredArgsConstructor
public class FinanceEmployeeArchiveController {

    private static final String EMPLOYEE_VIEW = "finance:archives:employees:view";

    private final SystemSettingsService systemSettingsService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FinanceEmployeeArchiveMetaVO> meta(HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EMPLOYEE_VIEW);
        FinanceEmployeeArchiveMetaVO meta = new FinanceEmployeeArchiveMetaVO();
        meta.setCompanies(systemSettingsService.listCompanies());
        meta.setDepartments(systemSettingsService.listDepartments());
        return Result.success(meta);
    }

    @PostMapping("/query")
    public Result<List<EmployeeVO>> queryEmployees(
            @RequestBody(required = false) EmployeeQueryDTO query,
            HttpServletRequest request
    ) {
        accessControlService.requirePermission(getCurrentUserId(request), EMPLOYEE_VIEW);
        return Result.success(systemSettingsService.listEmployees(query == null ? new EmployeeQueryDTO() : query));
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
