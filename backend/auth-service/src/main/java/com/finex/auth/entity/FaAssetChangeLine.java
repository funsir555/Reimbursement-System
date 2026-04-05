package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_change_line")
public class FaAssetChangeLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long billId;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String changeType;
    private Long categoryId;
    private String categoryCode;
    private String useCompanyId;
    private Long useDeptId;
    private Long keeperUserId;
    private LocalDate inServiceDate;
    private BigDecimal changeAmount;
    private BigDecimal oldValue;
    private BigDecimal newValue;
    private BigDecimal oldSalvageAmount;
    private BigDecimal newSalvageAmount;
    private Integer oldUsefulLifeMonths;
    private Integer newUsefulLifeMonths;
    private Integer oldRemainingMonths;
    private Integer newRemainingMonths;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
