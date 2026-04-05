package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fa_asset_opening_import_line")
public class FaAssetOpeningImportLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private Long batchId;
    private Integer rowNo;
    private String assetCode;
    private String assetName;
    private String categoryCode;
    private String resultStatus;
    private String errorMessage;
    private Long importedAssetId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
