package com.finex.auth.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FinanceModuleBackupRecordVO {

    private Long id;

    private String companyId;

    private String moduleCode;

    private String moduleName;

    private String backupFileName;

    private String backupFilePath;

    private String backupStatus;

    private LocalDateTime backupStartedAt;

    private LocalDateTime backupFinishedAt;

    private Long backupUserId;

    private String backupUserName;

    private Long fileSizeBytes;

    private String remark;
}
