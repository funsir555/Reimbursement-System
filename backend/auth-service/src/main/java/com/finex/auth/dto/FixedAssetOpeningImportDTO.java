package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetOpeningImportDTO {
    @NotBlank(message = "companyId is required")
    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;

    @Valid
    @NotEmpty(message = "rows are required")
    private List<FixedAssetOpeningImportRowDTO> rows = new ArrayList<>();
}
