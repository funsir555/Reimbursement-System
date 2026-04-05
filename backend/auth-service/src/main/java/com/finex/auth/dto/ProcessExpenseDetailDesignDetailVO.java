package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProcessExpenseDetailDesignDetailVO {

    private Long id;

    private String detailCode;

    private String detailName;

    private String detailType;

    private String detailTypeLabel;

    private String detailDescription;

    private String updatedAt;

    private Map<String, Object> schema = new LinkedHashMap<>();
}
