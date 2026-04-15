package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProcessFlowSceneSaveDTO {

    @NotBlank(message = "\u573a\u666f\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u573a\u666f\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String sceneName;

    private String sceneDescription;
    private Integer status;
}
