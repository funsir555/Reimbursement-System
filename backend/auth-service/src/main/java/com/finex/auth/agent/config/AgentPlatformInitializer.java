package com.finex.auth.agent.config;

import com.finex.auth.agent.model.ChatModelProvider;
import com.finex.auth.agent.model.ModelRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent 平台初始化配置
 * 在应用启动时执行初始化任务
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentPlatformInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final AgentSecurityConfig securityConfig;
    private final ModelRoutingService modelRoutingService;
    private final List<ChatModelProvider> providers;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=".repeat(80));
        log.info("FinEx AI Agent Platform 初始化中...");
        log.info("=".repeat(80));

        // 检查安全模式
        checkSecurityMode();

        // 注册模型 Providers
        registerProviders();

        log.info("=".repeat(80));
        log.info("FinEx AI Agent Platform 初始化完成");
        log.info("=".repeat(80));
    }

    /**
     * 检查安全模式
     */
    private void checkSecurityMode() {
        if (securityConfig.getMode() == AgentSecurityConfig.SecurityMode.DEMO) {
            log.warn("┌" + "─".repeat(78) + "┐");
            log.warn("│ 警告：当前运行在 DEMO 模式，租户隔离已降级！" + " ".repeat(38) + "│");
            log.warn("│ 默认租户: {}                                                          │",
                    String.format("%-50s", securityConfig.getDemo().getDefaultTenantId()));
            log.warn("│ 默认公司: {}                                                              │",
                    String.format("%-50s", securityConfig.getDemo().getDefaultCompanyId()));
            log.warn("│ 生产环境请设置: finex.agent.security.mode=enterprise" + " ".repeat(24) + "│");
            log.warn("└" + "─".repeat(78) + "┘");
        } else {
            log.info("当前运行在 ENTERPRISE 模式，租户隔离已启用");
        }
    }

    /**
     * 注册模型 Providers
     */
    private void registerProviders() {
        log.info("注册模型 Providers...");

        if (providers.isEmpty()) {
            log.warn("未找到任何模型 Provider！请检查配置");
            return;
        }

        for (ChatModelProvider provider : providers) {
            try {
                modelRoutingService.registerProvider(provider);
                log.info("  ✓ {}: {}",
                        provider.getProviderCode(),
                        provider.getCapability().getDeployment());
            } catch (Exception e) {
                log.error("  ✗ 注册 Provider 失败: {}", provider.getProviderCode(), e);
            }
        }

        log.info("已注册 {} 个模型 Providers", providers.size());
    }
}
