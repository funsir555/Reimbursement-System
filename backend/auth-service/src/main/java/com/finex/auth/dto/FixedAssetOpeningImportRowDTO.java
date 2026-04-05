package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetOpeningImportRowDTO {
    private Integer rowNo;
    private String assetCode;
    private String assetName;
    private String categoryCode;
    private String acquireDate;
    private String inServiceDate;
    @MoneyInput
    private BigDecimal originalAmount;
    @MoneyInput
    private BigDecimal accumDeprAmount;
    @MoneyInput
    private BigDecimal salvageAmount;
    private Integer usefulLifeMonths;
    private Integer depreciatedMonths;
    private Integer remainingMonths;
    private Long useDeptId;
    private Long keeperUserId;
    private String status;
    private BigDecimal workTotal;
    private BigDecimal workUsed;
    private String remark;
}
