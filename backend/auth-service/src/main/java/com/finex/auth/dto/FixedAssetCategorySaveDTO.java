package com.finex.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetCategorySaveDTO {
    private Long id;

    @NotBlank(message = "companyId is required")
    private String companyId;

    @NotBlank(message = "categoryCode is required")
    private String categoryCode;

    @NotBlank(message = "categoryName is required")
    private String categoryName;

    @NotBlank(message = "shareScope is required")
    private String shareScope;

    @NotBlank(message = "depreciationMethod is required")
    private String depreciationMethod;

    @NotNull(message = "usefulLifeMonths is required")
    @Min(value = 1, message = "usefulLifeMonths must be greater than 0")
    private Integer usefulLifeMonths;

    @NotNull(message = "residualRate is required")
    @DecimalMin(value = "0.0", message = "residualRate must not be negative")
    private BigDecimal residualRate;

    private Boolean depreciable;
    private String status;
    private String remark;
    private String bookCode;

    @NotBlank(message = "assetAccount is required")
    private String assetAccount;

    @NotBlank(message = "accumDeprAccount is required")
    private String accumDeprAccount;

    @NotBlank(message = "deprExpenseAccount is required")
    private String deprExpenseAccount;

    @NotBlank(message = "disposalAccount is required")
    private String disposalAccount;

    @NotBlank(message = "gainAccount is required")
    private String gainAccount;

    @NotBlank(message = "lossAccount is required")
    private String lossAccount;

    @NotBlank(message = "offsetAccount is required")
    private String offsetAccount;
}
