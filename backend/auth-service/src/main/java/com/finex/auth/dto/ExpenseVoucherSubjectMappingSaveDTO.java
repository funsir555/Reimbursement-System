package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseVoucherSubjectMappingSaveDTO {

    @NotBlank(message = "公司不能为空")
    private String companyId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    private String templateName;

    @NotBlank(message = "费用类型不能为空")
    private String expenseTypeCode;

    private String expenseTypeName;

    @NotBlank(message = "借方科目不能为空")
    private String debitAccountCode;

    private String debitAccountName;

    private String personRule;

    private String supplierRule;

    private String deptRule;

    private String projectRule;

    private Integer enabled;
}
