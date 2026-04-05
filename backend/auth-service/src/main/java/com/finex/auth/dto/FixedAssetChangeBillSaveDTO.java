package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetChangeBillSaveDTO {
    @NotBlank(message = "companyId is required")
    private String companyId;

    @NotBlank(message = "billType is required")
    private String billType;

    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String billDate;
    private String remark;

    @Valid
    @NotEmpty(message = "lines are required")
    private List<FixedAssetChangeLineDTO> lines = new ArrayList<>();
}
