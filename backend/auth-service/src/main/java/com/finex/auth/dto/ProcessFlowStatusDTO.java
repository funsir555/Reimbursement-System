package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProcessFlowStatusDTO {

    @NotBlank(message = "流程状态不能为空")
    private String status;
}
