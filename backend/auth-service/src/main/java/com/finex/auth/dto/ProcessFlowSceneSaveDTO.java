package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProcessFlowSceneSaveDTO {

    @NotBlank(message = "场景名称不能为空")
    private String sceneName;

    private String sceneDescription;

    private Integer status;
}
