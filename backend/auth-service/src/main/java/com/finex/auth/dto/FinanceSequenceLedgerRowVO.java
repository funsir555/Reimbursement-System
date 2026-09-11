package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinanceSequenceLedgerRowVO {

    private String voucherNo;

    private String displayVoucherNo;

    private String companyId;

    private Integer iyear;

    private Integer iyperiod;

    private Integer iperiod;

    private String csign;

    private String voucherTypeLabel;

    private Integer inoId;

    private String dbillDate;

    private String summary;

    private String cbill;

    private Integer idoc;

    private String status;

    private String statusLabel;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;
}
