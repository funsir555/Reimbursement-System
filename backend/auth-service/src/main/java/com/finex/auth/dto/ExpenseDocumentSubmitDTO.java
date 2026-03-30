package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExpenseDocumentSubmitDTO {

    @NotBlank
    private String templateCode;

    @NotNull
    private Map<String, Object> formData = new LinkedHashMap<>();
}
