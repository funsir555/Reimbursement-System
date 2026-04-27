package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceReconcileResultVO {

    private Boolean matched;

    private List<OpeningBalanceRowVO> differenceSubjects;

    private List<OpeningBalanceRowVO> missingAssistSubjects;

    private List<String> illegalAssistMessages;
}
