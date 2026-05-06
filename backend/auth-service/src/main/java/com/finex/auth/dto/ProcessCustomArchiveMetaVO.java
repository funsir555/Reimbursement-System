package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveMetaVO {

    private List<ProcessFormOptionVO> archiveTypeOptions = new ArrayList<>();

    private List<ProcessCustomArchiveOperatorVO> operatorOptions = new ArrayList<>();

    private List<ProcessCustomArchiveRuleFieldVO> ruleFields = new ArrayList<>();

    private List<ProcessFormOptionVO> companyOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> userOptions = new ArrayList<>();

    private List<EmployeeDirectoryOptionVO> employeeDirectory = new ArrayList<>();

    private List<ProcessFormOptionVO> expenseTypeOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> archiveOptions = new ArrayList<>();

    private String tagArchiveCode;

    private String installmentArchiveCode;
}
