package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseVoucherTemplatePolicySaveDTO {

    @NotBlank(message = "??????")
    private String companyId;

    @NotBlank(message = "????????")
    private String templateCode;

    private String templateName;

    @NotBlank(message = "??????????")
    private String creditAccountCode;

    private String creditAccountName;

    @NotBlank(message = "????????")
    private String voucherType;

    private String summaryRule;

    private Integer enabled;
}
