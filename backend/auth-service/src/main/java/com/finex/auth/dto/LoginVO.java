package com.finex.auth.dto;

import lombok.Data;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String name;

    /**
     * Token
     */
    private String token;

    /**
     * Token过期时间（秒）
     */
    private Long expireIn;
}
