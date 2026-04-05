package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetOpeningImportResultVO {
    private Long batchId;
    private String companyId;
    private String batchNo;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private List<FixedAssetOpeningImportLineVO> lines = new ArrayList<>();
}
