package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseVoucherTemplatePolicySaveDTO {

    @NotBlank(message = "公司不能为空")
    private String companyId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    private String templateName;

    @NotBlank(message = "贷方科目不能为空")
    private String creditAccountCode;

    private String creditAccountName;

    private String personRule;

    private String supplierRule;

    private String deptRule;

    private String projectRule;

    @NotBlank(message = "凭证类型不能为空")
    private String voucherType;

    private String summaryRule;

    private Integer enabled;
}
