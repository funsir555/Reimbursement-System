package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_document_action_log")
public class ProcessDocumentActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String documentCode;

    private String nodeKey;

    private String nodeName;

    private String actionType;

    private Long actorUserId;

    private String actorName;

    private String actionComment;

    private String payloadJson;

    private LocalDateTime createdAt;
}
