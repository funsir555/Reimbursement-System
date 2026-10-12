package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.ExpenseDocumentReadAccess;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Object access for direct Expense reads; the query permission and its scope stay together. */
@Service
@RequiredArgsConstructor
public class ExpenseDocumentReadAuthorization implements ExpenseDocumentReadAccess {
    public static final String MY_DOCUMENTS = "expense:list:view";
    public static final String APPROVAL = "expense:approval:view";
    public static final String DOCUMENT_SEARCH = "expense:documents:view";
    public static final String PAYMENT_ORDER = "expense:payment:payment_order:view";

    private final QueryAuthorizationService authorizations;
    private final ProcessDocumentInstanceMapper documents;
    private final ProcessDocumentTaskMapper tasks;

    public QueryAuthorization requireScope(Long userId, String permissionCode) {
        return authorizations.resolveForSelf(userId, permissionCode);
    }

    public void requireDocument(Long userId, String documentCode, List<String> grantedPermissions) {
        if (userId == null || documentCode == null || grantedPermissions == null
                || (TenantContext.getTenantId() != null && !"default".equals(TenantContext.getTenantId()))) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        ProcessDocumentInstance document = documents.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, documentCode)
                        .last("LIMIT 1"));
        if (document == null || Boolean.TRUE.equals(document.getDeleted())
                || document.getCompanyId() == null) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        String companyId = document.getCompanyId();
        boolean own = Objects.equals(document.getSubmitterUserId(), userId);
        boolean draft = "DRAFT".equals(document.getStatus());
        if (own && draft && (grantedPermissions.contains("expense:create:view")
                || grantedPermissions.contains("expense:create:create")
                || grantedPermissions.contains("expense:create:submit"))) return;
        if (own && allowed(grantedPermissions, MY_DOCUMENTS, userId, companyId, userId)) return;
        if (assigned(userId, documentCode)
                && allowedAssigned(grantedPermissions, APPROVAL, userId, companyId)) return;
        if (!draft && allowed(grantedPermissions, DOCUMENT_SEARCH, userId,
                companyId, document.getSubmitterUserId())) return;
        if (!draft && assigned(userId, documentCode)
                && allowedAssigned(grantedPermissions, DOCUMENT_SEARCH, userId, companyId)) return;
        if (!draft && assignedPayment(userId, documentCode)
                && allowedAssigned(grantedPermissions, PAYMENT_ORDER, userId, companyId)) return;
        throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
    }

    public void applyScope(LambdaQueryWrapper<ProcessDocumentInstance> wrapper,
                           QueryAuthorization authorization, boolean includeAssignedTasks) {
        List<Branch> branches = new ArrayList<>();
        List<String> assignedCodes = includeAssignedTasks
                ? tasks.selectList(Wrappers.<ProcessDocumentTask>lambdaQuery()
                        .eq(ProcessDocumentTask::getAssigneeUserId, authorization.requesterUserId()))
                        .stream().map(ProcessDocumentTask::getDocumentCode)
                        .filter(Objects::nonNull).distinct().toList()
                : List.of();
        for (String companyId : authorization.companyIds().stream().sorted().toList()) {
            Set<Long> subjects = authorization.subjectUserIdsForCompany(companyId);
            if (subjects == null || !subjects.isEmpty()) {
                branches.add(new Branch(companyId, subjects, null));
            }
            if (!assignedCodes.isEmpty() && authorization.allowsAssignedTask(companyId)) {
                Set<Long> taskSubjects = authorization.assignedSubjectUserIdsForCompany(companyId);
                if (taskSubjects == null || !taskSubjects.isEmpty()) {
                    branches.add(new Branch(companyId, taskSubjects, assignedCodes));
                }
            }
        }
        if (branches.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        wrapper.and(group -> {
            boolean first = true;
            for (Branch branch : branches) {
                if (!first) group.or();
                group.nested(slice -> {
                    slice.eq(ProcessDocumentInstance::getCompanyId, branch.companyId());
                    if (branch.subjectUserIds() != null) {
                        slice.in(ProcessDocumentInstance::getSubmitterUserId, branch.subjectUserIds());
                    }
                    if (branch.assignedDocumentCodes() != null) {
                        slice.in(ProcessDocumentInstance::getDocumentCode, branch.assignedDocumentCodes());
                    }
                });
                first = false;
            }
        });
    }

    private boolean allowed(List<String> permissions, String code, Long userId,
                            String companyId, Long submitterId) {
        if (!permissions.contains(code)) return false;
        try {
            return requireScope(userId, code).allows(companyId, submitterId);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private boolean allowedAssigned(List<String> permissions, String code, Long userId,
                                    String companyId) {
        if (!permissions.contains(code)) return false;
        try {
            return requireScope(userId, code).allowsAssignedTask(companyId);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private boolean assigned(Long userId, String documentCode) {
        return tasks.selectCount(Wrappers.<ProcessDocumentTask>lambdaQuery()
                .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                .eq(ProcessDocumentTask::getAssigneeUserId, userId)) > 0;
    }

    private boolean assignedPayment(Long userId, String documentCode) {
        return tasks.selectCount(Wrappers.<ProcessDocumentTask>lambdaQuery()
                .eq(ProcessDocumentTask::getDocumentCode, documentCode)
                .eq(ProcessDocumentTask::getAssigneeUserId, userId)
                .eq(ProcessDocumentTask::getNodeType, "PAYMENT")) > 0;
    }

    private record Branch(String companyId, Set<Long> subjectUserIds,
                          List<String> assignedDocumentCodes) { }
}
