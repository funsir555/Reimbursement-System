package com.finex.auth.agent.ontology;

import com.finex.auth.service.AccessControlService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/auth/agent/business-ontology")
@RequiredArgsConstructor
public class BusinessOntologyController {
    private final BusinessOntologyService ontology;
    private final OntologyResultContextService resultContext;
    private final AccessControlService access;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword, HttpServletRequest request) {
        long userId = userId(request);
        access.requirePermission(userId, "agent:business_ontology:view");
        boolean curator = access.getPermissionCodes(userId).stream().anyMatch(code ->
                "agent:business_ontology:edit".equals(code)
                        || "agent:business_ontology:publish".equals(code));
        LocalDate today = LocalDate.now();
        return Result.success(curator ? ontology.list(keyword) : ontology.list(keyword).stream()
                .filter(item -> "PUBLISHED".equals(item.status())
                        && !item.effectiveFrom().isAfter(today)
                        && (item.effectiveUntil() == null || !item.effectiveUntil().isBefore(today)))
                .toList());
    }

    @GetMapping("/result-links")
    public Result<?> resultLinks(HttpServletRequest request) {
        access.requirePermission(userId(request), "agent:business_ontology:view");
        return Result.success(resultContext.listPublished());
    }

    @PostMapping("/drafts")
    public Result<?> saveDraft(@RequestBody BusinessOntologyService.Draft draft, HttpServletRequest request) {
        long userId = userId(request);
        access.requirePermission(userId, "agent:business_ontology:edit");
        return Result.success(ontology.saveDraft(draft, userId));
    }

    @PostMapping("/{id}/publish")
    public Result<?> publish(@PathVariable long id, HttpServletRequest request) {
        long userId = userId(request);
        access.requirePermission(userId, "agent:business_ontology:publish");
        ontology.publish(id, userId);
        return Result.success("业务本体已发布", null);
    }

    @PostMapping("/{id}/retire")
    public Result<?> retire(@PathVariable long id, HttpServletRequest request) {
        long userId = userId(request);
        access.requirePermission(userId, "agent:business_ontology:publish");
        ontology.retire(id, userId);
        return Result.success("业务本体已停用", null);
    }

    private long userId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Number number) return number.longValue();
        throw new SecurityException("未找到当前登录用户");
    }
}
