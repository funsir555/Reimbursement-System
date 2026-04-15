// 业务域：登录认证与权限
// 文件角色：service 接口
// 上下游关系：上游通常来自 AuthController、用户相关 service 入口，下游会继续协调 用户、角色、权限与 JWT 等基础能力。
// 风险提醒：改坏后最容易影响 登录成功率、权限判断和当前用户识别。

package com.finex.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;

/**
 * UserService：service 接口。
 * 定义用户这块对外提供的业务入口能力。
 * 改这里时，要特别关注 登录成功率、权限判断和当前用户识别是否会被一起带坏。
 */
public interface UserService extends IService<User> {

    /**
     * 执行登录校验并返回登录结果。
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 按用户名获取用户。
     */
    User getByUsername(String username);

    /**
     * 获取角色编码。
     */
    java.util.List<String> getRoleCodes(Long userId);

    /**
     * 获取权限编码。
     */
    java.util.List<String> getPermissionCodes(Long userId);
}
