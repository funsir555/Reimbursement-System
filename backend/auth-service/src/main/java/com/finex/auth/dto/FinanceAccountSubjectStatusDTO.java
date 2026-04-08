package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinanceAccountSubjectStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
