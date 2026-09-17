package com.finex.auth.agent.desensitization;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 脱敏映射表实体
 */
@Data
@TableName(value = "ea_desensitization_mapping", autoResultMap = true)
public class DesensitizationMappingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 映射ID（UUID）
     */
    private String mappingId;

    /**
     * 映射关系（JSON）
     */
    private String mappings;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 过期时间
     */
    private Instant expiresAt;

    /**
     * 转换为映射表对象
     */
    public MappingTable toMappingTable() {
        MappingTable table = new MappingTable();
        table.setId(this.mappingId);
        table.setSessionId(this.sessionId);
        table.setCreatedAt(this.createdAt);
        table.setExpiresAt(this.expiresAt);

        // 解析 JSON 映射
        if (this.mappings != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> mappingMap = mapper.readValue(this.mappings, Map.class);
                for (Map.Entry<String, String> entry : mappingMap.entrySet()) {
                    table.addMapping(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                // Log error
            }
        }

        return table;
    }

    /**
     * 从映射表对象创建
     */
    public static DesensitizationMappingEntity fromMappingTable(MappingTable table, String tenantId) {
        DesensitizationMappingEntity entity = new DesensitizationMappingEntity();
        entity.setTenantId(tenantId);
        entity.setSessionId(table.getSessionId());
        entity.setMappingId(table.getId());
        entity.setCreatedAt(table.getCreatedAt());
        entity.setExpiresAt(table.getExpiresAt());

        // 序列化映射为 JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            entity.setMappings(mapper.writeValueAsString(table.getPlaceholderToOriginal()));
        } catch (Exception e) {
            // Log error
        }

        return entity;
    }
}
