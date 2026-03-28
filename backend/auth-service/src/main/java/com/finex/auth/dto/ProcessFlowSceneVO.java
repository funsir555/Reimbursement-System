package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessFlowSceneVO {

    private Long id;

    private String sceneCode;

    private String sceneName;

    private String sceneDescription;

    private Integer status;
}
