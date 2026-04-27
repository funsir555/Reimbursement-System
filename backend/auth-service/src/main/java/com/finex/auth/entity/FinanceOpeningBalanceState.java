package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_opening_balance_state")
public class FinanceOpeningBalanceState {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String status;

    @TableField("source_type")
    private String sourceType;

    @TableField("opened_by")
    private String openedBy;

    @TableField("opened_at")
    private LocalDateTime openedAt;

    @TableField("last_trial_at")
    private LocalDateTime lastTrialAt;

    @TableField("last_reconcile_at")
    private LocalDateTime lastReconcileAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
