package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseManualApproverSelectionDTO {

    @NotBlank
    private String nodeKey;

    @NotEmpty
    private List<Long> userIds = new ArrayList<>();
}
