package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinanceModuleEnableToggleDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotBlank(message = "模块编码不能为空")
    private String moduleCode;

    @NotNull(message = "启停状态不能为空")
    private Boolean enabled;
}
