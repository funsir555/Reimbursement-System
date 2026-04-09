package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherEntryVO {

    private Integer inid;

    private String cdigest;

    private String ccode;

    private String ccodeName;

    private String cdeptId;

    private String cpersonId;

    private String ccusId;

    private String csupId;

    private String citemClass;

    private String citemId;

    private String cexchName;

    private BigDecimal nfrat;

    @MoneyValue
    private BigDecimal md;

    @MoneyValue
    private BigDecimal mc;

    private BigDecimal ndS;

    private BigDecimal ncS;
}
