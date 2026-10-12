package com.finex.auth.controller;

import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.auth.service.digitalemployee.WorkflowShareService;
import com.finex.auth.service.digitalemployee.DigitalEmployeePlanService;
import com.finex.auth.service.digitalemployee.DigitalEmployeeMetricsService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/agent/digital-employees")
@RequiredArgsConstructor
public class DigitalEmployeeController {
    private final DigitalEmployeeService employees;
    private final WorkflowShareService shares;
    private final AccessControlService access;
    private final DigitalEmployeePlanService plans;
    private final DigitalEmployeeMetricsService metrics;

    @GetMapping
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(employees.list());
    }

    @GetMapping("/directory")
    public Result<Map<String, Object>> directory(HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(employees.directory());
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(employees.detail(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody DigitalEmployeeService.SaveRequest payload,
                                               HttpServletRequest request) {
        require(request, "agent:digital_employee:create");
        return Result.success(employees.create(payload, userId(request)));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id,
                                               @RequestBody DigitalEmployeeService.SaveRequest payload,
                                               HttpServletRequest request) {
        require(request, "agent:digital_employee:edit");
        return Result.success(employees.update(id, payload, userId(request)));
    }

    @PutMapping("/{id}/status")
    public Result<Map<String, Object>> status(@PathVariable Long id,
                                               @RequestBody DigitalEmployeeService.StatusRequest payload,
                                               HttpServletRequest request) {
        require(request, "agent:digital_employee:status");
        return Result.success(employees.changeStatus(id, payload, userId(request)));
    }

    @PutMapping("/{id}/budget")
    public Result<Map<String, Object>> budget(@PathVariable Long id,
                                               @RequestBody DigitalEmployeeService.BudgetRequest payload,
                                               HttpServletRequest request) {
        require(request, "agent:digital_employee:plan");
        return Result.success(employees.setBudget(id, payload, userId(request)));
    }

    @PutMapping("/{id}/users")
    public Result<Map<String, Object>> assign(@PathVariable Long id,
                                               @RequestBody DigitalEmployeeService.AssignmentRequest payload,
                                               HttpServletRequest request) {
        require(request, "agent:digital_employee:assign");
        return Result.success(employees.assign(id, payload, userId(request)));
    }

    @PostMapping("/{id}/workflows")
    public Result<Map<String, Object>> bind(@PathVariable Long id,
                                             @RequestBody DigitalEmployeeService.BindingRequest payload,
                                             HttpServletRequest request) {
        require(request, "agent:digital_employee:edit");
        return Result.success(employees.bind(id, payload, userId(request)));
    }

    @DeleteMapping("/{id}/workflows/{bindingId}")
    public Result<Boolean> unbind(@PathVariable Long id, @PathVariable Long bindingId,
                                  @RequestParam Long versionNo,
                                  HttpServletRequest request) {
        require(request, "agent:digital_employee:edit");
        employees.unbind(id, bindingId, versionNo, userId(request));
        return Result.success(true);
    }

    @GetMapping("/{id}/audit")
    public Result<List<Map<String, Object>>> audit(@PathVariable Long id, HttpServletRequest request) {
        require(request, "agent:digital_employee:logs");
        return Result.success(employees.audit(id));
    }

    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long id, @RequestParam Long userId,
            @RequestParam String permissionCode, HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(employees.preview(id, userId, permissionCode));
    }

    @GetMapping("/{id}/plans")
    public Result<List<Map<String, Object>>> plans(@PathVariable Long id, HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(plans.plans(id));
    }

    @PostMapping("/{id}/plans")
    public Result<Map<String, Object>> createPlan(@PathVariable Long id,
            @RequestBody DigitalEmployeePlanService.PlanRequest payload, HttpServletRequest request) {
        require(request, "agent:digital_employee:plan");
        return Result.success(plans.save(id, null, payload, userId(request)));
    }

    @PutMapping("/{id}/plans/{planId}")
    public Result<Map<String, Object>> updatePlan(@PathVariable Long id, @PathVariable Long planId,
            @RequestBody DigitalEmployeePlanService.PlanRequest payload, HttpServletRequest request) {
        require(request, "agent:digital_employee:plan");
        return Result.success(plans.save(id, planId, payload, userId(request)));
    }

    @GetMapping("/{id}/metrics")
    public Result<Map<String, Object>> metrics(@PathVariable Long id,
            @RequestParam(defaultValue = "7") int days, HttpServletRequest request) {
        require(request, "agent:digital_employee:view");
        return Result.success(metrics.metrics(id, days));
    }

    @GetMapping("/workflow-shares")
    public Result<List<Map<String, Object>>> workflowShares(HttpServletRequest request) {
        access.requireAnyPermission(userId(request), "agent:digital_employee:view", "agent:workbench:share_approve");
        return Result.success(shares.catalog(access.getPermissionCodes(userId(request))
                .contains("agent:workbench:share_approve")));
    }

    @PostMapping("/workflow-shares")
    public Result<Map<String, Object>> submitShare(@RequestBody ShareSubmit payload, HttpServletRequest request) {
        require(request, "agent:workbench:share_submit");
        return Result.success(shares.submit(payload.agentId(), payload.versionId(), userId(request)));
    }

    @PutMapping("/workflow-shares/{shareId}/review")
    public Result<Map<String, Object>> reviewShare(@PathVariable Long shareId, @RequestBody ShareReview payload,
                                                    HttpServletRequest request) {
        require(request, "agent:workbench:share_approve");
        return Result.success(shares.review(shareId, payload.approve(), userId(request)));
    }

    @PutMapping("/workflow-shares/{shareId}/revoke")
    public Result<Map<String, Object>> revokeShare(@PathVariable Long shareId, HttpServletRequest request) {
        require(request, "agent:workbench:share_approve");
        return Result.success(shares.revoke(shareId, userId(request)));
    }

    public record ShareSubmit(Long agentId, Long versionId) { }
    public record ShareReview(boolean approve) { }

    private void require(HttpServletRequest request, String code) { access.requirePermission(userId(request), code); }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Number number) return number.longValue();
        throw new SecurityException("用户未登录");
    }
}
