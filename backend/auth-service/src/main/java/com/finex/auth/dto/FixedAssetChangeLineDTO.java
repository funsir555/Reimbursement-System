package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetChangeLineDTO {
    private Long assetId;

    @NotBlank(message = "assetCode is required")
    @Size(max = 32, message = "资产编码长度不能超过32个字符")
    private String assetCode;

    @Size(max = 64, message = "资产名称长度不能超过64个字符")
    private String assetName;
    private Long categoryId;
    @Size(max = 32, message = "类别编码长度不能超过32个字符")
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
