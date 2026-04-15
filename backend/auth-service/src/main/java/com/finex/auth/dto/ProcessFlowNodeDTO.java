package com.finex.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessFlowNodeDTO {

    private String nodeKey;
    private String nodeType;

    @Size(max = 64, message = "\u8282\u70b9\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String nodeName;

    private Long sceneId;
    private String parentNodeKey;
    private Integer displayOrder;
    private Map<String, Object> config = new LinkedHashMap<>();
}
