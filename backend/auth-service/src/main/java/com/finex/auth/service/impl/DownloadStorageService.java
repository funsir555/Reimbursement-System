// 业务域：个人中心与下载
// 文件角色：存储支撑类
// 上下游关系：上游通常来自 个人中心页面、下载中心接口，下游会继续协调 个人信息、银行卡账户和下载记录。
// 风险提醒：改坏后最容易影响 个人信息展示、银行卡维护和下载留痕。

package com.finex.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DownloadStorageService：存储支撑类。
 * 封装 下载这块可复用的业务能力。
 * 改这里时，要特别关注 个人信息展示、银行卡维护和下载留痕是否会被一起带坏。
 */
@Service
public class DownloadStorageService {

    @Value("${finex.downloads.storage-path:${user.dir}/storage/downloads}")
    private String storagePath;

    /**
     * 处理下载中的这一步。
     */
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

    /**
     * 解析Path。
     */
    public Path resolvePath(Long downloadRecordId) {
        if (downloadRecordId == null) {
            throw new IllegalArgumentException("下载记录不存在");
        }
        return resolveRoot().resolve("download-" + downloadRecordId + ".xlsx");
    }

    /**
     * 判断当前业务数据是否已存在。
     */
    public boolean exists(Long downloadRecordId) {
        return Files.exists(resolvePath(downloadRecordId));
    }

    /**
     * 删除IfExists。
     */
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

    /**
     * 解析Root。
     */
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
