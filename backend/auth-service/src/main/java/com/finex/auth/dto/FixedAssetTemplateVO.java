package com.finex.auth.dto;

import lombok.Data;

@Data
public class FixedAssetTemplateVO {
    private String fileName;
    private String contentType;
    private String templateContent;
}
