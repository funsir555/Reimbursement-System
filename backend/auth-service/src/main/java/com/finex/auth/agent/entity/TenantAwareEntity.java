package com.finex.auth.agent.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.util.SecurityUtils;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 租户感知基础实体
 * 所有 Agent 相关实体继承此类
 */
@Data
public abstract class TenantAwareEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 公司ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long companyId;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updatedAt;

    /**
     * 是否删除
     */
    private Boolean deleted;

    /**
     * 删除时间
     */
    private Instant deletedAt;

    /**
     * 删除人
     */
    private Long deletedBy;

    /**
     * 追踪ID
     */
    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    /**
     * 版本号（乐观锁）
     */
    private Integer versionNo;

    /**
     * 插入前自动填充
     */
    public void prePersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getEffectiveTenantId();
        }
        if (this.companyId == null) {
            this.companyId = TenantContext.getEffectiveCompanyId();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.createdBy == null) {
            this.createdBy = SecurityUtils.getCurrentUserId();
        }
        if (this.traceId == null) {
            this.traceId = TraceContext.getCurrentTraceId();
        }
        if (this.deleted == null) {
            this.deleted = false;
        }
        if (this.versionNo == null) {
            this.versionNo = 0;
        }
    }

    /**
     * 更新前自动填充
     */
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtils.getCurrentUserId();
        if (this.versionNo != null) {
            this.versionNo++;
        }
    }
}
