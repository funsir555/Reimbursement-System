package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinancePostVoucherMetaVO {

    private String companyId;

    private String companyName;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String periodLabel;

    private String status;

    private String statusLabel;

    private Boolean canPost;

    private String blockedReason;

    private Integer unpostedVoucherCount;

    private List<String> unpostedSampleVoucherNos;

    private Integer errorVoucherCount;

    private List<String> errorSampleVoucherNos;

    private Integer reviewableVoucherCount;

    private Integer postedVoucherCount;

    private String lastTaskNo;

    private String lastTaskStatus;

    private String lastTaskMessage;
}
