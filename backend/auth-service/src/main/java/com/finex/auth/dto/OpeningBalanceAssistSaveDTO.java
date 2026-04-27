package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceAssistSaveDTO {

    @NotBlank
    private String companyId;

    @Min(2000)
    @Max(2099)
    private Integer iyear;

    @Min(1)
    @Max(12)
    private Integer iperiod;

    @Valid
    private List<OpeningAssistBalanceLineDTO> lines;
}
