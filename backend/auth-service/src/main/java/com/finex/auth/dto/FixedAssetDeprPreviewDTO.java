package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetDeprPreviewDTO {
    @NotBlank(message = "companyId is required")
    private String companyId;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private List<Long> assetIds = new ArrayList<>();

    @Valid
    private List<FixedAssetDeprWorkloadDTO> workloads = new ArrayList<>();

    private String remark;
}
