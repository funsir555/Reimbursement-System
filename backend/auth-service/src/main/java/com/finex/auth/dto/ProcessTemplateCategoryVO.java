package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 单据模板分组
 */
@Data
public class ProcessTemplateCategoryVO {

    private String code;

    private String name;

    private String description;

    private Integer templateCount;

    private List<ProcessTemplateCardVO> templates;
}
