package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_depr_line")
public class FaAssetDeprLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long runId;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String depreciationMethod;
    private BigDecimal workAmount;
    private BigDecimal depreciationAmount;
    private BigDecimal beforeAccumAmount;
    private BigDecimal afterAccumAmount;
    private BigDecimal beforeNetAmount;
    private BigDecimal afterNetAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
