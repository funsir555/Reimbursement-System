package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessUserGroupMetaVO {

    private List<ProcessFlowConditionFieldVO> scopeConditionFields = new ArrayList<>();

    private List<ProcessFormOptionVO> scopeOperatorOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> companyOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> userOptions = new ArrayList<>();
}
