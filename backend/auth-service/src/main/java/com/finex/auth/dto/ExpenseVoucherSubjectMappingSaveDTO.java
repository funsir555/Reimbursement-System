package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseVoucherSubjectMappingSaveDTO {

    @NotBlank(message = "??????")
    private String companyId;

    @NotBlank(message = "????????")
    private String templateCode;

    private String templateName;

    @NotBlank(message = "????????")
    private String expenseTypeCode;

    private String expenseTypeName;

    @NotBlank(message = "????????")
    private String debitAccountCode;

    private String debitAccountName;

    private Integer enabled;
}
