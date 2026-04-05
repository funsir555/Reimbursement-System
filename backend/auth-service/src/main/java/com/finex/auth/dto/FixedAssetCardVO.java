package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetCardVO {
    private Long id;
    private String companyId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String depreciationMethod;
    private String bookCode;
    private String useCompanyId;
    private Long useDeptId;
    private String useDeptName;
    private Long keeperUserId;
    private String keeperName;
    private Long managerUserId;
    private String managerName;
    private String sourceType;
    private String acquireDate;
    private String inServiceDate;
    @MoneyValue
    private BigDecimal originalAmount;
    @MoneyValue
    private BigDecimal accumDeprAmount;
    @MoneyValue
    private BigDecimal salvageAmount;
    @MoneyValue
    private BigDecimal netAmount;
    private Integer usefulLifeMonths;
    private Integer depreciatedMonths;
    private Integer remainingMonths;
    private BigDecimal workTotal;
    private BigDecimal workUsed;
    private String status;
    private Boolean canDepreciate;
    private Integer lastDeprYear;
    private Integer lastDeprPeriod;
    private String remark;
}
