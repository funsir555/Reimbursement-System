package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_disposal_line")
public class FaAssetDisposalLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long billId;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private BigDecimal originalAmount;
    private BigDecimal accumDeprAmount;
    private BigDecimal netAmount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
