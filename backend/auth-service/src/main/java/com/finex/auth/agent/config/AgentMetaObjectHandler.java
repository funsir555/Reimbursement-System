package com.finex.auth.agent.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.finex.auth.agent.context.TenantContext;
import com.finex.auth.agent.context.TraceContext;
import com.finex.auth.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * MyBatis-Plus 字段自动填充配置
 * 为 TenantAwareEntity 自动填充租户、用户、时间等字段
 */
@Slf4j
@Component
public class AgentMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 租户ID
        this.strictInsertFill(metaObject, "tenantId", String.class, TenantContext.getEffectiveTenantId());

        // 公司ID
        this.strictInsertFill(metaObject, "companyId", Long.class, TenantContext.getEffectiveCompanyId());

        // 创建人
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
        }

        // 创建时间
        this.strictInsertFill(metaObject, "createdAt", Instant.class, Instant.now());

        // 追踪ID
        this.strictInsertFill(metaObject, "traceId", String.class, TraceContext.getCurrentTraceId());

        // 删除标记
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);

        // 版本号
        this.strictInsertFill(metaObject, "versionNo", Integer.class, 0);

        log.debug("自动填充插入字段: tenantId={}, companyId={}, userId={}",
                TenantContext.getEffectiveTenantId(),
                TenantContext.getEffectiveCompanyId(),
                currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新人
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
        }

        // 更新时间
        this.strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());

        log.debug("自动填充更新字段: userId={}", currentUserId);
    }
}
