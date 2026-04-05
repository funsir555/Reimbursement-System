package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fa_asset_disposal_bill")
public class FaAssetDisposalBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String billNo;
    private String billType;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private LocalDate billDate;
    private String status;
    private BigDecimal totalOriginalAmount;
    private BigDecimal totalAccumAmount;
    private BigDecimal totalNetAmount;
    private String remark;
    private String createdBy;
    private String postedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime postedAt;
}
