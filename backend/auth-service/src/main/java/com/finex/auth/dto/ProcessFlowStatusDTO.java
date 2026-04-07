package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProcessFlowStatusDTO {

    @NotBlank(message = "娴佺▼鐘舵€佷笉鑳戒负绌?")
    private String status;
}
