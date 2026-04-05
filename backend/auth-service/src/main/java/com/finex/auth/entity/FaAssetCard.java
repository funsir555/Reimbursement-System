package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_card")
public class FaAssetCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryCode;
    private String bookCode;
    private String useCompanyId;
    private Long useDeptId;
    private Long keeperUserId;
    private Long managerUserId;
    private String sourceType;
    private LocalDate acquireDate;
    private LocalDate inServiceDate;
    private BigDecimal originalAmount;
    private BigDecimal accumDeprAmount;
    private BigDecimal salvageAmount;
    private BigDecimal netAmount;
    private Integer usefulLifeMonths;
    private Integer depreciatedMonths;
    private Integer remainingMonths;
    private BigDecimal workTotal;
    private BigDecimal workUsed;
    private String status;
    private Integer canDepreciate;
    private Integer lastDeprYear;
    private Integer lastDeprPeriod;
    private String remark;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
