package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exp_voucher_template_policy")
public class ExpVoucherTemplatePolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String templateCode;
    private String templateName;
    private String creditAccountCode;
    private String creditAccountName;
    private String voucherType;
    private String summaryRule;
    private Integer enabled;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
