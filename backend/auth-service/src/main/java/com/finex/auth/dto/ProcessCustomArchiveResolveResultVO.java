package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveResolveResultVO {

    private String archiveCode;

    private String archiveType;

    private List<ProcessCustomArchiveResolveItemVO> items = new ArrayList<>();
}
