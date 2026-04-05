package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetChangeLineVO {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String changeType;
    private Long categoryId;
    private String categoryCode;
    private String useCompanyId;
    private Long useDeptId;
    private String useDeptName;
    private Long keeperUserId;
    private String keeperName;
    private String inServiceDate;
    @MoneyValue
    private BigDecimal changeAmount;
    @MoneyValue
    private BigDecimal oldValue;
    @MoneyValue
    private BigDecimal newValue;
    @MoneyValue
    private BigDecimal oldSalvageAmount;
    @MoneyValue
    private BigDecimal newSalvageAmount;
    private Integer oldUsefulLifeMonths;
    private Integer newUsefulLifeMonths;
    private Integer oldRemainingMonths;
    private Integer newRemainingMonths;
    private String remark;
}
