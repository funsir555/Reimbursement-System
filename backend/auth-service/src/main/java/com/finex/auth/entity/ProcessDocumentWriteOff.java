package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pm_document_write_off")
public class ProcessDocumentWriteOff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceDocumentCode;

    private String sourceFieldKey;

    private String targetDocumentCode;

    private String targetTemplateType;

    private String writeoffSourceKind;

    private BigDecimal requestedAmount;

    private BigDecimal effectiveAmount;

    private BigDecimal availableSnapshotAmount;

    private BigDecimal remainingSnapshotAmount;

    private Integer sortOrder;

    private String status;

    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
