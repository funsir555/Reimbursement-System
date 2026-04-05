package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExpenseDetailInstanceDetailVO {

    private String documentCode;

    private String detailNo;

    private String detailDesignCode;

    private String detailType;

    private String detailTypeLabel;

    private String enterpriseMode;

    private String enterpriseModeLabel;

    private String expenseTypeCode;

    private String businessSceneMode;

    private String detailTitle;

    private Integer sortOrder;

    private Map<String, Object> schemaSnapshot = new LinkedHashMap<>();

    private Map<String, Object> formData = new LinkedHashMap<>();

    private String createdAt;

    private String updatedAt;
}
