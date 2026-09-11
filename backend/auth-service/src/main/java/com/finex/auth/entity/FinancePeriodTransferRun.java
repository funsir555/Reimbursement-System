package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_transfer_run")
public class FinancePeriodTransferRun {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("rule_name")
    private String ruleName;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String status;

    @TableField("preview_token")
    private String previewToken;

    @TableField("include_unposted")
    private Integer includeUnposted;

    @TableField("voucher_type")
    private String voucherType;

    @TableField("voucher_no")
    private String voucherNo;

    @TableField("generated_entry_count")
    private Integer generatedEntryCount;

    @TableField("skipped_entry_count")
    private Integer skippedEntryCount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("blocked_message")
    private String blockedMessage;

    @TableField("rule_updated_at")
    private LocalDateTime ruleUpdatedAt;

    @TableField("previewed_by")
    private String previewedBy;

    @TableField("previewed_at")
    private LocalDateTime previewedAt;

    @TableField("generated_by")
    private String generatedBy;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
