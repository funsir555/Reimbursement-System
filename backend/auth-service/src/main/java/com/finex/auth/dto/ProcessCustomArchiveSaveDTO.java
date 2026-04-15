package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveSaveDTO {

    @NotBlank(message = "\u6863\u6848\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 64, message = "\u6863\u6848\u540d\u79f0\u6700\u591a 64 \u4e2a\u5b57\u7b26")
    private String archiveName;

    @NotBlank(message = "\u6863\u6848\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a")
    private String archiveType;

    private String archiveDescription;
    private Integer status;

    @Valid
    private List<ProcessCustomArchiveItemDTO> items = new ArrayList<>();
}
