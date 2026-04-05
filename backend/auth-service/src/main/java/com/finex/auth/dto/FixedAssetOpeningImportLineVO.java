package com.finex.auth.dto;

import lombok.Data;

@Data
public class FixedAssetOpeningImportLineVO {
    private Integer rowNo;
    private String assetCode;
    private String assetName;
    private String categoryCode;
    private String resultStatus;
    private String errorMessage;
    private Long importedAssetId;
}
