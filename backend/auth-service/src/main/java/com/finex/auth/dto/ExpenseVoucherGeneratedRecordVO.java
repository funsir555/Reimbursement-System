package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseVoucherGeneratedRecordVO {

    private Long id;
    private String companyId;
    private String companyName;
    private String batchNo;
    private String documentCode;
    private String templateCode;
    private String templateName;
    private String submitterName;
    @MoneyValue
    private BigDecimal totalAmount;
    private String pushStatus;
    private String pushStatusLabel;
    private String voucherNo;
    private String voucherType;
    private Integer voucherNumber;
    private String billDate;
    private String pushedAt;
    private String failureReason;
}
