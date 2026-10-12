package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.ExpenseExportAccess;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Provenance and current authorization for asynchronous Expense downloads. */
@Service
@RequiredArgsConstructor
public class ExpenseExportSnapshotService implements ExpenseExportAccess {
    private static final Set<String> EXPENSE_DOWNLOAD_TYPES = Set.of(
            "我的报销导出", "待我审批导出", "单据查询导出", "支付单导出",
            "待还款导出", "待核销导出", "报销导出");

    private final QueryAuthorizationService authorizations;
    private final ExpenseDocumentService documents;
    private final ProcessDocumentInstanceMapper documentMapper;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public boolean isExpenseDownload(String businessType) {
        return EXPENSE_DOWNLOAD_TYPES.contains(businessType);
    }

    public void requireScene(Long userId, ExpenseExportSubmitDTO payload) {
        authorizations.resolveForSelf(userId, permissionCode(payload));
    }

    public Snapshot snapshot(Long userId, ExpenseExportSubmitDTO payload, List<String> exportedCodes) {
        QueryAuthorization authorization = authorizations.resolveForSelf(userId, permissionCode(payload));
        Set<String> eligible = eligibleCodes(userId, payload, authorization);
        if (exportedCodes == null || !eligible.containsAll(exportedCodes)) {
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        }
        try {
            String descriptor = json.writeValueAsString(Map.of(
                    "payload", payload, "sourceCodes", exportedCodes));
            return new Snapshot(permissionCode(payload), authorization.digest(),
                    descriptor, digest(exportedCodes));
        } catch (Exception exception) {
            throw new IllegalStateException("无法记录报销导出来源", exception);
        }
    }

    public void record(Long downloadId, Snapshot snapshot) {
        jdbc.update("""
                INSERT INTO sys_download_query_origin
                  (download_record_id,permission_code,auth_digest,query_json,source_digest)
                VALUES (?,?,?,?,?)
                ON DUPLICATE KEY UPDATE permission_code=VALUES(permission_code),
                  auth_digest=VALUES(auth_digest),query_json=VALUES(query_json),
                  source_digest=VALUES(source_digest)
                """, downloadId, snapshot.permissionCode(), snapshot.authDigest(),
                snapshot.queryJson(), snapshot.sourceDigest());
    }

    public boolean canDownload(Long userId, Long downloadId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM sys_download_query_origin WHERE download_record_id=?", downloadId);
            if (rows.size() != 1) return false;
            Map<String, Object> origin = rows.get(0);
            Map<String, Object> descriptor = json.readValue(
                    String.valueOf(origin.get("query_json")), new TypeReference<>() { });
            ExpenseExportSubmitDTO payload = json.convertValue(
                    descriptor.get("payload"), ExpenseExportSubmitDTO.class);
            List<String> sourceCodes = ((List<?>) descriptor.get("sourceCodes"))
                    .stream().map(String::valueOf).toList();
            if (!permissionCode(payload).equals(origin.get("permission_code"))) return false;
            QueryAuthorization current = authorizations.resolveForSelf(userId, permissionCode(payload));
            return current.digest().equals(origin.get("auth_digest"))
                    && digest(sourceCodes).equals(origin.get("source_digest"))
                    && eligibleCodes(userId, payload, current).containsAll(sourceCodes);
        } catch (Exception exception) {
            return false;
        }
    }

    private Set<String> eligibleCodes(Long userId, ExpenseExportSubmitDTO payload,
                                      QueryAuthorization authorization) {
        String scene = payload.getScene();
        if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES.equals(scene)) {
            return codes(documents.listExpenseSummaries(userId).stream()
                    .map(item -> item.getDocumentCode()).toList());
        }
        if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY.equals(scene)) {
            return codes(documents.listQueryDocumentSummaries(userId).stream()
                    .map(item -> item.getDocumentCode()).toList());
        }
        if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL.equals(scene)) {
            return codes(documents.listPendingApprovals(userId).stream()
                    .filter(item -> payload.getTaskIds() != null && payload.getTaskIds().contains(item.getTaskId()))
                    .map(item -> item.getDocumentCode()).toList());
        }
        if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PAYMENT_PENDING.equals(scene)) {
            List<String> codes = new ArrayList<>();
            for (String status : List.of("PENDING_PAYMENT", "PAYING", "PAYMENT_COMPLETED",
                    "PAYMENT_FINISHED", "PAYMENT_EXCEPTION")) {
                documents.listPaymentOrders(userId, status).stream()
                        .filter(item -> payload.getTaskIds() != null && payload.getTaskIds().contains(item.getTaskId()))
                        .forEach(item -> codes.add(item.getDocumentCode()));
            }
            return allowedDocuments(userId, codes, authorization);
        }
        if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING.equals(scene)) {
            return allowedDocuments(userId, documents.listOutstandingDocuments(userId, payload.getKind())
                    .stream().map(item -> item.getDocumentCode()).toList(), authorization);
        }
        throw new IllegalArgumentException("不支持的导出场景");
    }

    private Set<String> allowedDocuments(Long userId, List<String> codes,
                                         QueryAuthorization authorization) {
        Set<String> result = new HashSet<>();
        for (String code : codes) {
            if (code == null) continue;
            ProcessDocumentInstance doc = documentMapper.selectOne(
                    Wrappers.<ProcessDocumentInstance>lambdaQuery()
                            .eq(ProcessDocumentInstance::getDocumentCode, code).last("LIMIT 1"));
            if (doc != null && authorization.allows(doc.getCompanyId(), doc.getSubmitterUserId())) {
                result.add(code);
            }
        }
        return result;
    }

    private Set<String> codes(List<String> codes) {
        Set<String> result = new HashSet<>();
        for (String code : codes) if (code != null && !code.isBlank()) result.add(code);
        return result;
    }

    private String permissionCode(ExpenseExportSubmitDTO payload) {
        if (payload == null || payload.getScene() == null) {
            throw new IllegalArgumentException("导出场景不能为空");
        }
        return switch (payload.getScene().trim().toUpperCase()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> ExpenseDocumentReadAuthorization.MY_DOCUMENTS;
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> ExpenseDocumentReadAuthorization.APPROVAL;
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> ExpenseDocumentReadAuthorization.DOCUMENT_SEARCH;
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PAYMENT_PENDING -> ExpenseDocumentReadAuthorization.PAYMENT_ORDER;
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> "dashboard:view";
            default -> throw new IllegalArgumentException("不支持的导出场景");
        };
    }

    private String digest(List<String> sourceCodes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String code : sourceCodes.stream().sorted().toList()) {
                digest.update(code.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    public record Snapshot(String permissionCode, String authDigest,
                           String queryJson, String sourceDigest) { }
}
