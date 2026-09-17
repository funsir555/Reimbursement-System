package com.finex.auth.agent.desensitization;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 实体类型配置实体
 */
@Data
@TableName("ea_desensitization_entity_type")
public class EntityTypeConfigEntity implements Serializable {

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
     * 公司ID
     */
    private Long companyId;

    /**
     * 实体类型编码
     */
    private String code;

    /**
     * 实体类型名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 占位符前缀
     */
    private String placeholderPrefix;

    /**
     * 正则表达式（JSON 数组）
     */
    private String regexPatterns;

    /**
     * 示例（JSON 数组）
     */
    private String examples;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
