package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceDepartmentQueryDTO {

    private String keyword;

    private Long parentId;

    private Integer status;
}
