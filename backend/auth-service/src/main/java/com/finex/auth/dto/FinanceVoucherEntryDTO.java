package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceVoucherEntryDTO {

    private Integer inid;

    @Size(max = 255, message = "\u5206\u5f55\u6458\u8981\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 255 \u4e2a\u5b57\u7b26")
    private String cdigest;

    @Size(max = 64, message = "\u79d1\u76ee\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String ccode;

    @Size(max = 64, message = "\u90e8\u95e8\u6807\u8bc6\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String cdeptId;

    @Size(max = 64, message = "\u4eba\u5458\u6807\u8bc6\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String cpersonId;

    @Size(max = 64, message = "\u5ba2\u6237\u6807\u8bc6\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String ccusId;

    @Size(max = 64, message = "\u4f9b\u5e94\u5546\u6807\u8bc6\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String csupId;

    @Size(max = 2, message = "\u9879\u76ee\u5206\u7c7b\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 2 \u4e2a\u5b57\u7b26")
    private String citemClass;

    @Size(max = 6, message = "\u9879\u76ee\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 6 \u4e2a\u5b57\u7b26")
    private String citemId;

    private Long cashFlowItemId;

    @Size(max = 200, message = "\u73b0\u91d1\u6d41\u91cf\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 200 \u4e2a\u5b57\u7b26")
    private String cashFlowItemName;

    @Size(max = 32, message = "\u5e01\u79cd\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26")
    private String cexchName;

    private BigDecimal nfrat;

    @MoneyInput
    private BigDecimal md;

    @MoneyInput
    private BigDecimal mc;

    private BigDecimal ndS;

    private BigDecimal ncS;
}
