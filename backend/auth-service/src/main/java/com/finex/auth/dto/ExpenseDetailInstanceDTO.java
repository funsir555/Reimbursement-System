package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExpenseDetailInstanceDTO {

    private String detailNo;

    private String detailDesignCode;

    private String detailType;

    private String enterpriseMode;

    private String expenseTypeCode;

    private String businessSceneMode;

    private String detailTitle;

    private Integer sortOrder;

    private Map<String, Object> formData = new LinkedHashMap<>();
}
