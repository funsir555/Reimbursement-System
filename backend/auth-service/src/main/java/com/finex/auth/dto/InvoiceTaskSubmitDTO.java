package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvoiceTaskSubmitDTO {

    @NotBlank(message = "发票代码不能为空")
    private String code;

    @NotBlank(message = "发票号码不能为空")
    private String number;
}
