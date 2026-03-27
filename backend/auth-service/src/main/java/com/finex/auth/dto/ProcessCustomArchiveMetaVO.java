package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveMetaVO {

    private List<ProcessFormOptionVO> archiveTypeOptions = new ArrayList<>();

    private List<ProcessCustomArchiveOperatorVO> operatorOptions = new ArrayList<>();

    private List<ProcessCustomArchiveRuleFieldVO> ruleFields = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private String tagArchiveCode;

    private String installmentArchiveCode;
}
