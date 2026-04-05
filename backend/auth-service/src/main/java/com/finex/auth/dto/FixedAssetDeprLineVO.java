package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetDeprLineVO {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryName;
    private String depreciationMethod;
    private BigDecimal workAmount;
    @MoneyValue
    private BigDecimal depreciationAmount;
    @MoneyValue
    private BigDecimal beforeAccumAmount;
    @MoneyValue
    private BigDecimal afterAccumAmount;
    @MoneyValue
    private BigDecimal beforeNetAmount;
    @MoneyValue
    private BigDecimal afterNetAmount;
}
