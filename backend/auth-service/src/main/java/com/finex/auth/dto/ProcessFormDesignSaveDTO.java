package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessFormDesignSaveDTO {

    @NotBlank(message = "表单类型不能为空")
    private String templateType;

    @NotBlank(message = "表单名称不能为空")
    private String formName;

    private String formDescription;

    private Map<String, Object> schema = new LinkedHashMap<>();
}
