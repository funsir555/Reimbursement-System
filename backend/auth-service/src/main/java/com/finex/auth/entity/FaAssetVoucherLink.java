package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fa_asset_voucher_link")
public class FaAssetVoucherLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String businessType;
    private Long businessId;
    private String voucherNo;
    private Integer iperiod;
    private String csign;
    private Integer inoId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
