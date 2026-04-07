package com.finex.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 鐧诲綍璇锋眰DTO
 */
@Data
public class LoginDTO {

    @NotBlank(message = "鐢ㄦ埛鍚嶄笉鑳戒负绌?")
    private String username;

    @NotBlank(message = "瀵嗙爜涓嶈兘涓虹┖")
    private String password;
}
