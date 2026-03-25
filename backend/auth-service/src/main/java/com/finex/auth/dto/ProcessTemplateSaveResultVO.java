package com.finex.auth.dto;

import lombok.Data;

/**
 * 保存模板响应
 */
@Data
public class ProcessTemplateSaveResultVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String status;
}
