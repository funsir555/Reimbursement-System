package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fin_account_set_module_enable")
public class FinanceAccountSetModuleEnable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("module_code")
    private String moduleCode;

    private Integer enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
