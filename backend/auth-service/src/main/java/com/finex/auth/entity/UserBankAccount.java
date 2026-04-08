package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_bank_account")
public class UserBankAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String bankName;

    private String branchName;

    private String bankCode;

    private String branchCode;

    private String cnapsCode;

    private String province;

    private String city;

    private String accountName;

    private String accountNo;

    private String accountType;

    private Integer defaultAccount;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
