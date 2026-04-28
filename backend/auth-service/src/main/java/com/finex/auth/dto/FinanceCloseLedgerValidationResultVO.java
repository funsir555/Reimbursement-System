package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceCloseLedgerValidationResultVO {

    private Boolean passed;

    private Boolean generalPassed;

    private Boolean externalPassed;

    private Boolean alreadyClosed;

    private Boolean reconcilePassed;

    private String postStatus;

    private String postStatusLabel;

    private List<String> blockingReasons;

    private List<FinanceCloseLedgerCheckItemVO> generalChecks;

    private List<FinanceCloseLedgerCheckItemVO> externalChecks;
}
