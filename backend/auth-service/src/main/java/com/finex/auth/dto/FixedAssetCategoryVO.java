package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetCategoryVO {
    private Long id;
    private String companyId;
    private String categoryCode;
    private String categoryName;
    private String shareScope;
    private String depreciationMethod;
    private Integer usefulLifeMonths;
    private BigDecimal residualRate;
    private Boolean depreciable;
    private String status;
    private String remark;
    private String bookCode;
    private String assetAccount;
    private String accumDeprAccount;
    private String deprExpenseAccount;
    private String disposalAccount;
    private String gainAccount;
    private String lossAccount;
    private String offsetAccount;
}
