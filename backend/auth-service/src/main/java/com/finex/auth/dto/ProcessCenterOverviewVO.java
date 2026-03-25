package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 流程管理中心总览
 */
@Data
public class ProcessCenterOverviewVO {

    private List<ProcessCenterNavItemVO> navItems;

    private ProcessCenterSummaryVO summary;

    private List<ProcessTemplateCategoryVO> categories;
}
