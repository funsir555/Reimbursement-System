package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArchiveAgentToggleStatusDTO {

    @NotBlank(message = "??????")
    private String status;
}
