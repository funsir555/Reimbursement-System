package com.finex.auth.dto;

import lombok.Data;

@Data
public class FixedAssetOptionVO {
    private String value;
    private String label;
    private String code;
    private String name;
    private String parentValue;
}
