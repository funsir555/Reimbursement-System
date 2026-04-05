package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetMetaVO {
    private List<FixedAssetOptionVO> companyOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> departmentOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> employeeOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> categoryOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> depreciationMethodOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> cardStatusOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> changeTypeOptions = new ArrayList<>();
    private List<FixedAssetOptionVO> bookOptions = new ArrayList<>();
    private String defaultCompanyId;
    private String defaultBookCode;
    private Integer defaultFiscalYear;
    private Integer defaultFiscalPeriod;
    private String periodStatus;
    private Integer cardCount;
    private Integer pendingDepreciationCount;
    @MoneyValue
    private BigDecimal currentPeriodDepreciationAmount;
}
