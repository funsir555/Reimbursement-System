package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_depr_run")
public class FaAssetDeprRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String runNo;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private Integer assetCount;
    private BigDecimal totalAmount;
    private String remark;
    private String createdBy;
    private String postedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime postedAt;
}
