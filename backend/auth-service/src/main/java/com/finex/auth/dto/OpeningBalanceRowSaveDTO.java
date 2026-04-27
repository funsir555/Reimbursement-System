package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class OpeningBalanceRowSaveDTO {

    @NotBlank
    private String subjectCode;

    private BigDecimal mb;

    private BigDecimal mbF;

    private BigDecimal nbS;
}
