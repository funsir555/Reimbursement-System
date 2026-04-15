package com.finex.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProcessFlowConditionDTO {

    @Size(max = 64, message = "\u6761\u4ef6\u5b57\u6bb5\u6807\u8bc6\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String fieldKey;

    private String operator;
    private Object compareValue;
}
