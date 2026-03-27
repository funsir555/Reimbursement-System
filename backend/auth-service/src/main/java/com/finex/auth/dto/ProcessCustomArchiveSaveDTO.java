package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveSaveDTO {

    @NotBlank(message = "档案名称不能为空")
    private String archiveName;

    @NotBlank(message = "档案类型不能为空")
    private String archiveType;

    private String archiveDescription;

    private Integer status;

    @Valid
    private List<ProcessCustomArchiveItemDTO> items = new ArrayList<>();
}
