package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fin_account_set_code_rule")
public class FinanceAccountSetCodeRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("rule_type")
    private String ruleType;

    private String scheme;

    @TableField("level1_length")
    private Integer level1Length;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
