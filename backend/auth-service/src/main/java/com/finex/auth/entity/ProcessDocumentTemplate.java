package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_document_template")
public class ProcessDocumentTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String templateTypeLabel;

    private String categoryCode;

    private String templateDescription;

    private String numberingRule;

    private String formDesignCode;

    private String iconColor;

    private Integer enabled;

    private String publishStatus;

    private String printMode;

    private String approvalFlow;

    private String flowName;

    private String paymentMode;

    private String allocationForm;

    private String aiAuditMode;

    private String highlights;

    private String ownerName;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
