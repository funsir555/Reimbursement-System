package com.finex.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExpensePaymentBatchTaskDTO {

    @NotEmpty(message = "taskIds cannot be empty")
    private List<@NotNull Long> taskIds;

    private String comment;
}
