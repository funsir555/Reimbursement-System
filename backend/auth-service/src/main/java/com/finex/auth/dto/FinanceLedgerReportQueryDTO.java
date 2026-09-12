package com.finex.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FinanceLedgerReportQueryDTO {

    @NotBlank(message = "公司不能为空")
    private String companyId;

    private Integer iyear;

    @Min(value = 1, message = "会计月份不合法")
    @Max(value = 12, message = "会计月份不合法")
    private Integer iperiod;

    private Integer iyearFrom;

    @Min(value = 1, message = "期间起月份不合法")
    @Max(value = 12, message = "期间起月份不合法")
    private Integer iperiodFrom;

    private Integer iyearTo;

    @Min(value = 1, message = "期间止月份不合法")
    @Max(value = 12, message = "期间止月份不合法")
    private Integer iperiodTo;

    private String ledgerKind;

    private String accountCodeFrom;

    private String accountCodeTo;

    private String cdeptId;

    private String cpersonId;

    private String ccusId;

    private String csupId;

    private String citemClass;

    private String citemId;

    private String balanceAssistDisplay;

    private String subjectLevelRange;

    private Boolean includeUnposted;

    private String voucherNo;

    private String csign;

    private String summary;

    private String cbill;

    @Min(value = 1, message = "页码不合法")
    private Integer page;

    @Min(value = 1, message = "每页条数不合法")
    @Max(value = 500, message = "每页条数不能超过500")
    private Integer pageSize;
}
