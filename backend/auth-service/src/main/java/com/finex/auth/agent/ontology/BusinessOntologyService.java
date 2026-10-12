package com.finex.auth.agent.ontology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.workflow.ControlledQueryCatalog;
import com.finex.auth.agent.workflow.SelectedWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Curated product semantics. This service never reads business records or schema bindings. */
@Service
@RequiredArgsConstructor
public class BusinessOntologyService {
    private static final Set<String> KINDS = Set.of("ENTITY", "FIELD", "ENUM", "RELATION", "RULE", "METRIC", "CAPABILITY");
    private static final Set<String> DOMAINS = Set.of("EXPENSE", "FINANCE", "PROCESS", "SYSTEM", "AGENT");
    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9_.-]{2,127}");
    private static final Pattern PRIVATE_VALUE = Pattern.compile(
            "(?i)(?:[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}|(?<!\\d)1[3-9]\\d{9}(?!\\d)|(?<!\\d)\\d{12,}(?!\\d)|(?:api[_-]?key|secret|token|密码|密钥)\\s*[:=：]\\s*\\S+)");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public record Item(Long id, String itemKey, String kind, String entityKey, String displayName,
                       String description, List<String> aliases, String valueCode, String targetKey,
                       String cardinality, String semanticType, String unitName, String toolCode,
                       String ownerDomain, String sourceRef, int versionNo, String status,
                       LocalDate effectiveFrom, LocalDate effectiveUntil, Long createdBy, Long reviewedBy) { }

    public record Draft(String itemKey, String kind, String entityKey, String displayName,
                        String description, List<String> aliases, String valueCode, String targetKey,
                        String cardinality, String semanticType, String unitName, String toolCode,
                        String ownerDomain, String sourceRef, LocalDate effectiveFrom,
                        LocalDate effectiveUntil) { }

    public List<Item> list(String keyword) {
        List<Item> rows = jdbc.query("""
                SELECT id,item_key,kind,entity_key,display_name,description,aliases_json,value_code,
                       target_key,cardinality,semantic_type,unit_name,tool_code,owner_domain,source_ref,
                       version_no,status,effective_from,effective_until,created_by,reviewed_by
                FROM ea_ontology_item ORDER BY item_key,version_no DESC
                """, (rs, index) -> new Item(rs.getLong("id"), rs.getString("item_key"), rs.getString("kind"),
                rs.getString("entity_key"), rs.getString("display_name"), rs.getString("description"),
                parseAliases(rs.getString("aliases_json")), rs.getString("value_code"),
                rs.getString("target_key"), rs.getString("cardinality"), rs.getString("semantic_type"),
                rs.getString("unit_name"), rs.getString("tool_code"), rs.getString("owner_domain"),
                rs.getString("source_ref"), rs.getInt("version_no"), rs.getString("status"),
                rs.getDate("effective_from").toLocalDate(),
                rs.getDate("effective_until") == null ? null : rs.getDate("effective_until").toLocalDate(),
                rs.getLong("created_by"), rs.getObject("reviewed_by", Long.class)));
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        if (term.isEmpty()) return rows;
        return rows.stream().filter(item -> searchable(item).contains(term)).toList();
    }

    @Transactional
    public Item saveDraft(Draft draft, long userId) {
        validate(draft);
        List<Map<String, Object>> latest = jdbc.queryForList("""
                SELECT id,version_no,status FROM ea_ontology_item
                WHERE item_key=? ORDER BY version_no DESC LIMIT 1 FOR UPDATE
                """, draft.itemKey());
        String aliases = aliasesJson(draft.aliases());
        if (!latest.isEmpty() && "DRAFT".equals(latest.get(0).get("status"))) {
            long id = ((Number) latest.get(0).get("id")).longValue();
            jdbc.update("""
                    UPDATE ea_ontology_item SET kind=?,entity_key=?,display_name=?,description=?,
                    aliases_json=?,value_code=?,target_key=?,cardinality=?,semantic_type=?,unit_name=?,
                    tool_code=?,owner_domain=?,source_ref=?,effective_from=?,effective_until=?,
                    created_by=?,reviewed_by=NULL,reviewed_at=NULL WHERE id=? AND status='DRAFT'
                    """, draft.kind(), blankToNull(draft.entityKey()), draft.displayName().trim(),
                    draft.description().trim(), aliases, blankToNull(draft.valueCode()),
                    blankToNull(draft.targetKey()), blankToNull(draft.cardinality()),
                    blankToNull(draft.semanticType()), blankToNull(draft.unitName()),
                    blankToNull(draft.toolCode()), draft.ownerDomain(), draft.sourceRef().trim(),
                    Date.valueOf(draft.effectiveFrom()), date(draft.effectiveUntil()), userId, id);
        } else {
            int next = latest.isEmpty() ? 1 : ((Number) latest.get(0).get("version_no")).intValue() + 1;
            jdbc.update("""
                    INSERT INTO ea_ontology_item
                    (item_key,kind,entity_key,display_name,description,aliases_json,value_code,target_key,
                     cardinality,semantic_type,unit_name,tool_code,owner_domain,source_ref,version_no,status,
                     effective_from,effective_until,created_by)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?,?)
                    """, draft.itemKey(), draft.kind(), blankToNull(draft.entityKey()),
                    draft.displayName().trim(), draft.description().trim(), aliases,
                    blankToNull(draft.valueCode()), blankToNull(draft.targetKey()),
                    blankToNull(draft.cardinality()), blankToNull(draft.semanticType()),
                    blankToNull(draft.unitName()), blankToNull(draft.toolCode()),
                    draft.ownerDomain(), draft.sourceRef().trim(), next,
                    Date.valueOf(draft.effectiveFrom()), date(draft.effectiveUntil()), userId);
        }
        return list(null).stream().filter(item -> item.itemKey().equals(draft.itemKey())
                && "DRAFT".equals(item.status())).findFirst().orElseThrow();
    }

    @Transactional
    public void publish(long id, long reviewerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT item_key,kind,entity_key,target_key,tool_code,status,effective_from,effective_until
                FROM ea_ontology_item
                WHERE id=? FOR UPDATE
                """, id);
        if (rows.isEmpty() || !"DRAFT".equals(rows.get(0).get("status")))
            throw new IllegalArgumentException("只能发布待审核的本体定义");
        Map<String, Object> row = rows.get(0);
        LocalDate today = LocalDate.now();
        LocalDate from = ((Date) row.get("effective_from")).toLocalDate();
        Date untilDate = (Date) row.get("effective_until");
        if (from.isAfter(today) || untilDate != null && untilDate.toLocalDate().isBefore(today))
            throw new IllegalArgumentException("只能发布当前有效期内的定义");
        String kind = String.valueOf(row.get("kind"));
        if ("CAPABILITY".equals(kind)
                && !ControlledQueryCatalog.isRegistered(String.valueOf(row.get("tool_code"))))
            throw new IllegalArgumentException("能力未绑定已注册的受控工具");
        if (!"ENTITY".equals(kind) && row.get("entity_key") != null)
            requirePublishedEntity(String.valueOf(row.get("entity_key")));
        if ("RELATION".equals(kind)) requirePublishedEntity(String.valueOf(row.get("target_key")));
        jdbc.update("UPDATE ea_ontology_item SET status='RETIRED' WHERE item_key=? AND status='PUBLISHED'",
                row.get("item_key"));
        jdbc.update("UPDATE ea_ontology_item SET status='PUBLISHED',reviewed_by=?,reviewed_at=NOW() WHERE id=?",
                reviewerId, id);
    }

    @Transactional
    public void retire(long id, long reviewerId) {
        int changed = jdbc.update("""
                UPDATE ea_ontology_item SET status='RETIRED',reviewed_by=?,reviewed_at=NOW()
                WHERE id=? AND status='PUBLISHED'
                """, reviewerId, id);
        if (changed != 1) throw new IllegalArgumentException("只能停用已发布的本体定义");
    }

    /** Strict DTO projection: no table/column mapping, source reference, tool code or raw schema. */
    public List<Map<String, Object>> modelContext(String question, List<SelectedWorkflow> candidates) {
        String query = question == null ? "" : question.toLowerCase();
        Set<String> allowedTools = new HashSet<>();
        for (SelectedWorkflow candidate : candidates) {
            if (candidate.getToolCodes() != null) allowedTools.addAll(candidate.getToolCodes());
            if (candidate.getToolCode() != null) allowedTools.add(candidate.getToolCode());
        }
        LocalDate today = LocalDate.now();
        List<Item> effective = list(null).stream().filter(item -> "PUBLISHED".equals(item.status())
                && !item.effectiveFrom().isAfter(today)
                && (item.effectiveUntil() == null || !item.effectiveUntil().isBefore(today)))
                .filter(item -> !"CAPABILITY".equals(item.kind()) || allowedTools.contains(item.toolCode()))
                .toList();
        Set<String> activeEntities = new HashSet<>();
        for (Item item : effective) if ("ENTITY".equals(item.kind())) activeEntities.add(item.itemKey());
        List<Item> published = effective.stream()
                .filter(item -> item.entityKey() == null || activeEntities.contains(item.entityKey()))
                .filter(item -> !"RELATION".equals(item.kind()) || activeEntities.contains(item.targetKey()))
                .toList();
        Set<String> matchedEntities = new LinkedHashSet<>();
        for (Item item : published) {
            if (matches(query, item)) {
                String entity = "ENTITY".equals(item.kind()) ? item.itemKey() : item.entityKey();
                if (entity != null) matchedEntities.add(entity);
            }
        }
        List<Item> selected = new ArrayList<>();
        for (Item item : published) {
            if (matches(query, item) || item.entityKey() != null && matchedEntities.contains(item.entityKey())
                    || ("ENTITY".equals(item.kind()) && matchedEntities.contains(item.itemKey())))
                selected.add(item);
        }
        selected.sort(Comparator.comparing((Item item) -> !"ENTITY".equals(item.kind()))
                .thenComparing(Item::itemKey));
        return selected.stream().limit(12).map(item -> {
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("kind", item.kind());
            safe.put("name", item.displayName());
            safe.put("meaning", item.description());
            safe.put("aliases", item.aliases());
            safe.put("version", item.versionNo());
            if (item.entityKey() != null) safe.put("entity", item.entityKey());
            if (item.targetKey() != null) safe.put("targetEntity", item.targetKey());
            if (item.cardinality() != null) safe.put("cardinality", item.cardinality());
            if (item.unitName() != null) safe.put("unit", item.unitName());
            return safe;
        }).toList();
    }

    private void requirePublishedEntity(String key) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ea_ontology_item WHERE item_key=? AND kind='ENTITY'
                  AND status='PUBLISHED' AND effective_from<=CURRENT_DATE
                  AND (effective_until IS NULL OR effective_until>=CURRENT_DATE)
                """, Integer.class, key);
        if (count == null || count == 0) throw new IllegalArgumentException("关联的业务实体尚未发布");
    }

    private void validate(Draft draft) {
        if (draft == null || draft.itemKey() == null || !KEY.matcher(draft.itemKey()).matches())
            throw new IllegalArgumentException("本体编码格式无效");
        if (draft.kind() == null || draft.ownerDomain() == null
                || !KINDS.contains(draft.kind()) || !DOMAINS.contains(draft.ownerDomain()))
            throw new IllegalArgumentException("本体类型或业务域无效");
        if (draft.displayName() == null || draft.displayName().isBlank() || draft.displayName().length() > 120
                || draft.description() == null || draft.description().isBlank() || draft.description().length() > 1200
                || draft.sourceRef() == null || draft.sourceRef().isBlank() || draft.sourceRef().length() > 255
                || draft.effectiveFrom() == null || draft.effectiveUntil() != null
                && draft.effectiveUntil().isBefore(draft.effectiveFrom()))
            throw new IllegalArgumentException("名称、说明、来源或生效日期无效");
        if (draft.aliases() != null && (draft.aliases().size() > 8 || draft.aliases().stream()
                .anyMatch(alias -> alias == null || alias.isBlank() || alias.length() > 60)))
            throw new IllegalArgumentException("别名数量或长度无效");
        if (containsPrivateValue(draft)) throw new IllegalArgumentException("本体内容疑似包含敏感值，请改为通用业务说明");
        if (Set.of("FIELD", "ENUM", "RELATION", "CAPABILITY").contains(draft.kind())
                && (draft.entityKey() == null || !KEY.matcher(draft.entityKey()).matches()))
            throw new IllegalArgumentException("该定义需要有效的所属业务实体编码");
        if ("ENUM".equals(draft.kind()) && (draft.valueCode() == null || draft.valueCode().isBlank()))
            throw new IllegalArgumentException("枚举定义需要业务状态值");
        if ("CAPABILITY".equals(draft.kind()) && (draft.toolCode() == null
                || !ControlledQueryCatalog.isRegistered(draft.toolCode())))
            throw new IllegalArgumentException("能力只能引用已注册的受控工具");
        if ("RELATION".equals(draft.kind()) && (draft.targetKey() == null
                || !KEY.matcher(draft.targetKey()).matches()))
            throw new IllegalArgumentException("业务关系需要目标实体");
    }

    private boolean containsPrivateValue(Draft draft) {
        String text = String.join(" ", draft.displayName(), draft.description(), draft.sourceRef(),
                String.join(" ", draft.aliases() == null ? List.of() : draft.aliases()));
        return PRIVATE_VALUE.matcher(text).find();
    }

    private boolean matches(String question, Item item) {
        if (question.isBlank()) return false;
        if (item.displayName().length() >= 2 && question.contains(item.displayName().toLowerCase())) return true;
        return item.aliases().stream().anyMatch(alias -> alias.length() >= 2
                && question.contains(alias.toLowerCase()));
    }

    private String searchable(Item item) {
        return (item.itemKey() + " " + item.displayName() + " " + item.description() + " "
                + String.join(" ", item.aliases())).toLowerCase();
    }

    private List<String> parseAliases(String source) {
        if (source == null || source.isBlank()) return List.of();
        try { return json.readValue(source, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalStateException("本体别名数据无效", exception); }
    }

    private String aliasesJson(List<String> aliases) {
        try { return json.writeValueAsString(aliases == null ? List.of() : aliases); }
        catch (Exception exception) { throw new IllegalArgumentException("本体别名格式无效", exception); }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private Date date(LocalDate value) { return value == null ? null : Date.valueOf(value); }
}
