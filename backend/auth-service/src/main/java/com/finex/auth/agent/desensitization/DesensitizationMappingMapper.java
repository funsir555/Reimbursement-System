package com.finex.auth.agent.desensitization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.Instant;
import java.util.List;

/**
 * 脱敏映射表 Mapper
 */
@Mapper
public interface DesensitizationMappingMapper extends BaseMapper<DesensitizationMappingEntity> {

    /**
     * 根据映射ID查询
     */
    @Select("SELECT * FROM ea_desensitization_mapping WHERE mapping_id = #{mappingId} LIMIT 1")
    DesensitizationMappingEntity findByMappingId(@Param("mappingId") String mappingId);

    /**
     * 根据会话ID查询
     */
    @Select("SELECT * FROM ea_desensitization_mapping WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<DesensitizationMappingEntity> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 分页查询
     */
    @Select("""
            <script>
            SELECT * FROM ea_desensitization_mapping
            WHERE tenant_id = #{tenantId}
            <if test='sessionId != null and sessionId != ""'>
              AND session_id = #{sessionId}
            </if>
            ORDER BY created_at DESC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<DesensitizationMappingEntity> findPage(
            @Param("tenantId") String tenantId,
            @Param("sessionId") String sessionId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    /**
     * 统计数量
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM ea_desensitization_mapping
            WHERE tenant_id = #{tenantId}
            <if test='sessionId != null and sessionId != ""'>
              AND session_id = #{sessionId}
            </if>
            </script>
            """)
    int countByTenant(@Param("tenantId") String tenantId, @Param("sessionId") String sessionId);

    /**
     * 统计有效数量
     */
    @Select("SELECT COUNT(*) FROM ea_desensitization_mapping WHERE tenant_id = #{tenantId} AND expires_at >= #{now}")
    int countActive(@Param("tenantId") String tenantId, @Param("now") Instant now);

    /**
     * 删除过期映射
     */
    @Delete("DELETE FROM ea_desensitization_mapping WHERE expires_at < #{now}")
    int deleteExpired(@Param("now") Instant now);

    /**
     * 统计过期映射数量
     */
    @Select("SELECT COUNT(*) FROM ea_desensitization_mapping WHERE tenant_id = #{tenantId} AND expires_at < #{now}")
    int countExpired(@Param("tenantId") String tenantId, @Param("now") Instant now);
}
