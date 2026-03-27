package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_custom_archive_item")
public class ProcessCustomArchiveItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    private String itemCode;

    private String itemName;

    private Integer priority;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
