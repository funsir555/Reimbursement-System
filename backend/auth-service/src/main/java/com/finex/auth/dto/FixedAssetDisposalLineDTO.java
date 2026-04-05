package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FixedAssetDisposalLineDTO {
    private Long assetId;

    @NotBlank(message = "assetCode is required")
    private String assetCode;

    private String remark;
}
