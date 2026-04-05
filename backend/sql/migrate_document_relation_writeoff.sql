USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_document_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    source_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    source_field_key VARCHAR(128) NOT NULL COMMENT 'column comment',
    target_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    target_template_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_relation_source_target (source_document_code, source_field_key, target_document_code),
    KEY idx_pm_document_relation_source (source_document_code, source_field_key, status),
    KEY idx_pm_document_relation_target (target_document_code, target_template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS pm_document_write_off (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    source_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    source_field_key VARCHAR(128) NOT NULL COMMENT 'column comment',
    target_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    target_template_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    writeoff_source_kind VARCHAR(32) NOT NULL COMMENT 'column comment',
    requested_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'column comment',
    effective_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    available_snapshot_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    remaining_snapshot_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_EFFECTIVE' COMMENT 'column comment',
    effective_at DATETIME NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_write_off_source_target (source_document_code, source_field_key, target_document_code),
    KEY idx_pm_document_write_off_source (source_document_code, source_field_key, status),
    KEY idx_pm_document_write_off_target (target_document_code, target_template_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';
