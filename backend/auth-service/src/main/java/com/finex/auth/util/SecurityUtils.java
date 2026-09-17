package com.finex.auth.util;

import com.finex.auth.entity.User;

/**
 * Security 工具类
 * 用于获取当前登录用户信息
 *
 * 注意：这是一个简化的实现，实际项目中应该与现有的认证机制集成
 */
public class SecurityUtils {

    /**
     * 线程本地存储，用于临时存储当前用户信息
     */
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    /**
     * 设置当前用户ID（由认证过滤器或拦截器调用）
     */
    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前用户对象（简化版本）
     */
    public static User getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        // 创建一个基本的 User 对象
        User user = new User();
        user.setId(userId);

        return user;
    }

    /**
     * 创建 SecurityContext（用于异步任务恢复）
     * 简化实现：返回 null
     */
    public static Object createSecurityContext(Long userId) {
        // 简化实现
        return null;
    }

    /**
     * 检查是否已登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    /**
     * 清理当前线程的用户信息
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
