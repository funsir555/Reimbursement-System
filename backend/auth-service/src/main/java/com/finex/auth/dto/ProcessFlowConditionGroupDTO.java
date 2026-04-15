package com.finex.auth.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowConditionGroupDTO {

    private Integer groupNo;

    @Valid
    private List<ProcessFlowConditionDTO> conditions = new ArrayList<>();
}
