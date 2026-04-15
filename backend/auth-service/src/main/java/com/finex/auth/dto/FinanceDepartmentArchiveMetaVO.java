package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceDepartmentArchiveMetaVO {

    private List<DepartmentTreeNodeVO> departments = new ArrayList<>();

    private List<FinanceDepartmentArchiveOptionVO> statusOptions = new ArrayList<>();
}
