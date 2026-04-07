package com.finex.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DownloadStorageService {

    @Value("${finex.downloads.storage-path:${user.dir}/storage/downloads}")
    private String storagePath;

    public Path writeWorkbook(Long downloadRecordId, byte[] bytes) {
        Path target = resolvePath(downloadRecordId);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return target;
        } catch (IOException ex) {
            throw new IllegalStateException("写入下载文件失败", ex);
        }
    }

    public Path resolvePath(Long downloadRecordId) {
        if (downloadRecordId == null) {
            throw new IllegalArgumentException("下载记录不存在");
        }
        return resolveRoot().resolve("download-" + downloadRecordId + ".xlsx");
    }

    public boolean exists(Long downloadRecordId) {
        return Files.exists(resolvePath(downloadRecordId));
    }

    public void deleteIfExists(Long downloadRecordId) {
        if (downloadRecordId == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolvePath(downloadRecordId));
        } catch (IOException ignored) {
            // Keep the original task error if cleanup fails.
        }
    }

    private Path resolveRoot() {
        try {
            Path root = Path.of(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("初始化下载目录失败", ex);
        }
    }
}
