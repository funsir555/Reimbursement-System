package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fa_asset_opening_import")
public class FaAssetOpeningImport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyId;
    private String batchNo;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
