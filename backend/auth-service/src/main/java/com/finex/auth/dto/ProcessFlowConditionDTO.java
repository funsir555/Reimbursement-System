package com.finex.auth.dto;

import lombok.Data;

@Data
public class ProcessFlowConditionDTO {

    private String fieldKey;

    private String operator;

    private Object compareValue;
}
