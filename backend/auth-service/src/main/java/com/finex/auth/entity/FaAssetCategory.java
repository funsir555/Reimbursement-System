package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_category")
public class FaAssetCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String categoryCode;
    private String categoryName;
    private String shareScope;
    private String depreciationMethod;
    private Integer usefulLifeMonths;
    private BigDecimal residualRate;
    private Integer depreciable;
    private String status;
    private String remark;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
