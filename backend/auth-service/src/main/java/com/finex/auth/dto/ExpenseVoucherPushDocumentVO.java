package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseVoucherPushDocumentVO {

    private String companyId;
    private String companyName;
    private String documentCode;
    private String templateCode;
    private String templateName;
    private Long submitterUserId;
    private String submitterName;
    @MoneyValue
    private BigDecimal totalAmount;
    private String finishedAt;
    private String expenseSummary;
    private Boolean canPush;
    private String pushStatus;
    private String pushStatusLabel;
    private String failureReason;
    private String voucherNo;
}
