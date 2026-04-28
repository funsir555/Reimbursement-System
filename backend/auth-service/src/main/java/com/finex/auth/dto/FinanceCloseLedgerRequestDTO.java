package com.finex.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FinanceCloseLedgerRequestDTO {

    @NotBlank
    private String companyId;

    @Min(2000)
    @Max(2099)
    private Integer iyear;

    @Min(1)
    @Max(12)
    private Integer iperiod;

    private String closeNote;
}
