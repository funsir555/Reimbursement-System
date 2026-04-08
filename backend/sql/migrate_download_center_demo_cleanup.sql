USE finex_db;

SET NAMES utf8mb4;

/*
一次性清理下载中心的初始化演示数据，避免右上角“正在下载”长期显示假任务。
只删除已确认的 seed 记录，不影响后续真实异步导出生成的下载记录。
*/

DELETE FROM sys_download_record
WHERE (
        file_name = CONVERT(0x33E69C88E68AA5E99480E58D95E5AFBCE587BA2E786C7378 USING utf8mb4)
    AND business_type = CONVERT(0xE68AA5E99480E6988EE7BB86E5AFBCE587BA USING utf8mb4)
    AND status = 'DOWNLOADING'
    AND progress = 68
      )
   OR (
        file_name = CONVERT(0xE5BE85E5AEA1E689B9E58D95E68DAEE6B885E58D952E786C7378 USING utf8mb4)
    AND business_type = CONVERT(0xE5AEA1E689B9E6B885E58D95E5AFBCE587BA USING utf8mb4)
    AND status = 'COMPLETED'
    AND progress = 100
      )
   OR (
        file_name = CONVERT(0xE58F91E7A5A8E9AA8CE79C9FE7BB93E69E9C2E637376 USING utf8mb4)
    AND business_type = CONVERT(0xE58F91E7A5A8E7AEA1E79086E5AFBCE587BA USING utf8mb4)
    AND status = 'COMPLETED'
    AND progress = 100
      );
