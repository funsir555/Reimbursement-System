package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exp_voucher_push_document")
public class ExpVoucherPushDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long batchId;
    private String batchNo;
    private String documentCode;
    private String templateCode;
    private String templateName;
    private Long submitterUserId;
    private String submitterName;
    private BigDecimal totalAmount;
    private String pushStatus;
    private String voucherNo;
    private String voucherType;
    private Integer voucherNumber;
    private Integer fiscalPeriod;
    private LocalDate billDate;
    private String errorMessage;
    private LocalDateTime pushedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
