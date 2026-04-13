package com.finex.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetCardSaveDTO {
    private Long id;

    @NotBlank(message = "companyId is required")
    private String companyId;

    @NotBlank(message = "assetCode is required")
    @Size(max = 32, message = "资产编码长度不能超过32个字符")
    private String assetCode;

    @NotBlank(message = "assetName is required")
    @Size(max = 64, message = "资产名称长度不能超过64个字符")
    private String assetName;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    private String bookCode;
    private String useCompanyId;
    private Long useDeptId;
    private Long keeperUserId;
    private Long managerUserId;
    private String sourceType;
    private String acquireDate;

    @NotBlank(message = "inServiceDate is required")
    private String inServiceDate;

    @NotNull(message = "originalAmount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "originalAmount must be greater than 0")
    @MoneyInput
    private BigDecimal originalAmount;

    @NotNull(message = "accumDeprAmount is required")
    @DecimalMin(value = "0.0", message = "accumDeprAmount must not be negative")
    @MoneyInput
    private BigDecimal accumDeprAmount;

    @NotNull(message = "salvageAmount is required")
    @DecimalMin(value = "0.0", message = "salvageAmount must not be negative")
    @MoneyInput
    private BigDecimal salvageAmount;

    @NotNull(message = "usefulLifeMonths is required")
    @Min(value = 1, message = "usefulLifeMonths must be greater than 0")
    private Integer usefulLifeMonths;

    @NotNull(message = "depreciatedMonths is required")
    @Min(value = 0, message = "depreciatedMonths must not be negative")
    private Integer depreciatedMonths;

    @NotNull(message = "remainingMonths is required")
    @Min(value = 0, message = "remainingMonths must not be negative")
    private Integer remainingMonths;

    private BigDecimal workTotal;
    private BigDecimal workUsed;
    private String status;
    private Boolean canDepreciate;
    private String remark;
}
