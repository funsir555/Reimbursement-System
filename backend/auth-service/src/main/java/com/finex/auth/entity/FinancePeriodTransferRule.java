package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_transfer_rule")
public class FinancePeriodTransferRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("rule_name")
    private String ruleName;

    private Integer enabled;

    @TableField("voucher_type")
    private String voucherType;

    @TableField("include_unposted")
    private Integer includeUnposted;

    @TableField("transfer_granularity")
    private String transferGranularity;

    @TableField("subject_level")
    private Integer subjectLevel;

    @TableField("source_subject_code")
    private String sourceSubjectCode;

    @TableField("source_subject_name")
    private String sourceSubjectName;

    @TableField("target_subject_code")
    private String targetSubjectCode;

    @TableField("target_subject_name")
    private String targetSubjectName;

    @TableField("allocation_mode")
    private String allocationMode;

    @TableField("regenerate_strategy")
    private String regenerateStrategy;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
