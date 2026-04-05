package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FixedAssetPeriodCloseDTO {
    @NotBlank(message = "companyId is required")
    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
}
