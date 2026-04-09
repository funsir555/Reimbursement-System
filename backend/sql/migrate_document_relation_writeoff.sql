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

-- comment standardization begin
ALTER TABLE pm_document_relation
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN source_document_code varchar(64) NOT NULL COMMENT '源单据编码',
    MODIFY COLUMN source_field_key varchar(128) NOT NULL COMMENT '源字段标识',
    MODIFY COLUMN target_document_code varchar(64) NOT NULL COMMENT '目标单据编码',
    MODIFY COLUMN target_template_type varchar(32) NOT NULL COMMENT '目标模板类型',
    MODIFY COLUMN sort_order int NOT NULL DEFAULT 1 COMMENT '排序号',
    MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE启用/DISABLED停用',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '单据关联关系表';

ALTER TABLE pm_document_write_off
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN source_document_code varchar(64) NOT NULL COMMENT '源单据编码',
    MODIFY COLUMN source_field_key varchar(128) NOT NULL COMMENT '源字段标识',
    MODIFY COLUMN target_document_code varchar(64) NOT NULL COMMENT '目标单据编码',
    MODIFY COLUMN target_template_type varchar(32) NOT NULL COMMENT '目标模板类型',
    MODIFY COLUMN writeoff_source_kind varchar(32) NOT NULL COMMENT '核销来源类型',
    MODIFY COLUMN requested_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '申请金额',
    MODIFY COLUMN effective_amount decimal(18,2) NULL COMMENT '生效金额',
    MODIFY COLUMN available_snapshot_amount decimal(18,2) NULL COMMENT '可用快照金额',
    MODIFY COLUMN remaining_snapshot_amount decimal(18,2) NULL COMMENT '剩余快照金额',
    MODIFY COLUMN sort_order int NOT NULL DEFAULT 1 COMMENT '排序号',
    MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'PENDING_EFFECTIVE' COMMENT '状态:PENDING_EFFECTIVE待生效/EFFECTIVE已生效/CANCELLED已取消',
    MODIFY COLUMN effective_at datetime NULL COMMENT '生效时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '单据核销关系表';

-- comment standardization end
