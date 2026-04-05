package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetDisposalBillSaveDTO {
    @NotBlank(message = "companyId is required")
    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String billDate;
    private String remark;

    @Valid
    @NotEmpty(message = "lines are required")
    private List<FixedAssetDisposalLineDTO> lines = new ArrayList<>();
}
