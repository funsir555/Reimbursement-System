package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pm_document_expense_detail")
public class ProcessDocumentExpenseDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentCode;

    private String detailNo;

    private String detailDesignCode;

    private String detailType;

    private String enterpriseMode;

    private String expenseTypeCode;

    private String businessSceneMode;

    private String detailTitle;

    private Integer sortOrder;

    private BigDecimal invoiceAmount;

    private BigDecimal actualPaymentAmount;

    private BigDecimal pendingWriteOffAmount;

    private String schemaSnapshotJson;

    private String formDataJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
