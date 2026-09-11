package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceGeneralLedgerPeriodStatusOverviewVO {

    private String companyId;

    private String companyName;

    private Integer iyear;

    private Integer currentIyear;

    private Integer currentIperiod;

    private String currentPeriodLabel;

    private List<FinanceGeneralLedgerPeriodStatusRowVO> rows;
}
