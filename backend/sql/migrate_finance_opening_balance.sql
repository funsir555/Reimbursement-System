USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS gl_opening_balance_state (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    status VARCHAR(32) NOT NULL COMMENT '开账状态',
    source_type VARCHAR(32) COMMENT '来源类型',
    opened_by VARCHAR(64) COMMENT '开账人',
    opened_at DATETIME COMMENT '开账时间',
    last_trial_at DATETIME COMMENT '最近试算时间',
    last_reconcile_at DATETIME COMMENT '最近对账时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gl_opening_balance_state_period (company_id, iyear, iperiod),
    KEY idx_gl_opening_balance_state_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期初余额开账状态表';
