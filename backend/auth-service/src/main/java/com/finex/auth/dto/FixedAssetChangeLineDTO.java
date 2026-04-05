package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetChangeLineDTO {
    private Long assetId;

    @NotBlank(message = "assetCode is required")
    private String assetCode;

    private String assetName;
    private Long categoryId;
    private String categoryCode;
    private String useCompanyId;
    private Long useDeptId;
    private Long keeperUserId;
    private String inServiceDate;
    @MoneyInput
    private BigDecimal changeAmount;
    @MoneyInput
    private BigDecimal newValue;
    @MoneyInput
    private BigDecimal newSalvageAmount;
    private Integer newUsefulLifeMonths;
    private Integer newRemainingMonths;
    private String remark;
}
