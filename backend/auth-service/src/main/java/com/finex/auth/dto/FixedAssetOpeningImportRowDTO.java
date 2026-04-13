package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyInput;
import lombok.Data;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Data
public class FixedAssetOpeningImportRowDTO {
    private Integer rowNo;
    @Size(max = 32, message = "资产编码长度不能超过32个字符")
    private String assetCode;
    @Size(max = 64, message = "资产名称长度不能超过64个字符")
    private String assetName;
    @Size(max = 32, message = "类别编码长度不能超过32个字符")
    private String categoryCode;
    private String acquireDate;
    private String inServiceDate;
    @MoneyInput
    private BigDecimal originalAmount;
    @MoneyInput
    private BigDecimal accumDeprAmount;
    @MoneyInput
    private BigDecimal salvageAmount;
    private Integer usefulLifeMonths;
    private Integer depreciatedMonths;
    private Integer remainingMonths;
    private Long useDeptId;
    private Long keeperUserId;
    private String status;
    private BigDecimal workTotal;
    private BigDecimal workUsed;
    private String remark;
}
