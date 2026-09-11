package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceGeneralLedgerPeriodStatusRowVO {

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String periodLabel;

    private boolean available;

    private Integer voucherCount;

    private Integer unpostedVoucherCount;

    private Integer reviewedVoucherCount;

    private Integer errorVoucherCount;

    private Integer postedVoucherCount;

    private String reviewStatus;

    private String reviewStatusLabel;

    private String postStatus;

    private String postStatusLabel;

    private String closeStatus;

    private String closeStatusLabel;

    private String periodTransferStatus;

    private String periodTransferStatusLabel;

    private String nextPeriodStatus;

    private String nextPeriodStatusLabel;

    private List<String> allowedActions;

    private String blockingReason;
}
