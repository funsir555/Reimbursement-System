package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FixedAssetDisposalLineDTO {
    private Long assetId;

    @NotBlank(message = "assetCode is required")
    @Size(max = 32, message = "资产编码长度不能超过32个字符")
    private String assetCode;

    private String remark;
}
