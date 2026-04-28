package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceCloseLedgerReconcileResultVO {

    private Boolean passed;

    private String summaryMessage;

    private Integer differenceSubjectCount;

    private Integer differenceAssistCount;

    private Integer missingAssistCount;

    private Integer illegalAssistCount;

    private List<String> differenceSubjects;

    private List<String> differenceAssistKeys;

    private List<String> missingAssistSubjects;

    private List<String> illegalAssistMessages;
}
