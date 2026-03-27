package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessCustomArchiveRuleDTO {

    private Long id;

    @NotNull(message = "规则组不能为空")
    private Integer groupNo;

    @NotBlank(message = "规则字段不能为空")
    private String fieldKey;

    @NotBlank(message = "规则操作符不能为空")
    private String operator;

    private Object compareValue;
}
