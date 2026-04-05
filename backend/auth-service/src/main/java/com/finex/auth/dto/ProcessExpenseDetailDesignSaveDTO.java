package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessExpenseDetailDesignSaveDTO {

    @NotBlank
    private String detailName;

    @NotBlank
    private String detailType;

    private String detailDescription;

    private Map<String, Object> schema = new LinkedHashMap<>();
}
