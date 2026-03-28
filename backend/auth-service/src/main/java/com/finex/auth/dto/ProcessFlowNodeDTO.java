package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessFlowNodeDTO {

    private String nodeKey;

    private String nodeType;

    private String nodeName;

    private Long sceneId;

    private String parentNodeKey;

    private Integer displayOrder;

    private Map<String, Object> config = new LinkedHashMap<>();
}
