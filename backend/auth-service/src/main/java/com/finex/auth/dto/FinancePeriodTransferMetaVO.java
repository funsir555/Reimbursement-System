package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinancePeriodTransferMetaVO {

    private String companyId;

    private String companyName;

    private Integer iyear;

    private Integer iperiod;

    private Integer iyperiod;

    private String periodLabel;

    private String defaultVoucherType;

    private String defaultVoucherTypeLabel;

    private List<FinanceVoucherOptionVO> accountOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectClassOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> projectOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> metricOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> transferGranularityOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> allocationModeOptions = new ArrayList<>();

    private List<FinanceVoucherOptionVO> ruleTypeOptions = new ArrayList<>();
}
