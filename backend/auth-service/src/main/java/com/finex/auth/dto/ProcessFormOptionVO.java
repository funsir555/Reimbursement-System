package com.finex.auth.dto;

import lombok.Data;

/**
 * 下拉选项
 */
@Data
public class ProcessFormOptionVO {

    private String label;

    private String value;

    private String code;

    private String name;

    private String parentValue;
}
