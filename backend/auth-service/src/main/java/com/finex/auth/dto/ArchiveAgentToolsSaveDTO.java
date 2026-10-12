package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ArchiveAgentToolsSaveDTO {
    @NotNull
    private Integer expectedVersionNo;
    @NotNull
    private List<String> toolCodes;
}
