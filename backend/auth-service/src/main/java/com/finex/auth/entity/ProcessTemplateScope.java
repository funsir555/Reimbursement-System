package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_template_scope")
public class ProcessTemplateScope {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String optionType;

    private String optionCode;

    private String optionLabel;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
