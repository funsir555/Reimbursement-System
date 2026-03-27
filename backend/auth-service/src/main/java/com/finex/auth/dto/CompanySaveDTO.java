package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanySaveDTO {

    private String companyId;

    private String companyCode;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    private String invoiceTitle;

    private String taxNo;

    private String bankName;

    private String bankAccountName;

    private String bankAccountNo;

    private Integer status;
}
