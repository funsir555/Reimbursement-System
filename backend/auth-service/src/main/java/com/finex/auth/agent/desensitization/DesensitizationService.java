package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脱敏服务 - 正则规则实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesensitizationService {

    private final DesensitizationConfig config;
    private final MappingTableRepository mappingTableRepository;
    private final EntityTypeConfigRepository entityTypeConfigRepository;
    private final ObjectMapper objectMapper;

    // 内存缓存映射表（临时方案，生产环境应使用 Redis）
    private final Map<String, MappingTable> mappingCache = new ConcurrentHashMap<>();

    /**
     * 脱敏处理
     */
    public DesensitizedData desensitize(String sessionId, String text) {
        return desensitize(sessionId, text, null);
    }

    /**
     * 按指定策略脱敏。
     * v3.0 起脱敏统一走可审计的正则规则；requestedStrategy 仅保留接口兼容。
     */
    public DesensitizedData desensitize(String sessionId, String text, String requestedStrategy) {
        if (text == null || text.isEmpty()) {
            return DesensitizedData.builder()
                    .maskedText(text)
                    .success(true)
                    .strategy("NONE")
                    .build();
        }

        log.debug("开始脱敏处理: sessionId={}, textLength={}", sessionId, text.length());

        try {
            DesensitizedData result = regexDesensitization(sessionId, text);

            if (result != null && result.isSuccess()) {
                log.info("正则规则脱敏成功: sessionId={}", sessionId);
                return result;
            }

            // 如果都失败，返回原文（记录告警）
            log.error("所有脱敏策略均失败，返回原文: sessionId={}", sessionId);
            return DesensitizedData.builder()
                    .maskedText(text)
                    .success(false)
                    .errorMessage("所有脱敏策略均失败")
                    .build();

        } catch (Exception e) {
            log.error("脱敏处理异常: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return DesensitizedData.builder()
                    .maskedText(text)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 对结构化 JSON 中已知敏感字段先做确定性脱敏，再执行通用正则规则。
     * 字段值整体映射可以覆盖金额数值、标题内姓名等无法可靠依赖正则识别的场景。
     */
    public DesensitizedData desensitizeJsonFields(
            String sessionId,
            String json,
            Map<String, String> fieldPlaceholderPrefixes
    ) {
        return desensitizeJsonFields(sessionId, json, fieldPlaceholderPrefixes, Set.of());
    }

    public DesensitizedData desensitizeJsonFields(
            String sessionId,
            String json,
            Map<String, String> fieldPlaceholderPrefixes,
            Set<String> propagatedPrefixes
    ) {
        if (json == null || json.isBlank()) {
            return DesensitizedData.builder()
                    .maskedText(json)
                    .success(true)
                    .strategy("NONE")
                    .build();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            MappingTable mappingTable = createMappingTable(sessionId);
            Map<String, Integer> counters = new HashMap<>();
            Map<String, String> normalizedFields = new HashMap<>();
            if (fieldPlaceholderPrefixes != null) {
                fieldPlaceholderPrefixes.forEach((field, prefix) -> {
                    if (field != null && !field.isBlank()) {
                        normalizedFields.put(field.toLowerCase(Locale.ROOT), normalizePrefix(prefix));
                    }
                });
            }

            maskJsonFields(root, normalizedFields, mappingTable, counters);
            String fieldMaskedJson = propagateMappedValues(
                    objectMapper.writeValueAsString(root),
                    mappingTable,
                    propagatedPrefixes
            );
            String maskedText = applyRegexPatterns(fieldMaskedJson, mappingTable, counters);
            return saveResult(mappingTable, maskedText, "JSON_FIELDS_AND_REGEX");
        } catch (Exception e) {
            log.error("结构化 JSON 脱敏失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return DesensitizedData.builder()
                    .maskedText(null)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 正则规则脱敏
     */
    private DesensitizedData regexDesensitization(String sessionId, String text) {
        MappingTable mappingTable = createMappingTable(sessionId);
        Map<String, Integer> counters = new HashMap<>();
        String maskedText = applyRegexPatterns(text, mappingTable, counters);
        return saveResult(mappingTable, maskedText, "REGEX");
    }

    private MappingTable createMappingTable(String sessionId) {
        MappingTable mappingTable = new MappingTable();
        mappingTable.setId(UUID.randomUUID().toString());
        mappingTable.setSessionId(sessionId);
        mappingTable.setCreatedAt(Instant.now());
        mappingTable.setExpiresAt(Instant.now().plus(config.getMapping().getTtlHours(), ChronoUnit.HOURS));
        return mappingTable;
    }

    private String applyRegexPatterns(
            String text,
            MappingTable mappingTable,
            Map<String, Integer> counters
    ) {
        String maskedText = text;
        // 按优先级处理每种实体类型
        for (DesensitizationConfig.EntityType entityType : getEnabledEntityTypes()) {
            if (!Boolean.TRUE.equals(entityType.getEnabled())) {
                continue;
            }

            for (String regexPattern : getEffectivePatterns(entityType)) {
                try {
                    Pattern pattern = Pattern.compile(regexPattern);
                    Matcher matcher = pattern.matcher(maskedText);

                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        String originalValue = matcher.group();

                        // 检查是否已经映射过
                        String placeholder = mappingTable.getPlaceholder(originalValue);
                        if (placeholder == null) {
                            placeholder = nextPlaceholder(entityType.getPlaceholderPrefix(), counters);
                            mappingTable.addMapping(placeholder, originalValue);
                        }

                        matcher.appendReplacement(sb, Matcher.quoteReplacement("[" + placeholder + "]"));
                    }
                    matcher.appendTail(sb);
                    maskedText = sb.toString();

                } catch (Exception e) {
                    log.warn("正则匹配失败: pattern={}, error={}", regexPattern, e.getMessage());
                }
            }
        }
        return maskedText;
    }

    private DesensitizedData saveResult(MappingTable mappingTable, String maskedText, String strategy) {
        String mappingId = saveMappingTable(mappingTable);
        return DesensitizedData.builder()
                .maskedText(maskedText)
                .mappingId(mappingId)
                .strategy(strategy)
                .entityMappings(mappingTable.getPlaceholderToOriginal())
                .success(true)
                .build();
    }

    private void maskJsonFields(
            JsonNode node,
            Map<String, String> fieldPrefixes,
            MappingTable mappingTable,
            Map<String, Integer> counters
    ) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> maskJsonFields(child, fieldPrefixes, mappingTable, counters));
            return;
        }
        if (!node.isObject()) {
            return;
        }

        ObjectNode objectNode = (ObjectNode) node;
        List<String> fieldNames = new ArrayList<>();
        objectNode.fieldNames().forEachRemaining(fieldNames::add);
        for (String fieldName : fieldNames) {
            JsonNode value = objectNode.get(fieldName);
            String prefix = fieldPrefixes.get(fieldName.toLowerCase(Locale.ROOT));
            if (prefix != null && value != null && value.isValueNode() && !value.isNull()) {
                String originalValue = value.asText();
                if (!originalValue.isBlank()) {
                    String placeholder = mappingTable.getPlaceholder(originalValue);
                    if (placeholder == null) {
                        placeholder = nextPlaceholder(prefix, counters);
                        mappingTable.addMapping(placeholder, originalValue);
                    }
                    objectNode.put(fieldName, "[" + placeholder + "]");
                }
                continue;
            }
            maskJsonFields(value, fieldPrefixes, mappingTable, counters);
        }
    }

    private String nextPlaceholder(String rawPrefix, Map<String, Integer> counters) {
        String prefix = normalizePrefix(rawPrefix);
        int counter = counters.getOrDefault(prefix, 0) + 1;
        counters.put(prefix, counter);
        return String.format("%s_%03d", prefix, counter);
    }

    private String propagateMappedValues(
            String text,
            MappingTable mappingTable,
            Set<String> rawPrefixes
    ) {
        if (rawPrefixes == null || rawPrefixes.isEmpty()) {
            return text;
        }
        Set<String> prefixes = rawPrefixes.stream()
                .filter(Objects::nonNull)
                .map(this::normalizePrefix)
                .collect(java.util.stream.Collectors.toSet());
        String result = text;
        List<Map.Entry<String, String>> mappings = mappingTable.getPlaceholderToOriginal().entrySet().stream()
                .filter(entry -> prefixes.stream().anyMatch(prefix -> entry.getKey().startsWith(prefix + "_")))
                .sorted((left, right) -> Integer.compare(right.getValue().length(), left.getValue().length()))
                .toList();
        for (Map.Entry<String, String> entry : mappings) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                result = result.replace(entry.getValue(), "[" + entry.getKey() + "]");
            }
        }
        return result;
    }

    private String normalizePrefix(String rawPrefix) {
        if (rawPrefix == null || rawPrefix.isBlank()) {
            return "FIELD";
        }
        String normalized = rawPrefix.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        return normalized.isBlank() ? "FIELD" : normalized;
    }

    /**
     * 数据库配置是运行时配置的唯一来源；数据库不可用或尚未初始化时回退到 classpath 默认配置。
     */
    private List<DesensitizationConfig.EntityType> getEnabledEntityTypes() {
        try {
            List<DesensitizationDTO.EntityTypeDTO> databaseTypes = entityTypeConfigRepository.findAll()
                    .stream()
                    .map(entityTypeConfigRepository::toDto)
                    .toList();
            if (!databaseTypes.isEmpty()) {
                return databaseTypes.stream()
                        .filter(entityType -> Boolean.TRUE.equals(entityType.getEnabled()))
                        .map(this::toConfigEntityType)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("读取数据库脱敏实体类型失败，回退到默认配置: {}", e.getMessage());
        }

        if (config.getEntityTypes() == null) {
            return List.of();
        }
        return config.getEntityTypes().stream()
                .filter(entityType -> Boolean.TRUE.equals(entityType.getEnabled()))
                .toList();
    }

    private DesensitizationConfig.EntityType toConfigEntityType(DesensitizationDTO.EntityTypeDTO dto) {
        DesensitizationConfig.EntityType entityType = new DesensitizationConfig.EntityType();
        entityType.setCode(dto.getCode());
        entityType.setName(dto.getName());
        entityType.setDescription(dto.getDescription());
        entityType.setPlaceholderPrefix(dto.getPlaceholderPrefix());
        entityType.setRegexPatterns(dto.getRegexPatterns());
        entityType.setExamples(dto.getExamples());
        entityType.setEnabled(dto.getEnabled());
        return entityType;
    }

    /**
     * 泛中文短语规则会把“提交了报销单”等普通内容误判为人名。
     * 对历史默认规则做兼容收紧，保留常见姓名后接语义词的场景。
     */
    private List<String> getEffectivePatterns(DesensitizationConfig.EntityType entityType) {
        if (!"PERSON".equalsIgnoreCase(entityType.getCode())
                || entityType.getRegexPatterns() == null) {
            return entityType.getRegexPatterns() == null ? List.of() : entityType.getRegexPatterns();
        }

        return entityType.getRegexPatterns().stream()
                .map(pattern -> isBroadChinesePersonPattern(pattern)
                        ? "(?<![\\u4e00-\\u9fff])[\\u4e00-\\u9fff]{2,4}(?=(?:的|提交|发起|创建|审批|上传|填写|查看|查询|修改|删除|收到|联系|负责|报销|申请))"
                        : pattern)
                .toList();
    }

    private boolean isBroadChinesePersonPattern(String pattern) {
        if (pattern == null) {
            return false;
        }
        String normalized = pattern.replace("\\\\", "\\").replace(" ", "");
        return "[一-龥]{2,4}".equals(normalized)
                || "[\\u4e00-\\u9fff]{2,4}".equals(normalized);
    }

    /**
     * 还原文本
     */
    public String restore(String mappingId, String maskedText) {
        if (maskedText == null || maskedText.isEmpty()) {
            return maskedText;
        }

        // 从缓存或数据库获取映射表
        MappingTable mappingTable = mappingCache.get(mappingId);
        if (mappingTable == null) {
            mappingTable = mappingTableRepository.findById(mappingId);
            if (mappingTable != null) {
                mappingCache.put(mappingId, mappingTable);
            }
        }

        if (mappingTable == null) {
            log.error("映射表未找到: mappingId={}", mappingId);
            return maskedText;
        }

        if (mappingTable.isExpired()) {
            log.warn("映射表已过期: mappingId={}", mappingId);
            return maskedText;
        }

        return mappingTable.restore(maskedText);
    }

    /**
     * 保存映射表
     */
    private String saveMappingTable(MappingTable mappingTable) {
        // 保存到数据库
        mappingTableRepository.save(mappingTable);

        // 缓存
        mappingCache.put(mappingTable.getId(), mappingTable);

        return mappingTable.getId();
    }

    /**
     * 清理过期映射表
     */
    public void cleanupExpiredMappings() {
        log.info("开始清理过期映射表");
        int cleaned = 0;

        Iterator<Map.Entry<String, MappingTable>> iterator = mappingCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MappingTable> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                cleaned++;
            }
        }

        // 清理数据库
        int dbCleaned = mappingTableRepository.deleteExpired(Instant.now());

        log.info("清理完成: 内存={}, 数据库={}", cleaned, dbCleaned);
    }
}
