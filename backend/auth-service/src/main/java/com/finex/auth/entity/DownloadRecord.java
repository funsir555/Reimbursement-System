package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_download_record")
public class DownloadRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fileName;

    private String businessType;

    private String status;

    private Integer progress;

    private String fileSize;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}
