package com.finex.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinanceGeneralLedgerPeriodActionDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    @Max(value = 2099, message = "年度不合法")
    private Integer iyear;

    @NotNull(message = "期间不能为空")
    @Min(value = 1, message = "期间必须在 1 到 12 之间")
    @Max(value = 12, message = "期间必须在 1 到 12 之间")
    private Integer iperiod;

    @NotNull(message = "当前年度不能为空")
    @Min(value = 2000, message = "当前年度不合法")
    @Max(value = 2099, message = "当前年度不合法")
    private Integer currentIyear;

    @NotNull(message = "当前期间不能为空")
    @Min(value = 1, message = "当前期间必须在 1 到 12 之间")
    @Max(value = 12, message = "当前期间必须在 1 到 12 之间")
    private Integer currentIperiod;

    @NotBlank(message = "登录密码不能为空")
    private String password;
}
