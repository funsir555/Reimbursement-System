package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinanceAccountSubjectCloseDTO {

    @NotNull(message = "封存状态不能为空")
    private Integer bclose;
}
