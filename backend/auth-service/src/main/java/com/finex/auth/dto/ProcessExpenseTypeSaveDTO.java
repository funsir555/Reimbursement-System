package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessExpenseTypeSaveDTO {

    @NotBlank(message = "\u8d39\u7528\u7c7b\u578b\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u8d39\u7528\u7c7b\u578b\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String expenseName;

    private String expenseDescription;

    @NotBlank(message = "\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a")
    private String expenseCode;

    private List<String> scopeDeptIds = new ArrayList<>();
    private List<String> scopeUserIds = new ArrayList<>();

    @NotBlank(message = "\u53d1\u7968\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a")
    private String invoiceFreeMode;

    @NotBlank(message = "\u7a0e\u989d\u62b5\u6263\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a")
    private String taxDeductionMode;

    @NotBlank(message = "\u4ef7\u7a0e\u5206\u79bb\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a")
    private String taxSeparationMode;

    private Integer status;
}
