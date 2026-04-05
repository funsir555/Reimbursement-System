package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fa_asset_account_policy")
public class FaAssetAccountPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long categoryId;
    private String bookCode;
    private String assetAccount;
    private String accumDeprAccount;
    private String deprExpenseAccount;
    private String disposalAccount;
    private String gainAccount;
    private String lossAccount;
    private String offsetAccount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
