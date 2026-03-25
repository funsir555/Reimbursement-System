package com.finex.auth.dto;

import lombok.Data;

/**
 * 模板类型选项
 */
@Data
public class ProcessTemplateTypeVO {

    private String code;

    private String name;

    private String subtitle;

    private String description;

    private String accent;
}
