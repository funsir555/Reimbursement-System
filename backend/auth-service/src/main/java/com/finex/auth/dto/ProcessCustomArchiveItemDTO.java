package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveItemDTO {

    private Long id;

    private String itemCode;

    @NotBlank(message = "结果项名称不能为空")
    private String itemName;

    private Integer priority;

    private Integer status;

    @Valid
    private List<ProcessCustomArchiveRuleDTO> rules = new ArrayList<>();
}
