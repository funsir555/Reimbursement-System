package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserProfileVO {

    private Long userId;

    private String username;

    private String name;

    private String phone;

    private String email;

    private String position;

    private String laborRelationBelong;

    private String companyId;

    private List<String> roles = new ArrayList<>();

    private List<String> permissionCodes = new ArrayList<>();
}
