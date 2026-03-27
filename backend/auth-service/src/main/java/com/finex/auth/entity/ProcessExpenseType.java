package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_expense_type")
public class ProcessExpenseType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String expenseCode;

    private String expenseName;

    private String expenseDescription;

    private Integer codeLevel;

    private String codePrefix;

    private String scopeDeptIds;

    private String scopeUserIds;

    private String invoiceFreeMode;

    private String taxDeductionMode;

    private String taxSeparationMode;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
