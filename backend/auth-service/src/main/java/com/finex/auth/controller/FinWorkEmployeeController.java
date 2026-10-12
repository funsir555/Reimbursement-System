package com.finex.auth.controller;

import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.digitalemployee.DigitalEmployeePlanService;
import com.finex.auth.service.digitalemployee.DigitalEmployeeService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/agent/chat")
@RequiredArgsConstructor
public class FinWorkEmployeeController {
    private final DigitalEmployeeService employees;
    private final DigitalEmployeePlanService plans;
    private final AccessControlService access;

    @GetMapping("/available-employees")
    public Result<List<Map<String, Object>>> available(HttpServletRequest request) {
        require(request);
        return Result.success(employees.availableAuthorizedFor(userId(request)));
    }

    @GetMapping("/subscriptions")
    public Result<List<Map<String, Object>>> subscriptions(HttpServletRequest request) {
        require(request);
        return Result.success(plans.subscriptions(userId(request)));
    }

    @PutMapping("/subscriptions/{planId}")
    public Result<Boolean> subscribe(@PathVariable Long planId,
            @RequestBody DigitalEmployeePlanService.SubscriptionRequest payload, HttpServletRequest request) {
        require(request);
        plans.subscribe(planId, userId(request), payload.enabled());
        return Result.success(true);
    }

    @GetMapping("/inbox")
    public Result<List<Map<String, Object>>> inbox(HttpServletRequest request) {
        require(request);
        return Result.success(plans.inbox(userId(request)));
    }

    private void require(HttpServletRequest request) {
        access.requirePermission(userId(request), "agent:chat:view");
    }

    private Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Number number) return number.longValue();
        throw new SecurityException("用户未登录");
    }
}
