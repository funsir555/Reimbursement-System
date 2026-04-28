package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceAssistDraftLineDTO {

    @NotBlank
    private String subjectCode;

    @Valid
    private List<OpeningAssistBalanceLineDTO> lines = new ArrayList<>();
}
