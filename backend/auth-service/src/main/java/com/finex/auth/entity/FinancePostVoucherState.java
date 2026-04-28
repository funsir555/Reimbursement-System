package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_post_state")
public class FinancePostVoucherState {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String status;

    @TableField("posted_voucher_count")
    private Integer postedVoucherCount;

    @TableField("last_task_no")
    private String lastTaskNo;

    @TableField("last_task_status")
    private String lastTaskStatus;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("last_posted_by")
    private String lastPostedBy;

    @TableField("last_posted_at")
    private LocalDateTime lastPostedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
