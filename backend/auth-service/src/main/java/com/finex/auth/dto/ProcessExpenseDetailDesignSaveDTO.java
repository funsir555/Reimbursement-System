package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessExpenseDetailDesignSaveDTO {

    @NotBlank(message = "\u8d39\u7528\u660e\u7ec6\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u8d39\u7528\u660e\u7ec6\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String detailName;

    @NotBlank(message = "\u8d39\u7528\u660e\u7ec6\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a")
    private String detailType;

    private String detailDescription;
    private Map<String, Object> schema = new LinkedHashMap<>();
}
