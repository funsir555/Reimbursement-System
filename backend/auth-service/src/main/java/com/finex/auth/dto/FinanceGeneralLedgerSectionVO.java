package com.finex.auth.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinanceGeneralLedgerSectionVO {

    private String subjectCode;

    private String subjectName;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private BigDecimal endDebit;

    private BigDecimal endCredit;

    private int rowCount;

    private List<FinanceDetailLedgerRowVO> rows = new ArrayList<>();
}
