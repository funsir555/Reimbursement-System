package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessExpenseTypeSaveDTO {

    @NotBlank(message = "Expense type name is required")
    private String expenseName;

    private String expenseDescription;

    @NotBlank(message = "Expense type code is required")
    private String expenseCode;

    private List<String> scopeDeptIds = new ArrayList<>();

    private List<String> scopeUserIds = new ArrayList<>();

    @NotBlank(message = "Invoice free mode is required")
    private String invoiceFreeMode;

    @NotBlank(message = "Tax deduction mode is required")
    private String taxDeductionMode;

    @NotBlank(message = "Tax separation mode is required")
    private String taxSeparationMode;

    private Integer status;
}
