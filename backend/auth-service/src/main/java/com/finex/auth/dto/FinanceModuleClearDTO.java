package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FinanceModuleClearDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotBlank(message = "模块编码不能为空")
    private String moduleCode;
}
