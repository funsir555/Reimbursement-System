package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("gl_period_transfer_voucher_link")
public class FinancePeriodTransferVoucherLink {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("run_id")
    private Long runId;

    @TableField("run_detail_id")
    private Long runDetailId;

    @TableField("voucher_no")
    private String voucherNo;

    @TableField("entry_no")
    private Integer entryNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
