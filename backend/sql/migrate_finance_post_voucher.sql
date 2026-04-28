USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS gl_post_state (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    status VARCHAR(32) NOT NULL COMMENT '记账状态',
    posted_voucher_count INT NOT NULL DEFAULT 0 COMMENT '已记账凭证数',
    last_task_no VARCHAR(64) NULL COMMENT '最近任务号',
    last_task_status VARCHAR(32) NULL COMMENT '最近任务状态',
    last_error_message VARCHAR(1000) NULL COMMENT '最近失败信息',
    last_posted_by VARCHAR(64) NULL COMMENT '最近记账人',
    last_posted_at DATETIME NULL COMMENT '最近记账时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gl_post_state_period (company_id, iyear, iperiod),
    KEY idx_gl_post_state_status (status),
    KEY idx_gl_post_state_task_no (last_task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账记账期间状态表';

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'gl_accsum'
      AND index_name = 'uk_gl_accsum_period_subject_currency'
);

SET @sql = IF(
    @idx_exists > 0,
    'SELECT ''uk_gl_accsum_period_subject_currency exists''',
    'ALTER TABLE gl_accsum ADD UNIQUE KEY uk_gl_accsum_period_subject_currency (company_id, iyear, iperiod, ccode, currency_code)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'gl_accass'
      AND index_name = 'idx_gl_accass_period_subject_currency_dims'
);

SET @sql = IF(
    @idx_exists > 0,
    'SELECT ''idx_gl_accass_period_subject_currency_dims exists''',
    'ALTER TABLE gl_accass ADD KEY idx_gl_accass_period_subject_currency_dims (company_id, iyear, iperiod, ccode, currency_code, cdept_id, cperson_id, ccus_id, csup_id, citem_class, citem_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
