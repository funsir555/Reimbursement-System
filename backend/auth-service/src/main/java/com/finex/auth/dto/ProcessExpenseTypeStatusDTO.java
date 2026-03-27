package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessExpenseTypeStatusDTO {

    @NotNull(message = "Status is required")
    private Integer status;
}
