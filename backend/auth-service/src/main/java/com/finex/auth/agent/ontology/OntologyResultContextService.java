package com.finex.auth.agent.ontology;

import com.finex.auth.agent.workflow.SelectedWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Approved JSON field semantics for controlled query results; physical index evidence stays in SQL. */
@Service
@RequiredArgsConstructor
public class OntologyResultContextService {
    private final JdbcTemplate jdbc;
    private final BusinessOntologyService ontology;

    public record ResultLink(String sourceToolCode, String sourceField, String sourceLabel,
                             String targetToolCode, String targetField, String targetLabel,
                             String relationKind, String explanation, String scopeNote, int versionNo) { }

    public List<ResultLink> listPublished() {
        return jdbc.query("""
                SELECT source_tool_code,source_field,source_label,target_tool_code,target_field,target_label,
                       relation_kind,explanation,scope_note,version_no
                FROM ea_ontology_result_link current_link
                WHERE current_link.status='PUBLISHED' AND current_link.effective_from<=CURRENT_DATE
                  AND (current_link.effective_until IS NULL OR current_link.effective_until>=CURRENT_DATE)
                  AND NOT EXISTS (
                    SELECT 1 FROM ea_ontology_result_link newer
                    WHERE newer.link_key=current_link.link_key AND newer.version_no>current_link.version_no
                      AND newer.status='PUBLISHED' AND newer.effective_from<=CURRENT_DATE
                      AND (newer.effective_until IS NULL OR newer.effective_until>=CURRENT_DATE))
                ORDER BY current_link.link_key
                """, (rs, row) -> new ResultLink(rs.getString("source_tool_code"),
                rs.getString("source_field"), rs.getString("source_label"),
                rs.getString("target_tool_code"), rs.getString("target_field"),
                rs.getString("target_label"), rs.getString("relation_kind"),
                rs.getString("explanation"), rs.getString("scope_note"), rs.getInt("version_no")));
    }

    /** Only describe fields actually present in an authorized tool result. Never supply data or SQL. */
    public Map<String, Object> forAnalysis(String question, SelectedWorkflow workflow,
                                           List<Map<String, Object>> toolResults) {
        Map<String, Set<String>> fields = new HashMap<>();
        Map<String, Set<String>> companies = new HashMap<>();
        for (Map<String, Object> result : toolResults) {
            Object code = result.get("toolCode");
            if (!(code instanceof String toolCode)) continue;
            if (result.get("queryDescriptor") instanceof Map<?, ?> descriptor
                    && descriptor.get("companyId") instanceof String companyId && !companyId.isBlank())
                companies.computeIfAbsent(toolCode, ignored -> new HashSet<>()).add(companyId);
            Object records = result.get("records");
            if (!(records instanceof List<?> rows)) continue;
            Set<String> names = fields.computeIfAbsent(toolCode, ignored -> new HashSet<>());
            for (Object row : rows) {
                if (row instanceof Map<?, ?> values) {
                    for (Object key : values.keySet()) if (key instanceof String name) names.add(name);
                }
            }
        }
        List<Map<String, Object>> links = listPublished().stream()
                .filter(link -> fields.getOrDefault(link.sourceToolCode(), Set.of()).contains(link.sourceField()))
                .limit(16)
                .map(link -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("sourceTool", link.sourceToolCode());
                    value.put("sourceField", link.sourceField());
                    value.put("sourceLabel", link.sourceLabel());
                    value.put("targetTool", link.targetToolCode());
                    value.put("targetField", link.targetField());
                    value.put("targetLabel", link.targetLabel());
                    value.put("relation", link.relationKind());
                    value.put("meaning", link.explanation());
                    value.put("scope", link.scopeNote());
                    value.put("version", link.versionNo());
                    value.put("targetInResult", fields.getOrDefault(link.targetToolCode(), Set.of())
                            .contains(link.targetField()));
                    Set<String> sourceCompanies = companies.getOrDefault(link.sourceToolCode(), Set.of());
                    value.put("comparableScope", sourceCompanies.size() == 1
                            && sourceCompanies.equals(companies.getOrDefault(link.targetToolCode(), Set.of())));
                    return value;
                })
                .toList();
        return Map.of("terms", ontology.modelContext(question, List.of(workflow)),
                "fieldRelations", links,
                "interpretationRule", "字段关系仅用于解释当前已授权查询结果；目标字段未出现或 comparableScope=false 时不得推断目标记录。期间不一致时不得直接合并；不得生成或执行 SQL。");
    }
}
