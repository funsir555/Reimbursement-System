package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProcessCustomArchiveResolveDTO {

    @NotBlank(message = "档案编码不能为空")
    private String archiveCode;

    private Map<String, Object> context = new HashMap<>();
}
