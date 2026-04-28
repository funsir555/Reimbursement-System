package com.finex.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinancePostVoucherTaskRequestDTO {

    @NotBlank(message = "公司不能为空")
    private String companyId;

    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    @Max(value = 2099, message = "年度不合法")
    private Integer iyear;

    @NotNull(message = "期间不能为空")
    @Min(value = 1, message = "期间不合法")
    @Max(value = 12, message = "期间不合法")
    private Integer iperiod;
}
