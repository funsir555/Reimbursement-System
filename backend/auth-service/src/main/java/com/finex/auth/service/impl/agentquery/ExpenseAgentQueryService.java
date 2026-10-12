package com.finex.auth.service.impl.agentquery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 报销域对 Agent 暴露的权限感知只读适配器。
 */
@Service
@RequiredArgsConstructor
public class ExpenseAgentQueryService implements ExpenseAgentQueryPort {

    private final ProcessDocumentInstanceMapper documentMapper;
    private final ProcessDocumentTaskMapper taskMapper;

    @Override
    public List<DocumentView> queryAuthorizedDocuments(Query query, QueryAuthorization authorization) {
        validateAuthorized(query, authorization);
        return queryDocuments(query, authorization);
    }

    @Override
    public long countAuthorizedDocuments(Query query, QueryAuthorization authorization) {
        validateAuthorized(query, authorization);
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = buildWrapper(query, authorization);
        return wrapper == null ? 0 : documentMapper.selectCount(wrapper);
    }

    @Override
    public Aggregate aggregateAuthorizedDocuments(Query query, QueryAuthorization authorization) {
        validateAuthorized(query, authorization);
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = buildWrapper(query, authorization);
        if (wrapper == null) return new Aggregate(0, BigDecimal.ZERO);
        Map<String, Object> values = documentMapper.aggregateAuthorized(wrapper);
        if (values == null) throw new IllegalStateException("无法计算授权单据金额");
        Object count = values.get("total_count");
        Object amount = values.get("total_amount");
        return new Aggregate(count instanceof Number number ? number.longValue() : 0,
                amount instanceof BigDecimal decimal ? decimal
                        : amount == null ? BigDecimal.ZERO : new BigDecimal(amount.toString()));
    }

    @Override
    public String fingerprintAuthorizedDocuments(Query query, QueryAuthorization authorization) {
        validateAuthorized(query, authorization);
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = buildWrapper(query, authorization);
        List<String> snapshots = wrapper == null ? List.of() : documentMapper.selectList(
                wrapper.select(ProcessDocumentInstance::getId,
                        ProcessDocumentInstance::getTotalAmount,
                        ProcessDocumentInstance::getUpdatedAt)).stream()
                .map(item -> item.getId() + "|" + item.getTotalAmount() + "|" + item.getUpdatedAt())
                .sorted().toList();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String snapshot : snapshots) {
                digest.update(snapshot.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private void validateAuthorized(Query query, QueryAuthorization authorization) {
        if (query == null || query.userId() == null || query.scope() == null || authorization == null
                || !query.userId().equals(authorization.requesterUserId())
                || !QueryAuthorizationService.EXPENSE_QUERY.equals(authorization.permissionCode())
                || authorization.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
    }

    private List<DocumentView> queryDocuments(Query query, QueryAuthorization authorization) {
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = buildWrapper(query, authorization);
        if (wrapper == null) return List.of();
        int limit = Math.min(Math.max(query.limit(), 1), 50);
        wrapper.orderByDesc(ProcessDocumentInstance::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        return documentMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    private LambdaQueryWrapper<ProcessDocumentInstance> buildWrapper(Query query, QueryAuthorization authorization) {
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = new LambdaQueryWrapper<>();
        {
            List<String> companies = authorization.companyIds().stream()
                    .filter(id -> query.companyId() == null || query.companyId().isBlank()
                            || id.equals(query.companyId().trim()))
                    .filter(id -> query.scope() != Scope.ASSIGNED_TO_USER || authorization.allowsAssignedTask(id))
                    .sorted().toList();
            if (companies.isEmpty()) throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
            List<Object> parameters = new ArrayList<>();
            List<String> slices = new ArrayList<>();
            for (String company : companies) {
                Set<Long> subjects = query.scope() == Scope.ASSIGNED_TO_USER
                        ? authorization.assignedSubjectUserIdsForCompany(company)
                        : authorization.subjectUserIdsForCompany(company);
                if (subjects != null && subjects.isEmpty()) continue;
                int index = parameters.size();
                parameters.add(company);
                StringBuilder slice = new StringBuilder("(pm_document_instance.company_id={" + index + "}");
                if (subjects != null) {
                    slice.append(" AND pm_document_instance.submitter_user_id IN (");
                    List<Long> ordered = subjects.stream().sorted().toList();
                    for (int i = 0; i < ordered.size(); i++) {
                        if (i > 0) slice.append(',');
                        slice.append('{').append(parameters.size()).append('}');
                        parameters.add(ordered.get(i));
                    }
                    slice.append(')');
                }
                slice.append(')');
                slices.add(slice.toString());
            }
            if (slices.isEmpty()) return null;
            wrapper.apply("(" + String.join(" OR ", slices) + ")", parameters.toArray());
        }

        if (query.scope() == Scope.ASSIGNED_TO_USER) {
            List<String> assignedDocumentCodes = taskMapper.selectList(
                    new LambdaQueryWrapper<ProcessDocumentTask>()
                            .eq(ProcessDocumentTask::getAssigneeUserId, query.userId())
                            .eq(ProcessDocumentTask::getStatus, "PENDING")
                            .orderByAsc(ProcessDocumentTask::getCreatedAt, ProcessDocumentTask::getId)
            ).stream()
                    .map(ProcessDocumentTask::getDocumentCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .toList();
            if (assignedDocumentCodes.isEmpty()) {
                return null;
            }
            wrapper.in(ProcessDocumentInstance::getDocumentCode, assignedDocumentCodes);
        } else if (query.scope() == Scope.SUBMITTED_BY_USER) {
            wrapper.eq(ProcessDocumentInstance::getSubmitterUserId, query.userId());
        }

        if (hasText(query.templateType())) {
            wrapper.eq(ProcessDocumentInstance::getTemplateType, query.templateType().trim());
        }
        if (query.excludeDeleted()) {
            wrapper.and(inner -> inner.isNull(ProcessDocumentInstance::getDeleted)
                    .or()
                    .eq(ProcessDocumentInstance::getDeleted, Boolean.FALSE));
        }
        if (hasText(query.status())) {
            wrapper.eq(ProcessDocumentInstance::getStatus, query.status().trim());
        }
        if (query.startDate() != null) {
            LocalDateTime start = LocalDateTime.ofInstant(query.startDate(), ZoneId.systemDefault());
            wrapper.ge(ProcessDocumentInstance::getCreatedAt, start);
        }
        if (query.endDate() != null) {
            LocalDateTime end = LocalDateTime.ofInstant(query.endDate(), ZoneId.systemDefault());
            wrapper.le(ProcessDocumentInstance::getCreatedAt, end);
        }
        if (query.minAmount() != null) {
            wrapper.ge(ProcessDocumentInstance::getTotalAmount, query.minAmount());
        }
        if (query.maxAmount() != null) {
            wrapper.le(ProcessDocumentInstance::getTotalAmount, query.maxAmount());
        }
        if (hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(inner -> inner
                    .like(ProcessDocumentInstance::getDocumentCode, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getDocumentTitle, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getDocumentReason, keyword)
                    .or()
                    .like(ProcessDocumentInstance::getTemplateName, keyword));
        }

        return wrapper;
    }

    private DocumentView toView(ProcessDocumentInstance document) {
        return new DocumentView(
                document.getId(),
                document.getDocumentCode(),
                document.getDocumentTitle(),
                document.getDocumentReason(),
                document.getTotalAmount(),
                document.getStatus(),
                document.getTemplateName(),
                document.getCreatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
