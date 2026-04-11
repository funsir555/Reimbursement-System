package com.finex.auth.service.impl.profile;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.dto.DownloadRecordVO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserCenterService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;
import com.finex.auth.support.AsyncTaskSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ProfileDownloadDomainSupport extends AbstractProfileDomainSupport {

    public ProfileDownloadDomainSupport(
            UserService userService,
            UserBankAccountMapper userBankAccountMapper,
            DownloadRecordMapper downloadRecordMapper,
            DownloadStorageService downloadStorageService
    ) {
        super(userService, userBankAccountMapper, downloadRecordMapper, downloadStorageService);
    }

    public DownloadCenterVO getDownloadCenter(Long userId) {
        List<DownloadRecord> records = downloadRecordMapper().selectList(
                Wrappers.<DownloadRecord>lambdaQuery()
                        .eq(DownloadRecord::getUserId, userId)
                        .orderByDesc(DownloadRecord::getCreatedAt, DownloadRecord::getId)
                        .last("limit 20")
        );

        DownloadCenterVO center = new DownloadCenterVO();
        center.setInProgress(records.stream()
                .filter(this::isDownloading)
                .map(this::toDownloadRecord)
                .toList());
        center.setHistory(records.stream()
                .filter(record -> !isDownloading(record))
                .map(this::toDownloadRecord)
                .toList());
        return center;
    }

    public UserCenterService.DownloadContent loadDownloadContent(Long userId, Long downloadId) {
        DownloadRecord record = requireDownloadRecord(userId, downloadId);
        if (!AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED.equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("下载文件尚未生成完成");
        }
        Path path = downloadStorageService().resolvePath(record.getId());
        if (!Files.exists(path)) {
            throw new IllegalStateException("下载文件不存在或已失效");
        }
        Resource resource = new FileSystemResource(path);
        try {
            return new UserCenterService.DownloadContent(resource, record.getFileName(), Files.size(path));
        } catch (Exception ex) {
            throw new IllegalStateException("读取下载文件失败", ex);
        }
    }

    private DownloadRecord requireDownloadRecord(Long userId, Long downloadId) {
        DownloadRecord record = downloadRecordMapper().selectOne(
                Wrappers.<DownloadRecord>lambdaQuery()
                        .eq(DownloadRecord::getId, downloadId)
                        .eq(DownloadRecord::getUserId, userId)
                        .last("limit 1")
        );
        if (record == null) {
            throw new IllegalArgumentException("下载记录不存在");
        }
        return record;
    }

    private DownloadRecordVO toDownloadRecord(DownloadRecord record) {
        boolean downloadable = isDownloadable(record);
        DownloadRecordVO vo = new DownloadRecordVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setBusinessType(record.getBusinessType());
        vo.setStatus(resolveDownloadStatus(record.getStatus()));
        vo.setProgress(record.getProgress() == null ? 0 : record.getProgress());
        vo.setFileSize(StrUtil.blankToDefault(record.getFileSize(), "-"));
        vo.setCreatedAt(record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DATE_TIME_FORMATTER));
        vo.setFinishedAt(record.getFinishedAt() == null ? "" : record.getFinishedAt().format(DATE_TIME_FORMATTER));
        vo.setDownloadable(downloadable);
        vo.setDownloadUrl(downloadable ? "/auth/user-center/downloads/" + record.getId() + "/content" : null);
        return vo;
    }

    private boolean isDownloading(DownloadRecord record) {
        return AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING.equalsIgnoreCase(record.getStatus());
    }

    private boolean isDownloadable(DownloadRecord record) {
        return AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED.equalsIgnoreCase(record.getStatus())
                && downloadStorageService().exists(record.getId());
    }

    private String resolveDownloadStatus(String status) {
        if (AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING.equalsIgnoreCase(status)) {
            return "下载中";
        }
        if (AsyncTaskSupport.DOWNLOAD_STATUS_FAILED.equalsIgnoreCase(status)) {
            return "下载失败";
        }
        return "已完成";
    }
}
