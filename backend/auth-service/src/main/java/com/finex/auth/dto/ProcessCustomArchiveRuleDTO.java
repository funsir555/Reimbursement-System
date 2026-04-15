package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProcessCustomArchiveRuleDTO {

    private Long id;

    @NotNull(message = "\u89c4\u5219\u7ec4\u5e8f\u53f7\u4e0d\u80fd\u4e3a\u7a7a")
    private Integer groupNo;

    @NotBlank(message = "\u89c4\u5219\u5b57\u6bb5\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u89c4\u5219\u5b57\u6bb5\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String fieldKey;

    @NotBlank(message = "\u89c4\u5219\u64cd\u4f5c\u7b26\u4e0d\u80fd\u4e3a\u7a7a")
    private String operator;

    private Object compareValue;
}
