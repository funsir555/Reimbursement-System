package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_project_archive")
public class FinanceProjectArchive {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    private String citemcode;

    private String citemname;

    private Integer bclose;

    private String citemccode;

    private Integer iotherused;

    @TableField("dEndDate")
    private LocalDateTime dEndDate;

    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
