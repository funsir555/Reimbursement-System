package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessFlowConditionGroupDTO {

    private Integer groupNo;

    private List<ProcessFlowConditionDTO> conditions = new ArrayList<>();
}
