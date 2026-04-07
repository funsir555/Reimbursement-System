package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_company")
public class SystemCompany {

    @TableId(value = "company_id", type = IdType.INPUT)
    private String companyId;

    private String companyCode;

    private String companyName;

    private String invoiceTitle;

    private String taxNo;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
