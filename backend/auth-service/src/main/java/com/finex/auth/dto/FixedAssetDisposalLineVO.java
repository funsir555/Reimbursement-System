package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetDisposalLineVO {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryName;
    @MoneyValue
    private BigDecimal originalAmount;
    @MoneyValue
    private BigDecimal accumDeprAmount;
    @MoneyValue
    private BigDecimal netAmount;
    private String remark;
}
