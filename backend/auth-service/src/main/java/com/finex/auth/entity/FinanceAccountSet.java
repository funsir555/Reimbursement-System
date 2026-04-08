package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_account_set")
public class FinanceAccountSet {

    @TableId(value = "company_id", type = IdType.INPUT)
    private String companyId;

    private String status;

    @TableField("enabled_year")
    private Integer enabledYear;

    @TableField("enabled_period")
    private Integer enabledPeriod;

    @TableField("template_code")
    private String templateCode;

    @TableField("supervisor_user_id")
    private Long supervisorUserId;

    @TableField("create_mode")
    private String createMode;

    @TableField("reference_company_id")
    private String referenceCompanyId;

    @TableField("subject_code_scheme")
    private String subjectCodeScheme;

    @TableField("subject_count")
    private Integer subjectCount;

    @TableField("last_task_no")
    private String lastTaskNo;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
