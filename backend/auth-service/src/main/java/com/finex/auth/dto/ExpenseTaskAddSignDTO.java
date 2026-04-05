package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseTaskAddSignDTO {

    @NotNull
    private Long targetUserId;

    private String remark;
}
