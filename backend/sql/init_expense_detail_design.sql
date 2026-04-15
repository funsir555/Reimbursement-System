USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_expense_detail_design (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    detail_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_name VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    detail_description VARCHAR(500) NULL COMMENT 'column comment',
    schema_json LONGTEXT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_expense_detail_design_code (detail_code),
    KEY idx_pm_expense_detail_design_type (detail_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS pm_document_expense_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_no VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_design_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    enterprise_mode VARCHAR(32) NULL COMMENT 'column comment',
    expense_type_code VARCHAR(64) NULL COMMENT 'column comment',
    business_scene_mode VARCHAR(32) NULL COMMENT 'column comment',
    detail_title VARCHAR(128) NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    invoice_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    actual_payment_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    pending_write_off_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    schema_snapshot_json LONGTEXT NOT NULL COMMENT 'column comment',
    form_data_json LONGTEXT NOT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_expense_detail_doc_no (document_code, detail_no),
    KEY idx_pm_document_expense_detail_document (document_code, sort_order),
    KEY idx_pm_document_expense_detail_design (detail_design_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'SELECT 1',
        'ALTER TABLE pm_document_expense_detail DROP INDEX uk_pm_document_expense_detail_no'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'uk_pm_document_expense_detail_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD UNIQUE KEY uk_pm_document_expense_detail_doc_no (document_code, detail_no)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'uk_pm_document_expense_detail_doc_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_template ADD COLUMN expense_detail_design_code VARCHAR(64) NULL COMMENT ''column comment'' AFTER form_design_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_template'
      AND COLUMN_NAME = 'expense_detail_design_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_template ADD COLUMN expense_detail_mode_default VARCHAR(32) NULL COMMENT ''column comment'' AFTER expense_detail_design_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_template'
      AND COLUMN_NAME = 'expense_detail_mode_default'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- comment standardization begin
ALTER TABLE pm_document_template
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    MODIFY COLUMN company_id varchar(64) NULL COMMENT '公司主体编码',
    MODIFY COLUMN template_code varchar(64) NOT NULL COMMENT '模板编码',
    MODIFY COLUMN template_name varchar(64) NOT NULL COMMENT '模板名称',
    MODIFY COLUMN template_type varchar(32) NOT NULL COMMENT '模板类型:report/application/loan',
    MODIFY COLUMN template_type_label varchar(32) NOT NULL COMMENT '模板类型中文名',
    MODIFY COLUMN category_code varchar(64) NOT NULL COMMENT '分类编码',
    MODIFY COLUMN template_description varchar(500) NULL COMMENT '模板说明',
    MODIFY COLUMN numbering_rule varchar(64) NULL COMMENT '编号规则',
    MODIFY COLUMN form_design_code varchar(64) NULL COMMENT '表单设计编码',
    MODIFY COLUMN expense_detail_design_code varchar(64) NULL COMMENT '费用明细设计编码',
    MODIFY COLUMN icon_color varchar(32) NULL DEFAULT 'blue' COMMENT '图标颜色',
    MODIFY COLUMN enabled tinyint NULL DEFAULT 1 COMMENT '是否启用',
    MODIFY COLUMN publish_status varchar(16) NULL DEFAULT 'ENABLED' COMMENT '发布状态',
    MODIFY COLUMN print_mode varchar(64) NULL COMMENT '打印方式',
    MODIFY COLUMN approval_flow varchar(64) NULL COMMENT '审批流程编码',
    MODIFY COLUMN flow_name varchar(64) NULL COMMENT '审批流程名称',
    MODIFY COLUMN payment_mode varchar(64) NULL COMMENT '付款联动模式',
    MODIFY COLUMN split_payment tinyint NULL DEFAULT 0 COMMENT '是否支持分期付款:1是 0否',
    MODIFY COLUMN travel_form varchar(64) NULL COMMENT '出差申请配置',
    MODIFY COLUMN allocation_form varchar(64) NULL COMMENT '分摊表单',
    MODIFY COLUMN ai_audit_mode varchar(64) NULL DEFAULT 'disabled' COMMENT 'AI审核模式',
    MODIFY COLUMN relation_remark varchar(500) NULL COMMENT '关联规则说明',
    MODIFY COLUMN validation_remark varchar(500) NULL COMMENT '校验规则说明',
    MODIFY COLUMN installment_remark varchar(500) NULL COMMENT '分期付款说明',
    MODIFY COLUMN highlights varchar(500) NULL COMMENT '卡片亮点，使用|分隔',
    MODIFY COLUMN owner_name varchar(64) NULL COMMENT '负责人姓名',
    MODIFY COLUMN sort_order int NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN created_at datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN expense_detail_mode_default varchar(32) NULL COMMENT '企业往来默认模式',
    COMMENT = '单据流程模板表';

-- comment standardization end
