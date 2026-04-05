package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_document_relation")
public class ProcessDocumentRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceDocumentCode;

    private String sourceFieldKey;

    private String targetDocumentCode;

    private String targetTemplateType;

    private Integer sortOrder;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
