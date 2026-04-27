package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceSaveDTO {

    @NotBlank
    private String companyId;

    @Min(2000)
    @Max(2099)
    private Integer iyear;

    @Min(1)
    @Max(12)
    private Integer iperiod;

    @Valid
    @NotEmpty
    private List<OpeningBalanceRowSaveDTO> rows;
}
