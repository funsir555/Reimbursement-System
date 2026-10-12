package com.finex.auth.agent.context;

import com.finex.auth.agent.config.AgentSecurityConfig;
import com.finex.auth.agent.exception.TenantContextMissingException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 租户上下文管理器
 * 提供线程级别的租户、公司、用户上下文
 */
@Slf4j
@Component
public class TenantContext {

    private static AgentSecurityConfig securityConfig;

    private static final ThreadLocal<TenantContextHolder> CONTEXT = new ThreadLocal<>();

    @Autowired
    public void setSecurityConfig(AgentSecurityConfig config) {
        TenantContext.securityConfig = config;
    }

    /**
     * 设置租户ID
     */
    public static void setTenantId(String tenantId) {
        getContextHolder().setTenantId(tenantId);
    }

    /**
     * 获取租户ID
     */
    public static String getTenantId() {
        return getContextHolder().getTenantId();
    }

    /**
     * 设置公司ID
     */
    public static void setCompanyId(String companyId) {
        getContextHolder().setCompanyId(companyId);
    }

    /**
     * 获取公司ID
     */
    public static String getCompanyId() {
        return getContextHolder().getCompanyId();
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        getContextHolder().setUserId(userId);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return getContextHolder().getUserId();
    }

    /**
     * 获取有效的租户ID（带回落逻辑）
     */
    public static String getEffectiveTenantId() {
        String tenantId = getTenantId();

        if (tenantId != null) {
            return tenantId;
        }

        // 租户上下文缺失
        if (securityConfig != null && securityConfig.getMode() == AgentSecurityConfig.SecurityMode.DEMO) {
            // Demo 模式：允许回落
            String defaultTenantId = securityConfig.getDemo().getDefaultTenantId();

            if (securityConfig.getDemo().isWarnOnFallback()) {
                log.warn("租户上下文缺失，回落到默认租户: {}", defaultTenantId);
            }

            return defaultTenantId;
        } else {
            // Enterprise 模式：直接拒绝
            throw new TenantContextMissingException("租户上下文缺失，请求被拒绝");
        }
    }

    /**
     * 获取有效的公司ID（带回落逻辑）
     */
    public static String getEffectiveCompanyId() {
        String companyId = getCompanyId();

        if (companyId != null) {
            return companyId;
        }

        if (securityConfig != null && securityConfig.getMode() == AgentSecurityConfig.SecurityMode.DEMO) {
            String defaultCompanyId = securityConfig.getDemo().getDefaultCompanyId();
            if (defaultCompanyId != null && !defaultCompanyId.isBlank()) {
                return defaultCompanyId.trim();
            }
            throw new TenantContextMissingException("Demo 公司上下文未显式配置，请求被拒绝");
        } else {
            throw new TenantContextMissingException("公司上下文缺失，请求被拒绝");
        }
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取上下文持有者
     */
    private static TenantContextHolder getContextHolder() {
        TenantContextHolder holder = CONTEXT.get();
        if (holder == null) {
            holder = new TenantContextHolder();
            CONTEXT.set(holder);
        }
        return holder;
    }

    /**
     * 上下文数据持有者
     */
    @Data
    static class TenantContextHolder {
        private String tenantId;
        private String companyId;
        private Long userId;
    }
}
