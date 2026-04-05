package com.finex.auth.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetDeprWorkloadDTO {
    private Long assetId;
    private BigDecimal workAmount;
}
