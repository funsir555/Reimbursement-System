USE finex_db;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS gl_period_close_log;
DROP TABLE IF EXISTS gl_period_close;

CREATE TABLE gl_period_close (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    status VARCHAR(32) NOT NULL COMMENT '结账状态',
    closed_by VARCHAR(64) NULL COMMENT '结账人',
    closed_at DATETIME NULL COMMENT '结账时间',
    reopened_by VARCHAR(64) NULL COMMENT '反结账人',
    reopened_at DATETIME NULL COMMENT '反结账时间',
    close_note VARCHAR(500) NULL COMMENT '结账备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gl_period_close_period (company_id, iyear, iperiod),
    KEY idx_gl_period_close_status (status),
    KEY idx_gl_period_close_iyperiod (iyperiod)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期间结账状态表';

CREATE TABLE gl_period_close_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    action_type VARCHAR(32) NOT NULL COMMENT '日志动作类型',
    action_status VARCHAR(32) NOT NULL COMMENT '日志结果状态',
    operator_name VARCHAR(64) NULL COMMENT '操作人',
    message VARCHAR(1000) NULL COMMENT '结果说明',
    detail_json TEXT NULL COMMENT '结构化详情',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_close_log_period (company_id, iyear, iperiod),
    KEY idx_gl_period_close_log_action (action_type, action_status),
    KEY idx_gl_period_close_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期间结账日志表';
