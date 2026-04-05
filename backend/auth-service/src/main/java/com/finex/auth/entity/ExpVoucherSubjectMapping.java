package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exp_voucher_subject_mapping")
public class ExpVoucherSubjectMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String templateCode;
    private String templateName;
    private String expenseTypeCode;
    private String expenseTypeName;
    private String debitAccountCode;
    private String debitAccountName;
    private Integer enabled;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
