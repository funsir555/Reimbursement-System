package com.finex.auth.agent.desensitization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 实体类型配置 Mapper
 */
@Mapper
public interface EntityTypeConfigMapper extends BaseMapper<EntityTypeConfigEntity> {

    /**
     * 根据编码查询
     */
    @Select("SELECT * FROM ea_desensitization_entity_type " +
            "WHERE code = #{code} AND tenant_id = #{tenantId} AND company_id = #{companyId} LIMIT 1")
    EntityTypeConfigEntity findByCode(@Param("code") String code,
                                      @Param("tenantId") String tenantId,
                                      @Param("companyId") String companyId);

    /**
     * 查询所有启用的配置
     */
    @Select("SELECT * FROM ea_desensitization_entity_type " +
            "WHERE enabled = 1 AND tenant_id = #{tenantId} AND company_id = #{companyId} ORDER BY id")
    List<EntityTypeConfigEntity> findAllEnabled(@Param("tenantId") String tenantId,
                                                @Param("companyId") String companyId);

    /**
     * 查询所有配置
     */
    @Select("SELECT * FROM ea_desensitization_entity_type WHERE tenant_id = #{tenantId} AND company_id = #{companyId} ORDER BY id")
    List<EntityTypeConfigEntity> findAll(@Param("tenantId") String tenantId, @Param("companyId") String companyId);

    /**
     * 更新启用状态
     */
    @Update("UPDATE ea_desensitization_entity_type SET enabled = #{enabled}, updated_at = NOW() " +
            "WHERE code = #{code} AND tenant_id = #{tenantId} AND company_id = #{companyId}")
    int updateEnabled(@Param("code") String code,
                      @Param("tenantId") String tenantId,
                      @Param("companyId") String companyId,
                      @Param("enabled") Boolean enabled);

    /**
     * 删除配置
     */
    @Delete("DELETE FROM ea_desensitization_entity_type " +
            "WHERE code = #{code} AND tenant_id = #{tenantId} AND company_id = #{companyId}")
    int deleteByCode(@Param("code") String code,
                     @Param("tenantId") String tenantId,
                     @Param("companyId") String companyId);
}
