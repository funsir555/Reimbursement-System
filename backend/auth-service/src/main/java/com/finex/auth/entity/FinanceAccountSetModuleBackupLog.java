package com.finex.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fin_account_set_module_backup_log")
public class FinanceAccountSetModuleBackupLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private String companyId;

    @TableField("module_code")
    private String moduleCode;

    @TableField("backup_file_name")
    private String backupFileName;

    @TableField("backup_file_path")
    private String backupFilePath;

    @TableField("backup_status")
    private String backupStatus;

    @TableField("backup_started_at")
    private LocalDateTime backupStartedAt;

    @TableField("backup_finished_at")
    private LocalDateTime backupFinishedAt;

    @TableField("backup_user_id")
    private Long backupUserId;

    @TableField("backup_user_name")
    private String backupUserName;

    @TableField("file_size_bytes")
    private Long fileSizeBytes;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
