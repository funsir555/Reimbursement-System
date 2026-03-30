package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pm_document_instance")
public class ProcessDocumentInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentCode;
    private String templateCode;
    private String templateName;
    private String templateType;
    private String formDesignCode;
    private String approvalFlowCode;
    private String flowName;
    private Long submitterUserId;
    private String submitterName;
    private String documentTitle;
    private String documentReason;
    private BigDecimal totalAmount;
    private String status;
    private String currentNodeKey;
    private String currentNodeName;
    private String currentTaskType;
    private String formDataJson;
    private String templateSnapshotJson;
    private String formSchemaSnapshotJson;
    private String flowSnapshotJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
}
