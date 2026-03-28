package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_process_flow_node")
public class ProcessFlowNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    private String nodeKey;

    private String nodeType;

    private String nodeName;

    private Long sceneId;

    private String parentNodeKey;

    private Integer displayOrder;

    private String configJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
