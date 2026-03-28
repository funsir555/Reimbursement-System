package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherDetailVO {

    private String voucherNo;

    private String companyId;

    private Integer iperiod;

    private String csign;

    private Integer inoId;

    private String dbillDate;

    private Integer idoc;

    private String cbill;

    private String ctext1;

    private String ctext2;

    private String status;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private List<FinanceVoucherEntryVO> entries = new ArrayList<>();
}
