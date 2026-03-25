package com.finex.auth.dto;

import lombok.Data;

@Data
public class UserProfileVO {

    private Long userId;

    private String username;

    private String name;

    private String phone;

    private String email;

    private String position;

    private String laborRelationBelong;
}
