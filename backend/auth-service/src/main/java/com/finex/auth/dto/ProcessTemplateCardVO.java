package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 单据模板卡片
 */
@Data
public class ProcessTemplateCardVO {

    private Long id;

    private String templateCode;

    private String name;

    private String templateTypeCode;

    private String templateType;

    private String businessDomain;

    private String description;

    private List<String> highlights;

    private String flowCode;

    private String flowName;

    private String formCode;

    private String formName;

    private String expenseDetailDesignCode;

    private String expenseDetailDesignName;

    private String status;

    private String statusLabel;

    private String updatedAt;

    private String owner;

    private String color;
}
