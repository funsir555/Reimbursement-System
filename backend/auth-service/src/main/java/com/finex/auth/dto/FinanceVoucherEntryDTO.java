package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherEntryDTO {

    private Integer inid;

    private String cdigest;

    private String ccode;

    private String cdeptId;

    private String cpersonId;

    private String ccusId;

    private String csupId;

    private String citemClass;

    private String citemId;

    private String cexchName;

    private BigDecimal nfrat;

    @MoneyInput
    private BigDecimal md;

    @MoneyInput
    private BigDecimal mc;

    private BigDecimal ndS;

    private BigDecimal ncS;
}
