package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体类型配置仓储
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EntityTypeConfigRepository {

    private final EntityTypeConfigMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存配置
     */
    public void save(DesensitizationDTO.SaveEntityTypeRequest request) {
        String tenantId = TenantContext.getEffectiveTenantId();
        String companyId = TenantContext.getEffectiveCompanyId();

        EntityTypeConfigEntity entity = mapper.findByCode(request.getCode(), tenantId, companyId);

        if (entity == null) {
            // 新增
            entity = new EntityTypeConfigEntity();
            entity.setTenantId(tenantId);
            entity.setCompanyId(companyId);
            entity.setCode(request.getCode());
            entity.setCreatedAt(Instant.now());
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPlaceholderPrefix(request.getPlaceholderPrefix());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        entity.setUpdatedAt(Instant.now());

        try {
            entity.setRegexPatterns(objectMapper.writeValueAsString(request.getRegexPatterns()));
            entity.setExamples(objectMapper.writeValueAsString(request.getExamples()));
        } catch (Exception e) {
            log.error("序列化配置失败", e);
            throw new RuntimeException("序列化配置失败", e);
        }

        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    /**
     * 更新启用状态
     */
    public void updateEnabled(String code, Boolean enabled) {
        String tenantId = TenantContext.getEffectiveTenantId();
        String companyId = TenantContext.getEffectiveCompanyId();
        mapper.updateEnabled(code, tenantId, companyId, enabled);
    }

    /**
     * 删除配置
     */
    public void deleteByCode(String code) {
        String tenantId = TenantContext.getEffectiveTenantId();
        String companyId = TenantContext.getEffectiveCompanyId();
        mapper.deleteByCode(code, tenantId, companyId);
    }

    /**
     * 查询所有启用的配置
     */
    public List<EntityTypeConfigEntity> findAllEnabled() {
        String tenantId = TenantContext.getEffectiveTenantId();
        String companyId = TenantContext.getEffectiveCompanyId();
        return mapper.findAllEnabled(tenantId, companyId);
    }

    /**
     * 查询所有配置
     */
    public List<EntityTypeConfigEntity> findAll() {
        String tenantId = TenantContext.getEffectiveTenantId();
        String companyId = TenantContext.getEffectiveCompanyId();
        return mapper.findAll(tenantId, companyId);
    }

    public DesensitizationDTO.EntityTypeDTO toDto(EntityTypeConfigEntity entity) {
        DesensitizationDTO.EntityTypeDTO dto = new DesensitizationDTO.EntityTypeDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPlaceholderPrefix(entity.getPlaceholderPrefix());
        dto.setRegexPatterns(parseStringList(entity.getRegexPatterns()));
        dto.setExamples(parseStringList(entity.getExamples()));
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        return dto;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("解析脱敏配置 JSON 数组失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
