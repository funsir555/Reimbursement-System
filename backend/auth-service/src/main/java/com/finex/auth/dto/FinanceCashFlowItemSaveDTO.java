package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceCashFlowItemSaveDTO {

    @NotBlank(message = "现金流量编码不能为空")
    @Size(max = 32, message = "现金流量编码长度不能超过 32 个字符")
    private String cashFlowCode;

    @NotBlank(message = "现金流量名称不能为空")
    @Size(max = 200, message = "现金流量名称长度不能超过 200 个字符")
    private String cashFlowName;

    @NotBlank(message = "现金流量方向不能为空")
    @Size(max = 16, message = "现金流量方向长度不能超过 16 个字符")
    private String direction;

    private Integer status;

    private Integer sortOrder;
}
