package com.finex.auth.dto;

import lombok.Data;

/**
 * 流程管理中心统计
 */
@Data
public class ProcessCenterSummaryVO {

    private Integer totalTemplates;

    private Integer enabledTemplates;

    private Integer draftTemplates;

    private Integer aiAuditTemplates;
}
