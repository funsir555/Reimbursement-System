package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceModuleEnableMetaVO {

    private String companyId;

    private String companyName;

    private List<FinanceModuleEnableSummaryVO> modules;
}
