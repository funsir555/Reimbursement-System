package com.finex.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseVoucherPushDTO {

    @NotEmpty(message = "??????????")
    private List<String> documentCodes = new ArrayList<>();
}
