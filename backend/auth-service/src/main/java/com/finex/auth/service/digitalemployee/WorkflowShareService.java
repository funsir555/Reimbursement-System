package com.finex.auth.service.digitalemployee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.workflow.WorkflowSelectorService;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import com.finex.auth.agent.workflow.WorkflowGraphPlan;
import com.finex.auth.agent.workflow.ExpenseDocumentControlledQueryTool;
import com.finex.auth.agent.workflow.FinanceUnclosedCompanyTool;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.entity.ArchiveAgentDefinition;
import com.finex.auth.entity.ArchiveAgentVersion;
import com.finex.auth.mapper.ArchiveAgentDefinitionMapper;
import com.finex.auth.mapper.ArchiveAgentVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Enterprise approval of a specific published workbench version. */
@Service
@RequiredArgsConstructor
public class WorkflowShareService {
    private final JdbcTemplate jdbc;
    private final ArchiveAgentDefinitionMapper definitionMapper;
    private final ArchiveAgentVersionMapper versionMapper;
    private final WorkflowSelectorService selector;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> submit(Long agentId, Long versionId, Long actorId) {
        ArchiveAgentDefinition definition = definitionMapper.selectById(agentId);
        ArchiveAgentVersion version = versionMapper.selectById(versionId);
        if (definition == null || version == null || !actorId.equals(definition.getOwnerUserId())
                || !agentId.equals(version.getAgentId()) || version.getPublished() == null
                || version.getPublished() != 1) throw new SecurityException("只能提交本人已发布的固定版本");
        String permissionCode;
        List<String> permissions;
        try {
            Map<String, Object> config = objectMapper.readValue(version.getConfigJson(),
                    new TypeReference<LinkedHashMap<String, Object>>() { });
            if (!selector.containsControlledTool(config)) throw new IllegalArgumentException("仅只读受控查询工作流可供共享");
            permissions = selector.selectConfigured(definition, version, config, null, null)
                    .getQueryPermissionCodes();
            if (permissions == null || permissions.isEmpty())
                throw new IllegalArgumentException("该版本没有受控查询能力声明");
            permissionCode = permissions.get(0);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("工作流配置无效", exception);
        }
        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id,status FROM ea_workflow_share WHERE agent_version_id=?", versionId);
        if (!existing.isEmpty()) {
            if (!"REVOKED".equals(existing.get(0).get("status")))
                throw new IllegalStateException("该固定版本已提交共享");
            Long shareId = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("""
                    UPDATE ea_workflow_share SET status='SUBMITTED',permission_code=?,reviewer_user_id=NULL,
                    reviewed_at=NULL,submitted_at=NOW() WHERE id=?
                    """, permissionCode, shareId);
            syncCapabilities(shareId, permissions);
            audit(shareId, actorId, "RESUBMIT");
            return requireShare(shareId);
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ea_workflow_share (agent_id,agent_version_id,author_user_id,permission_code,status)
                    VALUES (?,?,?,?,'SUBMITTED')
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, agentId);
            statement.setLong(2, versionId);
            statement.setLong(3, actorId);
            statement.setString(4, permissionCode);
            return statement;
        }, keys);
        Long shareId = keys.getKey().longValue();
        syncCapabilities(shareId, permissions);
        audit(shareId, actorId, "SUBMIT");
        return requireShare(shareId);
    }

    @Transactional
    public Map<String, Object> review(Long shareId, boolean approve, Long actorId) {
        Map<String, Object> share = requireShare(shareId);
        if (!"SUBMITTED".equals(share.get("status"))) throw new IllegalStateException("仅待审核版本可审核");
        int updated = jdbc.update("""
                UPDATE ea_workflow_share SET status=?,reviewer_user_id=?,reviewed_at=NOW()
                WHERE id=? AND status='SUBMITTED'
                """, approve ? "APPROVED" : "REJECTED", actorId, shareId);
        if (updated != 1) throw new IllegalStateException("共享状态已变化，请刷新后重试");
        audit(shareId, actorId, approve ? "APPROVE" : "REJECT");
        return requireShare(shareId);
    }

    @Transactional
    public Map<String, Object> revoke(Long shareId, Long actorId) {
        Map<String, Object> share = requireShare(shareId);
        if (!"APPROVED".equals(share.get("status"))) throw new IllegalStateException("仅已批准版本可撤销");
        int updated = jdbc.update("UPDATE ea_workflow_share SET status='REVOKED',reviewer_user_id=?,reviewed_at=NOW() WHERE id=? AND status='APPROVED'",
                actorId, shareId);
        if (updated != 1) throw new IllegalStateException("共享状态已变化，请刷新后重试");
        audit(shareId, actorId, "REVOKE");
        return requireShare(shareId);
    }

    public List<Map<String, Object>> catalog(boolean includePending) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT sh.id,sh.agent_id,sh.agent_version_id,sh.author_user_id,sh.reviewer_user_id,
                       sh.permission_code,
                       sh.status,sh.submitted_at,sh.reviewed_at,d.agent_name,v.version_no,v.version_label,v.config_json
                FROM ea_workflow_share sh JOIN ea_agent_definition d ON d.id=sh.agent_id
                JOIN ea_agent_version v ON v.id=sh.agent_version_id
                WHERE (?=1 OR sh.status='APPROVED')
                ORDER BY sh.submitted_at DESC,sh.id DESC
                """, includePending ? 1 : 0);
        for (Map<String, Object> row : rows) {
            row.put("permission_codes", capabilities(((Number) row.get("id")).longValue()));
            try {
                Map<String, Object> config = objectMapper.readValue(String.valueOf(row.remove("config_json")),
                        new TypeReference<LinkedHashMap<String, Object>>() { });
                Object workflow = config.get("workflow");
                List<Map<String, Object>> nodes = WorkflowGraphPlan.from(
                        workflow instanceof Map<?, ?> graph ? castMap(graph) : Map.of())
                        .requireControlledQueryPlan().orderedNodes();
                row.put("tool_nodes", nodes.stream().filter(node -> "tool".equalsIgnoreCase(
                        String.valueOf(node.get("nodeType")))).map(node -> {
                    String code = String.valueOf(WorkflowGraphPlan.nodeConfig(node).get("toolCode"));
                    return Map.of("nodeKey", String.valueOf(node.get("nodeKey")),
                            "toolCode", code, "fields", ControlledQueryCatalog.require(code).metadata().get("testFields"));
                }).toList());
            } catch (Exception exception) { row.put("tool_nodes", List.of()); }
        }
        return rows;
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    public List<String> capabilities(Long shareId) {
        return jdbc.queryForList("SELECT permission_code FROM ea_workflow_share_capability WHERE share_id=? ORDER BY permission_code",
                String.class, shareId);
    }

    private void syncCapabilities(Long shareId, List<String> permissions) {
        jdbc.update("DELETE FROM ea_workflow_share_capability WHERE share_id=?", shareId);
        for (String code : permissions) jdbc.update("""
                INSERT INTO ea_workflow_share_capability (share_id,permission_code) VALUES (?,?)
                """, shareId, code);
    }

    private Map<String, Object> requireShare(Long shareId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ea_workflow_share WHERE id=?", shareId);
        if (rows.size() != 1) throw new IllegalArgumentException("共享版本不存在");
        return rows.get(0);
    }

    private void audit(Long shareId, Long actorId, String action) {
        jdbc.update("INSERT INTO ea_workflow_share_audit (share_id,actor_user_id,action_code) VALUES (?,?,?)",
                shareId, actorId, action);
    }
}
