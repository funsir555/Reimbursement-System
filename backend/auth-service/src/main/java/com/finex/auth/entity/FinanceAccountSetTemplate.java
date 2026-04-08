package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_account_set_template")
public class FinanceAccountSetTemplate {

    @TableId(value = "template_code", type = IdType.INPUT)
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("accounting_standard")
    private String accountingStandard;

    private String description;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
