package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveDetailVO {

    private Long id;

    private String archiveCode;

    private String archiveName;

    private String archiveType;

    private String archiveTypeLabel;

    private String archiveDescription;

    private Integer status;

    private List<ProcessCustomArchiveItemDTO> items = new ArrayList<>();
}
