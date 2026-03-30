package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_document_task")
public class ProcessDocumentTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentCode;

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private Long assigneeUserId;

    private String assigneeName;

    private String status;

    private String taskBatchNo;

    private String approvalMode;

    private String actionComment;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
