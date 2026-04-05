package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_expense_detail_design")
public class ProcessExpenseDetailDesign {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String detailCode;

    private String detailName;

    private String detailType;

    private String detailDescription;

    private String schemaJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
