package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceEmployeeArchiveMetaVO {

    private List<CompanyVO> companies = new ArrayList<>();

    private List<DepartmentTreeNodeVO> departments = new ArrayList<>();
}
