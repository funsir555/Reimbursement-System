package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_close_log")
public class FinancePeriodCloseLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    @TableField("action_type")
    private String actionType;

    @TableField("action_status")
    private String actionStatus;

    @TableField("operator_name")
    private String operatorName;

    private String message;

    @TableField("detail_json")
    private String detailJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
