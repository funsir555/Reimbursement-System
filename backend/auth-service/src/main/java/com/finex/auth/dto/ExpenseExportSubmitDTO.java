package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ExpenseExportSubmitDTO {

    @NotBlank
    private String scene;

    private List<String> documentCodes;

    private List<Long> taskIds;

    private String kind;
}
