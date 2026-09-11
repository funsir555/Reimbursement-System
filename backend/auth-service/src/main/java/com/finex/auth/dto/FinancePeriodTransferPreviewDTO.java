package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinancePeriodTransferPreviewDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotNull(message = "会计年度不能为空")
    private Integer iyear;

    @NotNull(message = "会计期间不能为空")
    private Integer iperiod;

    @NotNull(message = "规则ID不能为空")
    private Long ruleId;
}
