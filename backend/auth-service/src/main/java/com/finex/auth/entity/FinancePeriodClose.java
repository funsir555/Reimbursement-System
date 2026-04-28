package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_close")
public class FinancePeriodClose {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String status;

    @TableField("closed_by")
    private String closedBy;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("reopened_by")
    private String reopenedBy;

    @TableField("reopened_at")
    private LocalDateTime reopenedAt;

    @TableField("close_note")
    private String closeNote;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
