package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class PersonalCenterVO {

    private UserProfileVO user;

    private List<BankAccountVO> bankAccounts;
}
