package com.finex.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 当前用户角色编码
     */
    java.util.List<String> getRoleCodes(Long userId);

    /**
     * 当前用户权限编码
     */
    java.util.List<String> getPermissionCodes(Long userId);
}
