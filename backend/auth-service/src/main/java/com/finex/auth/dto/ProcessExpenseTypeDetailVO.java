package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessExpenseTypeDetailVO {

    private Long id;

    private Long parentId;

    private String expenseCode;

    private String expenseName;

    private String expenseDescription;

    private Integer codeLevel;

    private String codePrefix;

    private List<String> scopeDeptIds = new ArrayList<>();

    private List<String> scopeUserIds = new ArrayList<>();

    private String invoiceFreeMode;

    private String taxDeductionMode;

    private String taxSeparationMode;

    private Integer status;
}
