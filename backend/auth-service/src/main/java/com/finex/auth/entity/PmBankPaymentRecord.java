package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_bank_payment_record")
public class PmBankPaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String documentCode;

    private Long companyBankAccountId;

    private String bankProvider;

    private String bankChannel;

    private Integer manualPaid;

    private String pushRequestNo;

    private String bankOrderNo;

    private String bankFlowNo;

    private String pushPayloadJson;

    private String pushResultJson;

    private String callbackPayloadJson;

    private LocalDateTime callbackReceivedAt;

    private LocalDateTime paidAt;

    private String receiptStatus;

    private LocalDateTime receiptReceivedAt;

    private LocalDateTime lastReceiptQueryAt;

    private Integer receiptQueryCount;

    private String receiptAttachmentId;

    private String receiptFileName;

    private String receiptResultJson;

    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
