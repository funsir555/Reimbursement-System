package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_process_flow_route")
public class ProcessFlowRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    private String routeKey;

    private String sourceNodeKey;

    private String targetNodeKey;

    private String routeName;

    private Integer priority;

    private Integer defaultRoute;

    private Integer attachBelowNodes;

    private String conditionJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
