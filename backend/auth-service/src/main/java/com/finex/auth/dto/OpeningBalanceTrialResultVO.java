package com.finex.auth.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceTrialResultVO {

    private Boolean balanced;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private BigDecimal difference;

    private List<OpeningBalanceRowVO> abnormalSubjects;
}
