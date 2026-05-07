USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''是否软删除:1是 0否'' AFTER flow_snapshot_json',
        'SELECT ''pm_document_instance.deleted exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'deleted'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN deleted_at DATETIME NULL COMMENT ''删除时间'' AFTER deleted',
        'SELECT ''pm_document_instance.deleted_at exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'deleted_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN deleted_by BIGINT NULL COMMENT ''删除人用户ID'' AFTER deleted_at',
        'SELECT ''pm_document_instance.deleted_by exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'deleted_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_pm_document_instance_deleted ON pm_document_instance (deleted)',
        'SELECT ''idx_pm_document_instance_deleted exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'pm_document_instance'
      AND INDEX_NAME = 'idx_pm_document_instance_deleted'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
