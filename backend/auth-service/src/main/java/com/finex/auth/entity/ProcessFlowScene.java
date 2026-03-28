package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_process_flow_scene")
public class ProcessFlowScene {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sceneCode;

    private String sceneName;

    private String sceneDescription;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
