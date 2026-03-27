package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_custom_archive_rule")
public class ProcessCustomArchiveRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveItemId;

    private Integer groupNo;

    private String fieldKey;

    private String operator;

    private String compareValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
