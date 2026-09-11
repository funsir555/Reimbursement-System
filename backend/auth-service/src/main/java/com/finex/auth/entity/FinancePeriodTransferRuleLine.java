package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_transfer_rule_line")
public class FinancePeriodTransferRuleLine {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("line_no")
    private Integer lineNo;

    @TableField("source_subject_code")
    private String sourceSubjectCode;

    @TableField("source_subject_name")
    private String sourceSubjectName;

    @TableField("target_subject_code")
    private String targetSubjectCode;

    @TableField("target_subject_name")
    private String targetSubjectName;

    @TableField("metric_type")
    private String metricType;

    private BigDecimal ratio;

    @TableField("allocation_project_class")
    private String allocationProjectClass;

    @TableField("allocation_project_id")
    private String allocationProjectId;

    @TableField("allocation_project_name")
    private String allocationProjectName;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
