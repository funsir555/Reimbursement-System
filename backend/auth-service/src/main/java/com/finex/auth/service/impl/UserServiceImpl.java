package com.finex.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finex.auth.dto.LoginDTO;
import com.finex.auth.dto.LoginVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.UserService;
import com.finex.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        User user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验密码（MD5加密）
        String encryptedPassword = DigestUtil.md5Hex(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 组装响应
        LoginVO loginVO = new LoginVO();
        BeanUtil.copyProperties(user, loginVO);
        loginVO.setUserId(user.getId());
        loginVO.setToken(token);
        loginVO.setExpireIn(7 * 24 * 60 * 60L); // 7天

        log.info("用户登录成功: {}", user.getUsername());
        return loginVO;
    }

    @Override
    public User getByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }
}
