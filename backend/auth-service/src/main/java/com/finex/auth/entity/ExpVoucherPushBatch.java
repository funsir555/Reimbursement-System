package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exp_voucher_push_batch")
public class ExpVoucherPushBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String batchNo;
    private Integer documentCount;
    private Integer successCount;
    private Integer failureCount;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
