package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_transfer_run_detail")
public class FinancePeriodTransferRunDetail {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("run_id")
    private Long runId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_line_id")
    private Long ruleLineId;

    @TableField("line_no")
    private Integer lineNo;

    @TableField("source_subject_code")
    private String sourceSubjectCode;

    @TableField("source_subject_name")
    private String sourceSubjectName;

    @TableField("source_assist_key")
    private String sourceAssistKey;

    @TableField("source_assist_label")
    private String sourceAssistLabel;

    @TableField("target_subject_code")
    private String targetSubjectCode;

    @TableField("target_subject_name")
    private String targetSubjectName;

    @TableField("target_cdept_id")
    private String targetCdeptId;

    @TableField("target_cperson_id")
    private String targetCpersonId;

    @TableField("target_ccus_id")
    private String targetCcusId;

    @TableField("target_csup_id")
    private String targetCsupId;

    @TableField("target_citem_class")
    private String targetCitemClass;

    @TableField("target_citem_id")
    private String targetCitemId;

    @TableField("metric_type")
    private String metricType;

    private String direction;

    private BigDecimal amount;

    @TableField("skip_reason")
    private String skipReason;

    @TableField("source_trace_key")
    private String sourceTraceKey;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
