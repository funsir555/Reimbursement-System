package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessFormDesignDetailVO {

    private Long id;

    private String formCode;

    private String formName;

    private String templateType;

    private String templateTypeLabel;

    private String formDescription;

    private Map<String, Object> schema = new LinkedHashMap<>();

    private String updatedAt;
}
