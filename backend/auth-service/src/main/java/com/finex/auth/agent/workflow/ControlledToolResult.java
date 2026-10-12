package com.finex.auth.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 受控工具执行结果。
 */
@Data
@Builder
public class ControlledToolResult {

    private boolean success;
    private String errorMessage;
    private String summary;
    private List<Map<String, Object>> records;
    private List<String> evidenceRefs;
    private Long totalCount;
    private BigDecimal totalAmount;
    private Object queryDescriptor;
    private String sourceDigest;
    private Integer page;
    private Integer pageSize;
    private Boolean hasMore;
}
