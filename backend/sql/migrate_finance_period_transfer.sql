USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS gl_period_transfer_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    rule_type VARCHAR(32) NOT NULL COMMENT '规则类型:PROFIT/CUSTOM/MANUFACTURE',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    transfer_granularity VARCHAR(32) NULL COMMENT '结转粒度:SUBJECT/ASSIST',
    subject_level INT NULL COMMENT '科目级次',
    source_subject_code VARCHAR(64) NULL COMMENT '默认转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '默认转出科目名称',
    target_subject_code VARCHAR(64) NULL COMMENT '默认转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '默认转入科目名称',
    allocation_mode VARCHAR(32) NULL COMMENT '分配方式:FULL/MANUAL_RATIO/PROJECT',
    regenerate_strategy VARCHAR(32) NOT NULL DEFAULT 'REPLACE_UNPOSTED' COMMENT '重生成策略',
    created_by VARCHAR(64) NULL COMMENT '创建人',
    updated_by VARCHAR(64) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gl_period_transfer_rule_company_type (company_id, rule_type),
    KEY idx_gl_period_transfer_rule_enabled (company_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转规则表';

CREATE TABLE IF NOT EXISTS gl_period_transfer_rule_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    line_no INT NOT NULL COMMENT '行号',
    source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    metric_type VARCHAR(32) NULL COMMENT '取数指标:ENDING_BALANCE/CURRENT_DEBIT/CURRENT_CREDIT/CURRENT_NET',
    ratio DECIMAL(18,6) NULL COMMENT '比例系数',
    allocation_project_class VARCHAR(2) NULL COMMENT '分配项目分类',
    allocation_project_id VARCHAR(6) NULL COMMENT '分配项目编码',
    allocation_project_name VARCHAR(128) NULL COMMENT '分配项目名称',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_rule_line_rule (rule_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转规则明细表';

CREATE TABLE IF NOT EXISTS gl_period_transfer_run (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_type VARCHAR(32) NOT NULL COMMENT '规则类型',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称快照',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    status VARCHAR(32) NOT NULL COMMENT '运行状态:PREVIEWED/GENERATED/CANCELLED',
    preview_token VARCHAR(64) NULL COMMENT '预览令牌',
    include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    voucher_no VARCHAR(128) NULL COMMENT '生成凭证号',
    generated_entry_count INT NOT NULL DEFAULT 0 COMMENT '生成分录数',
    skipped_entry_count INT NOT NULL DEFAULT 0 COMMENT '跳过分录数',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '生成总金额',
    blocked_message VARCHAR(500) NULL COMMENT '阻断或说明信息',
    rule_updated_at DATETIME NULL COMMENT '规则更新时间快照',
    previewed_by VARCHAR(64) NULL COMMENT '预览人',
    previewed_at DATETIME NULL COMMENT '预览时间',
    generated_by VARCHAR(64) NULL COMMENT '生成人',
    generated_at DATETIME NULL COMMENT '生成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_run_period (company_id, iyear, iperiod, rule_type),
    KEY idx_gl_period_transfer_run_rule (rule_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转运行表';

CREATE TABLE IF NOT EXISTS gl_period_transfer_run_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_line_id BIGINT NULL COMMENT '规则明细ID',
    line_no INT NOT NULL COMMENT '行号',
    source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    source_assist_key VARCHAR(255) NULL COMMENT '转出辅助维度键',
    source_assist_label VARCHAR(255) NULL COMMENT '转出辅助维度标签',
    target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    target_cdept_id VARCHAR(64) NULL COMMENT '目标部门编码',
    target_cperson_id VARCHAR(64) NULL COMMENT '目标人员编码',
    target_ccus_id VARCHAR(64) NULL COMMENT '目标客户编码',
    target_csup_id VARCHAR(64) NULL COMMENT '目标供应商编码',
    target_citem_class VARCHAR(2) NULL COMMENT '目标项目分类',
    target_citem_id VARCHAR(6) NULL COMMENT '目标项目编码',
    metric_type VARCHAR(32) NULL COMMENT '取数指标',
    direction VARCHAR(16) NULL COMMENT '方向:DEBIT/CREDIT',
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    skip_reason VARCHAR(255) NULL COMMENT '跳过原因',
    source_trace_key VARCHAR(255) NULL COMMENT '来源追溯键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_run_detail_run (run_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转运行明细表';

CREATE TABLE IF NOT EXISTS gl_period_transfer_voucher_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    run_detail_id BIGINT NOT NULL COMMENT '运行明细ID',
    voucher_no VARCHAR(128) NOT NULL COMMENT '凭证号',
    entry_no INT NOT NULL COMMENT '分录行号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_voucher_link_run (run_id),
    KEY idx_gl_period_transfer_voucher_link_voucher (voucher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转凭证关联表';

ALTER TABLE gl_period_transfer_rule
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN rule_type VARCHAR(32) NOT NULL COMMENT '规则类型:PROFIT/CUSTOM/MANUFACTURE',
    MODIFY COLUMN rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    MODIFY COLUMN voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    MODIFY COLUMN include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    MODIFY COLUMN transfer_granularity VARCHAR(32) NULL COMMENT '结转粒度:SUBJECT/ASSIST',
    MODIFY COLUMN subject_level INT NULL COMMENT '科目级次',
    MODIFY COLUMN source_subject_code VARCHAR(64) NULL COMMENT '默认转出科目编码',
    MODIFY COLUMN source_subject_name VARCHAR(128) NULL COMMENT '默认转出科目名称',
    MODIFY COLUMN target_subject_code VARCHAR(64) NULL COMMENT '默认转入科目编码',
    MODIFY COLUMN target_subject_name VARCHAR(128) NULL COMMENT '默认转入科目名称',
    MODIFY COLUMN allocation_mode VARCHAR(32) NULL COMMENT '分配方式:FULL/MANUAL_RATIO/PROJECT',
    MODIFY COLUMN regenerate_strategy VARCHAR(32) NOT NULL DEFAULT 'REPLACE_UNPOSTED' COMMENT '重生成策略',
    MODIFY COLUMN created_by VARCHAR(64) NULL COMMENT '创建人',
    MODIFY COLUMN updated_by VARCHAR(64) NULL COMMENT '更新人',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期末结转规则表';

ALTER TABLE gl_period_transfer_rule_line
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN rule_id BIGINT NOT NULL COMMENT '规则ID',
    MODIFY COLUMN line_no INT NOT NULL COMMENT '行号',
    MODIFY COLUMN source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    MODIFY COLUMN source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    MODIFY COLUMN target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    MODIFY COLUMN target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    MODIFY COLUMN metric_type VARCHAR(32) NULL COMMENT '取数指标:ENDING_BALANCE/CURRENT_DEBIT/CURRENT_CREDIT/CURRENT_NET',
    MODIFY COLUMN ratio DECIMAL(18,6) NULL COMMENT '比例系数',
    MODIFY COLUMN allocation_project_class VARCHAR(2) NULL COMMENT '分配项目分类',
    MODIFY COLUMN allocation_project_id VARCHAR(6) NULL COMMENT '分配项目编码',
    MODIFY COLUMN allocation_project_name VARCHAR(128) NULL COMMENT '分配项目名称',
    MODIFY COLUMN remark VARCHAR(255) NULL COMMENT '备注',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期末结转规则明细表';

ALTER TABLE gl_period_transfer_run
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN rule_id BIGINT NOT NULL COMMENT '规则ID',
    MODIFY COLUMN rule_type VARCHAR(32) NOT NULL COMMENT '规则类型',
    MODIFY COLUMN rule_name VARCHAR(128) NOT NULL COMMENT '规则名称快照',
    MODIFY COLUMN iyear INT NOT NULL COMMENT '会计年度',
    MODIFY COLUMN iperiod TINYINT NOT NULL COMMENT '会计期间',
    MODIFY COLUMN iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    MODIFY COLUMN status VARCHAR(32) NOT NULL COMMENT '运行状态:PREVIEWED/GENERATED/CANCELLED',
    MODIFY COLUMN preview_token VARCHAR(64) NULL COMMENT '预览令牌',
    MODIFY COLUMN include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    MODIFY COLUMN voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    MODIFY COLUMN voucher_no VARCHAR(128) NULL COMMENT '生成凭证号',
    MODIFY COLUMN generated_entry_count INT NOT NULL DEFAULT 0 COMMENT '生成分录数',
    MODIFY COLUMN skipped_entry_count INT NOT NULL DEFAULT 0 COMMENT '跳过分录数',
    MODIFY COLUMN total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '生成总金额',
    MODIFY COLUMN blocked_message VARCHAR(500) NULL COMMENT '阻断或说明信息',
    MODIFY COLUMN rule_updated_at DATETIME NULL COMMENT '规则更新时间快照',
    MODIFY COLUMN previewed_by VARCHAR(64) NULL COMMENT '预览人',
    MODIFY COLUMN previewed_at DATETIME NULL COMMENT '预览时间',
    MODIFY COLUMN generated_by VARCHAR(64) NULL COMMENT '生成人',
    MODIFY COLUMN generated_at DATETIME NULL COMMENT '生成时间',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期末结转运行表';

ALTER TABLE gl_period_transfer_run_detail
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN run_id BIGINT NOT NULL COMMENT '运行ID',
    MODIFY COLUMN rule_id BIGINT NOT NULL COMMENT '规则ID',
    MODIFY COLUMN rule_line_id BIGINT NULL COMMENT '规则明细ID',
    MODIFY COLUMN line_no INT NOT NULL COMMENT '行号',
    MODIFY COLUMN source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    MODIFY COLUMN source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    MODIFY COLUMN source_assist_key VARCHAR(255) NULL COMMENT '转出辅助维度键',
    MODIFY COLUMN source_assist_label VARCHAR(255) NULL COMMENT '转出辅助维度标签',
    MODIFY COLUMN target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    MODIFY COLUMN target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    MODIFY COLUMN target_cdept_id VARCHAR(64) NULL COMMENT '目标部门编码',
    MODIFY COLUMN target_cperson_id VARCHAR(64) NULL COMMENT '目标人员编码',
    MODIFY COLUMN target_ccus_id VARCHAR(64) NULL COMMENT '目标客户编码',
    MODIFY COLUMN target_csup_id VARCHAR(64) NULL COMMENT '目标供应商编码',
    MODIFY COLUMN target_citem_class VARCHAR(2) NULL COMMENT '目标项目分类',
    MODIFY COLUMN target_citem_id VARCHAR(6) NULL COMMENT '目标项目编码',
    MODIFY COLUMN metric_type VARCHAR(32) NULL COMMENT '取数指标',
    MODIFY COLUMN direction VARCHAR(16) NULL COMMENT '方向:DEBIT/CREDIT',
    MODIFY COLUMN amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    MODIFY COLUMN skip_reason VARCHAR(255) NULL COMMENT '跳过原因',
    MODIFY COLUMN source_trace_key VARCHAR(255) NULL COMMENT '来源追溯键',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期末结转运行明细表';

ALTER TABLE gl_period_transfer_voucher_link
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    MODIFY COLUMN run_id BIGINT NOT NULL COMMENT '运行ID',
    MODIFY COLUMN run_detail_id BIGINT NOT NULL COMMENT '运行明细ID',
    MODIFY COLUMN voucher_no VARCHAR(128) NOT NULL COMMENT '凭证号',
    MODIFY COLUMN entry_no INT NOT NULL COMMENT '分录行号',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT='总账期末结转凭证关联表';

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
    SELECT 'finance:general_ledger:period_transfer:view' AS permission_code, '期末结转' AS permission_name, 'MENU' AS permission_type, 'finance:general_ledger:menu' AS parent_code, 'finance' AS module_code, '/finance/general-ledger/period-transfer' AS route_path, 40137 AS sort_order
    UNION ALL
    SELECT 'finance:general_ledger:period_transfer:manage', '维护期末结转规则', 'BUTTON', 'finance:general_ledger:period_transfer:view', 'finance', NULL, 401371
    UNION ALL
    SELECT 'finance:general_ledger:period_transfer:generate', '生成期末结转凭证', 'BUTTON', 'finance:general_ledger:period_transfer:view', 'finance', NULL, 401372
) seed
JOIN sys_permission parent
    ON parent.permission_code = seed.parent_code
LEFT JOIN sys_permission permission
    ON permission.permission_code = seed.permission_code
WHERE permission.id IS NULL;

UPDATE sys_permission permission
JOIN (
    SELECT 'finance:general_ledger:period_transfer:view' AS permission_code, '期末结转' AS permission_name, 'MENU' AS permission_type, 'finance' AS module_code, '/finance/general-ledger/period-transfer' AS route_path, 40137 AS sort_order
    UNION ALL
    SELECT 'finance:general_ledger:period_transfer:manage', '维护期末结转规则', 'BUTTON', 'finance', NULL, 401371
    UNION ALL
    SELECT 'finance:general_ledger:period_transfer:generate', '生成期末结转凭证', 'BUTTON', 'finance', NULL, 401372
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
    ON permission.permission_code IN (
        'finance:general_ledger:period_transfer:view',
        'finance:general_ledger:period_transfer:manage',
        'finance:general_ledger:period_transfer:generate'
    )
WHERE role.role_code = 'SUPER_ADMIN';
