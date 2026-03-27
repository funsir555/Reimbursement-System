package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_custom_archive_design")
public class ProcessCustomArchiveDesign {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String archiveCode;

    private String archiveName;

    private String archiveType;

    private String archiveDescription;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
