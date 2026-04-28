USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS gl_period_close (
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

CREATE TABLE IF NOT EXISTS gl_period_close_log (
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

ALTER TABLE gl_period_close
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN iyear INT NOT NULL COMMENT '会计年度',
    MODIFY COLUMN iperiod TINYINT NOT NULL COMMENT '会计期间',
    MODIFY COLUMN iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '结账状态',
    MODIFY COLUMN closed_by VARCHAR(64) NULL COMMENT '结账人',
    MODIFY COLUMN closed_at DATETIME NULL COMMENT '结账时间',
    MODIFY COLUMN reopened_by VARCHAR(64) NULL COMMENT '反结账人',
    MODIFY COLUMN reopened_at DATETIME NULL COMMENT '反结账时间',
    MODIFY COLUMN close_note VARCHAR(500) NULL COMMENT '结账备注',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期间结账状态表';

ALTER TABLE gl_period_close_log
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN iyear INT NOT NULL COMMENT '会计年度',
    MODIFY COLUMN iperiod TINYINT NOT NULL COMMENT '会计期间',
    MODIFY COLUMN iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    MODIFY COLUMN action_type VARCHAR(32) NOT NULL COMMENT '日志动作类型',
    MODIFY COLUMN action_status VARCHAR(32) NOT NULL COMMENT '日志结果状态',
    MODIFY COLUMN operator_name VARCHAR(64) NULL COMMENT '操作人',
    MODIFY COLUMN message VARCHAR(1000) NULL COMMENT '结果说明',
    MODIFY COLUMN detail_json TEXT NULL COMMENT '结构化详情',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    COMMENT='总账期间结账日志表';

INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT
    seed.permission_code,
    seed.permission_name,
    seed.permission_type,
    parent.id,
    seed.module_code,
    seed.route_path,
    seed.sort_order,
    1
FROM (
    SELECT
        'finance:general_ledger:close_ledger:close' AS permission_code,
        '执行结账' AS permission_name,
        'BUTTON' AS permission_type,
        'finance:general_ledger:close_ledger:view' AS parent_code,
        'finance' AS module_code,
        NULL AS route_path,
        401361 AS sort_order
) seed
JOIN sys_permission parent
    ON parent.permission_code = seed.parent_code
LEFT JOIN sys_permission permission
    ON permission.permission_code = seed.permission_code
WHERE permission.id IS NULL;

UPDATE sys_permission permission
JOIN (
    SELECT
        'finance:general_ledger:close_ledger:close' AS permission_code,
        '执行结账' AS permission_name,
        'BUTTON' AS permission_type,
        'finance' AS module_code,
        NULL AS route_path,
        401361 AS sort_order
) seed
    ON seed.permission_code = permission.permission_code
SET permission.permission_name = seed.permission_name,
    permission.permission_type = seed.permission_type,
    permission.module_code = seed.module_code,
    permission.route_path = seed.route_path,
    permission.sort_order = seed.sort_order,
    permission.status = 1;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
    ON permission.permission_code = 'finance:general_ledger:close_ledger:close'
WHERE role.role_code = 'SUPER_ADMIN';
