package com.finex.auth.service.impl.financesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FinanceModuleBackupStorageService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("${finex.finance.module-backup.storage-path:${user.dir}/storage/finance-module-backups}")
    private String storagePath;

    public Path resolvePath(String companyId, String moduleCode, LocalDateTime backupTime) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("模块编码不能为空");
        }
        LocalDateTime effectiveTime = backupTime == null ? LocalDateTime.now() : backupTime;
        String fileName = buildFileName(companyId, moduleCode, effectiveTime);
        return resolveRoot()
                .resolve(companyId.trim())
                .resolve(moduleCode.trim().toLowerCase(Locale.ROOT))
                .resolve(fileName);
    }

    public Path writeSql(String companyId, String moduleCode, LocalDateTime backupTime, String sqlContent) {
        Path target = resolvePath(companyId, moduleCode, backupTime);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, sqlContent == null ? "" : sqlContent, StandardCharsets.UTF_8);
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("写入模块备份文件失败", ex);
        }
    }

    public boolean exists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        return Files.exists(Path.of(filePath.trim()).toAbsolutePath().normalize());
    }

    private Path resolveRoot() {
        try {
            Path root = Path.of(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("初始化模块备份目录失败", ex);
        }
    }

    private String buildFileName(String companyId, String moduleCode, LocalDateTime backupTime) {
        return companyId.trim() + "-" + moduleCode.trim() + "-" + FILE_TIME_FORMATTER.format(backupTime) + ".sql";
    }
}
