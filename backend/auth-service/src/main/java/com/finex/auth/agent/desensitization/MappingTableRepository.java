package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 映射表仓储
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MappingTableRepository {

    private final DesensitizationMappingMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 保存映射表
     */
    public void save(MappingTable mappingTable) {
        String tenantId = TenantContext.getEffectiveTenantId();
        DesensitizationMappingEntity entity = DesensitizationMappingEntity.fromMappingTable(mappingTable, tenantId, objectMapper);
        mapper.insert(entity);
        log.debug("保存映射表: mappingId={}, sessionId={}", mappingTable.getId(), mappingTable.getSessionId());
    }

    /**
     * 根据映射ID查询
     */
    public MappingTable findById(String mappingId) {
        DesensitizationMappingEntity entity = mapper.findByMappingId(mappingId);
        if (entity == null) {
            return null;
        }
        return entity.toMappingTable(objectMapper);
    }

    /**
     * 根据会话ID查询所有映射表
     */
    public List<MappingTable> findBySessionId(String sessionId) {
        List<DesensitizationMappingEntity> entities = mapper.findBySessionId(sessionId);
        return entities.stream()
                .map(entity -> entity.toMappingTable(objectMapper))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询映射表
     */
    public List<MappingTable> findPage(String sessionId, int page, int pageSize) {
        String tenantId = TenantContext.getEffectiveTenantId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(Math.min(pageSize, 100), 1);
        int offset = (safePage - 1) * safePageSize;

        List<DesensitizationMappingEntity> entities = mapper.findPage(tenantId, sessionId, offset, safePageSize);
        return entities.stream()
                .map(entity -> entity.toMappingTable(objectMapper))
                .collect(Collectors.toList());
    }

    /**
     * 统计映射表数量
     */
    public int count(String sessionId) {
        String tenantId = TenantContext.getEffectiveTenantId();
        return mapper.countByTenant(tenantId, sessionId);
    }

    public int count() {
        return count(null);
    }

    /**
     * 统计有效映射数量
     */
    public int countActive(Instant now) {
        String tenantId = TenantContext.getEffectiveTenantId();
        return mapper.countActive(tenantId, now);
    }

    /**
     * 删除过期映射
     */
    public int deleteExpired(Instant now) {
        return mapper.deleteExpired(now);
    }

    /**
     * 统计过期映射数量
     */
    public int countExpired(Instant now) {
        String tenantId = TenantContext.getEffectiveTenantId();
        return mapper.countExpired(tenantId, now);
    }
}
