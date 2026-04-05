package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fa_asset_period_close")
public class FaAssetPeriodClose {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private String closedBy;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
