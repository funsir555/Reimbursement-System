package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exp_voucher_push_entry")
public class ExpVoucherPushEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long pushDocumentId;
    private Integer entryNo;
    private String direction;
    private String digest;
    private String accountCode;
    private String accountName;
    private String expenseTypeCode;
    private String expenseTypeName;
    private BigDecimal amount;
    private Long personId;
    private String personName;
    private Long supplierId;
    private String supplierName;
    private Long deptId;
    private String deptName;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
