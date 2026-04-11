package com.finex.auth.service.impl.profile;

import com.finex.auth.dto.DownloadCenterVO;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import com.finex.auth.service.UserCenterService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.DownloadStorageService;
import com.finex.auth.support.AsyncTaskSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileDownloadDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private DownloadStorageService downloadStorageService;

    private ProfileDownloadDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new ProfileDownloadDomainSupport(
                userService,
                userBankAccountMapper,
                downloadRecordMapper,
                downloadStorageService
        );
    }

    @Test
    void getDownloadCenterSplitsInProgressAndHistory() {
        DownloadRecord downloading = new DownloadRecord();
        downloading.setId(1L);
        downloading.setUserId(5L);
        downloading.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloading.setCreatedAt(LocalDateTime.of(2026, 4, 10, 9, 0, 0));

        DownloadRecord completed = new DownloadRecord();
        completed.setId(2L);
        completed.setUserId(5L);
        completed.setFileName("done.xlsx");
        completed.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED);
        completed.setFinishedAt(LocalDateTime.of(2026, 4, 10, 9, 5, 0));

        when(downloadRecordMapper.selectList(any())).thenReturn(List.of(downloading, completed));
        when(downloadStorageService.exists(2L)).thenReturn(true);

        DownloadCenterVO result = support.getDownloadCenter(5L);

        assertEquals(1, result.getInProgress().size());
        assertEquals(1, result.getHistory().size());
        assertTrue(result.getHistory().get(0).getDownloadable());
    }

    @Test
    void loadDownloadContentRejectsIncompleteRecord() {
        DownloadRecord record = new DownloadRecord();
        record.setId(2L);
        record.setUserId(5L);
        record.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);

        when(downloadRecordMapper.selectOne(any())).thenReturn(record);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.loadDownloadContent(5L, 2L)
        );

        assertEquals("下载文件尚未生成完成", error.getMessage());
    }

    @Test
    void loadDownloadContentReturnsResourceForCompletedFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("download-3.xlsx");
        Files.writeString(file, "xlsx");

        DownloadRecord record = new DownloadRecord();
        record.setId(3L);
        record.setUserId(5L);
        record.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED);
        record.setFileName("report.xlsx");

        when(downloadRecordMapper.selectOne(any())).thenReturn(record);
        when(downloadStorageService.resolvePath(3L)).thenReturn(file);

        UserCenterService.DownloadContent content = support.loadDownloadContent(5L, 3L);

        assertEquals("report.xlsx", content.fileName());
        assertEquals(4L, content.contentLength());
        Resource resource = content.resource();
        assertTrue(resource.exists());
    }
}
