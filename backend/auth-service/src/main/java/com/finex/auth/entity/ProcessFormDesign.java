package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_form_design")
public class ProcessFormDesign {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String formCode;

    private String formName;

    private String templateType;

    private String formDescription;

    private String schemaJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
