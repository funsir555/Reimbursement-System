USE finex_db;

SET NAMES utf8mb4;


CREATE TABLE IF NOT EXISTS fa_asset_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    share_scope VARCHAR(16) NOT NULL DEFAULT 'COMPANY',
    depreciation_method VARCHAR(32) NOT NULL,
    useful_life_months INT NOT NULL,
    residual_rate DECIMAL(10,4) NOT NULL DEFAULT 0.0500,
    depreciable TINYINT(1) NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_category_code (company_id, category_code),
    KEY idx_fa_asset_category_scope (company_id, share_scope),
    KEY idx_fa_asset_category_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset category';

CREATE TABLE IF NOT EXISTS fa_asset_account_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    category_id BIGINT NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    asset_account VARCHAR(64) NOT NULL,
    accum_depr_account VARCHAR(64) NOT NULL,
    depr_expense_account VARCHAR(64) NOT NULL,
    disposal_account VARCHAR(64) NOT NULL,
    gain_account VARCHAR(64) NOT NULL,
    loss_account VARCHAR(64) NOT NULL,
    offset_account VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_account_policy (company_id, category_id, book_code),
    KEY idx_fa_asset_account_policy_category (company_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset account policy';

CREATE TABLE IF NOT EXISTS fa_asset_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
    category_id BIGINT NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    use_company_id VARCHAR(64) NOT NULL,
    use_dept_id BIGINT NULL,
    keeper_user_id BIGINT NULL,
    manager_user_id BIGINT NULL,
    source_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    acquire_date DATE NULL,
    in_service_date DATE NOT NULL,
    original_amount DECIMAL(18,2) NOT NULL,
    accum_depr_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    salvage_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    useful_life_months INT NOT NULL,
    depreciated_months INT NOT NULL DEFAULT 0,
    remaining_months INT NOT NULL DEFAULT 0,
    work_total DECIMAL(18,6) NULL,
    work_used DECIMAL(18,6) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    can_depreciate TINYINT(1) NOT NULL DEFAULT 1,
    last_depr_year INT NULL,
    last_depr_period INT NULL,
    remark VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_card_code (company_id, asset_code),
    KEY idx_fa_asset_card_category (company_id, category_id),
    KEY idx_fa_asset_card_status (company_id, status),
    KEY idx_fa_asset_card_book (company_id, book_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset card';

CREATE TABLE IF NOT EXISTS fa_asset_change_bill (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    bill_no VARCHAR(32) NOT NULL,
    bill_type VARCHAR(32) NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    fiscal_year INT NOT NULL,
    fiscal_period INT NOT NULL,
    bill_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    remark VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    posted_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    posted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_change_bill (company_id, bill_no, bill_type),
    KEY idx_fa_asset_change_bill_period (company_id, fiscal_year, fiscal_period, book_code),
    KEY idx_fa_asset_change_bill_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset change bill';

CREATE TABLE IF NOT EXISTS fa_asset_change_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    asset_id BIGINT NULL,
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NULL,
    change_type VARCHAR(32) NOT NULL,
    category_id BIGINT NULL,
    category_code VARCHAR(32) NULL,
    use_company_id VARCHAR(64) NULL,
    use_dept_id BIGINT NULL,
    keeper_user_id BIGINT NULL,
    in_service_date DATE NULL,
    change_amount DECIMAL(18,2) NULL,
    old_value DECIMAL(18,2) NULL,
    new_value DECIMAL(18,2) NULL,
    old_salvage_amount DECIMAL(18,2) NULL,
    new_salvage_amount DECIMAL(18,2) NULL,
    old_useful_life_months INT NULL,
    new_useful_life_months INT NULL,
    old_remaining_months INT NULL,
    new_remaining_months INT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fa_asset_change_line_bill (company_id, bill_id),
    KEY idx_fa_asset_change_line_asset (company_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset change line';

CREATE TABLE IF NOT EXISTS fa_asset_depr_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    run_no VARCHAR(32) NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    fiscal_year INT NOT NULL,
    fiscal_period INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    asset_count INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    remark VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    posted_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    posted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_depr_run (company_id, run_no),
    KEY idx_fa_asset_depr_run_period (company_id, fiscal_year, fiscal_period, book_code),
    KEY idx_fa_asset_depr_run_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset depreciation run';

CREATE TABLE IF NOT EXISTS fa_asset_depr_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    run_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
    category_id BIGINT NOT NULL,
    depreciation_method VARCHAR(32) NOT NULL,
    work_amount DECIMAL(18,6) NULL,
    depreciation_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    before_accum_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    after_accum_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    before_net_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    after_net_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fa_asset_depr_line_run (company_id, run_id),
    KEY idx_fa_asset_depr_line_asset (company_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset depreciation line';

CREATE TABLE IF NOT EXISTS fa_asset_disposal_bill (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    bill_no VARCHAR(32) NOT NULL,
    bill_type VARCHAR(32) NOT NULL DEFAULT 'DISPOSAL',
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    fiscal_year INT NOT NULL,
    fiscal_period INT NOT NULL,
    bill_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    total_original_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_accum_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    total_net_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    remark VARCHAR(255) NULL,
    created_by VARCHAR(64) NULL,
    posted_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    posted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_disposal_bill (company_id, bill_no, bill_type),
    KEY idx_fa_asset_disposal_bill_period (company_id, fiscal_year, fiscal_period, book_code),
    KEY idx_fa_asset_disposal_bill_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset disposal bill';

CREATE TABLE IF NOT EXISTS fa_asset_disposal_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_code VARCHAR(32) NOT NULL,
    asset_name VARCHAR(64) NOT NULL,
    category_id BIGINT NOT NULL,
    original_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    accum_depr_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fa_asset_disposal_line_bill (company_id, bill_id),
    KEY idx_fa_asset_disposal_line_asset (company_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset disposal line';

CREATE TABLE IF NOT EXISTS fa_asset_opening_import (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_no VARCHAR(32) NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    fiscal_year INT NOT NULL,
    fiscal_period INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_opening_import_batch (company_id, batch_no),
    KEY idx_fa_asset_opening_import_period (company_id, fiscal_year, fiscal_period, book_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset opening import batch';

CREATE TABLE IF NOT EXISTS fa_asset_opening_import_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    asset_code VARCHAR(32) NULL,
    asset_name VARCHAR(64) NULL,
    category_code VARCHAR(32) NULL,
    result_status VARCHAR(16) NOT NULL,
    error_message VARCHAR(500) NULL,
    imported_asset_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fa_asset_opening_import_line_batch (company_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset opening import line';

CREATE TABLE IF NOT EXISTS fa_asset_voucher_link (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id BIGINT NOT NULL,
    voucher_no VARCHAR(128) NOT NULL,
    iperiod INT NOT NULL,
    csign VARCHAR(32) NOT NULL,
    ino_id INT NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_voucher_link (company_id, business_type, business_id),
    KEY idx_fa_asset_voucher_link_voucher (company_id, voucher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset voucher link';

CREATE TABLE IF NOT EXISTS fa_asset_period_close (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    book_code VARCHAR(32) NOT NULL DEFAULT 'FINANCE',
    fiscal_year INT NOT NULL,
    fiscal_period INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CLOSED',
    closed_by VARCHAR(64) NULL,
    closed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_asset_period_close (company_id, fiscal_year, fiscal_period, book_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Fixed asset period close';

-- fixed asset seed data
INSERT INTO fa_asset_category (
    company_id, category_code, category_name, share_scope, depreciation_method,
    useful_life_months, residual_rate, depreciable, status, created_by, updated_by
)
SELECT c.company_id, 'HOUSE_BUILD', CONVERT(0xe688bfe5b18be58f8ae5bbbae7ad91e789a9 USING utf8mb4), 'COMPANY', 'STRAIGHT_LINE', 240, 0.0500, 1, 'ACTIVE', 'system', 'system' FROM sys_company c WHERE NOT EXISTS (SELECT 1 FROM fa_asset_category x WHERE x.company_id = c.company_id AND x.category_code = 'HOUSE_BUILD')
UNION ALL
SELECT c.company_id, 'MACHINE_EQUIP', CONVERT(0xe69cbae599a8e8aebee5a487 USING utf8mb4), 'COMPANY', 'STRAIGHT_LINE', 120, 0.0500, 1, 'ACTIVE', 'system', 'system' FROM sys_company c WHERE NOT EXISTS (SELECT 1 FROM fa_asset_category x WHERE x.company_id = c.company_id AND x.category_code = 'MACHINE_EQUIP')
UNION ALL
SELECT c.company_id, 'TRANS_EQUIP', CONVERT(0xe8bf90e8be93e8aebee5a487 USING utf8mb4), 'COMPANY', 'STRAIGHT_LINE', 72, 0.0500, 1, 'ACTIVE', 'system', 'system' FROM sys_company c WHERE NOT EXISTS (SELECT 1 FROM fa_asset_category x WHERE x.company_id = c.company_id AND x.category_code = 'TRANS_EQUIP')
UNION ALL
SELECT c.company_id, 'ELECTRONIC_EQUIP', CONVERT(0xe794b5e5ad90e8aebee5a487 USING utf8mb4), 'COMPANY', 'DOUBLE_DECLINING', 36, 0.0300, 1, 'ACTIVE', 'system', 'system' FROM sys_company c WHERE NOT EXISTS (SELECT 1 FROM fa_asset_category x WHERE x.company_id = c.company_id AND x.category_code = 'ELECTRONIC_EQUIP')
UNION ALL
SELECT c.company_id, 'OFFICE_EQUIP', CONVERT(0xe58a9ee585ace8aebee5a487 USING utf8mb4), 'COMPANY', 'STRAIGHT_LINE', 60, 0.0500, 1, 'ACTIVE', 'system', 'system' FROM sys_company c WHERE NOT EXISTS (SELECT 1 FROM fa_asset_category x WHERE x.company_id = c.company_id AND x.category_code = 'OFFICE_EQUIP');

INSERT INTO fa_asset_account_policy (
    company_id, category_id, book_code, asset_account, accum_depr_account,
    depr_expense_account, disposal_account, gain_account, loss_account, offset_account
)
SELECT c.company_id, cat.id, 'FINANCE', '1601', '1602', '660202', '1606', '6301', '6711', '1002' FROM sys_company c JOIN fa_asset_category cat ON cat.company_id = c.company_id AND cat.category_code = 'HOUSE_BUILD' WHERE NOT EXISTS (SELECT 1 FROM fa_asset_account_policy p WHERE p.company_id = c.company_id AND p.category_id = cat.id AND p.book_code = 'FINANCE')
UNION ALL
SELECT c.company_id, cat.id, 'FINANCE', '1601', '1602', '660202', '1606', '6301', '6711', '1002' FROM sys_company c JOIN fa_asset_category cat ON cat.company_id = c.company_id AND cat.category_code = 'MACHINE_EQUIP' WHERE NOT EXISTS (SELECT 1 FROM fa_asset_account_policy p WHERE p.company_id = c.company_id AND p.category_id = cat.id AND p.book_code = 'FINANCE')
UNION ALL
SELECT c.company_id, cat.id, 'FINANCE', '1601', '1602', '660202', '1606', '6301', '6711', '1002' FROM sys_company c JOIN fa_asset_category cat ON cat.company_id = c.company_id AND cat.category_code = 'TRANS_EQUIP' WHERE NOT EXISTS (SELECT 1 FROM fa_asset_account_policy p WHERE p.company_id = c.company_id AND p.category_id = cat.id AND p.book_code = 'FINANCE')
UNION ALL
SELECT c.company_id, cat.id, 'FINANCE', '1601', '1602', '660202', '1606', '6301', '6711', '1002' FROM sys_company c JOIN fa_asset_category cat ON cat.company_id = c.company_id AND cat.category_code = 'ELECTRONIC_EQUIP' WHERE NOT EXISTS (SELECT 1 FROM fa_asset_account_policy p WHERE p.company_id = c.company_id AND p.category_id = cat.id AND p.book_code = 'FINANCE')
UNION ALL
SELECT c.company_id, cat.id, 'FINANCE', '1601', '1602', '660202', '1606', '6301', '6711', '1002' FROM sys_company c JOIN fa_asset_category cat ON cat.company_id = c.company_id AND cat.category_code = 'OFFICE_EQUIP' WHERE NOT EXISTS (SELECT 1 FROM fa_asset_account_policy p WHERE p.company_id = c.company_id AND p.category_id = cat.id AND p.book_code = 'FINANCE');

-- comment standardization begin
ALTER TABLE fa_asset_account_policy
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN category_id bigint NOT NULL COMMENT '分类ID',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN asset_account varchar(64) NOT NULL COMMENT '资产科目编码',
    MODIFY COLUMN accum_depr_account varchar(64) NOT NULL COMMENT '累计折旧科目编码',
    MODIFY COLUMN depr_expense_account varchar(64) NOT NULL COMMENT '折旧费用科目编码',
    MODIFY COLUMN disposal_account varchar(64) NOT NULL COMMENT '处置清理科目编码',
    MODIFY COLUMN gain_account varchar(64) NOT NULL COMMENT '处置收益科目编码',
    MODIFY COLUMN loss_account varchar(64) NOT NULL COMMENT '处置损失科目编码',
    MODIFY COLUMN offset_account varchar(64) NOT NULL COMMENT '对冲科目编码',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产科目策略表';

ALTER TABLE fa_asset_card
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN asset_code varchar(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name varchar(64) NOT NULL COMMENT '资产名称',
    MODIFY COLUMN category_id bigint NOT NULL COMMENT '分类ID',
    MODIFY COLUMN category_code varchar(32) NOT NULL COMMENT '分类编码',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN use_company_id varchar(64) NOT NULL COMMENT '使用公司主体编码',
    MODIFY COLUMN use_dept_id bigint NULL COMMENT '使用部门ID',
    MODIFY COLUMN keeper_user_id bigint NULL COMMENT '保管人用户ID',
    MODIFY COLUMN manager_user_id bigint NULL COMMENT '管理人用户ID',
    MODIFY COLUMN source_type varchar(16) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
    MODIFY COLUMN acquire_date date NULL COMMENT '取得日期',
    MODIFY COLUMN in_service_date date NOT NULL COMMENT '投入使用日期',
    MODIFY COLUMN original_amount decimal(18,2) NOT NULL COMMENT '原值金额',
    MODIFY COLUMN accum_depr_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计折旧金额',
    MODIFY COLUMN salvage_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '残值金额',
    MODIFY COLUMN net_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '净值金额',
    MODIFY COLUMN useful_life_months int NOT NULL COMMENT '使用寿命月数',
    MODIFY COLUMN depreciated_months int NOT NULL DEFAULT 0 COMMENT '已折旧月数',
    MODIFY COLUMN remaining_months int NOT NULL DEFAULT 0 COMMENT '剩余月数',
    MODIFY COLUMN work_total decimal(18,6) NULL COMMENT '工作总量',
    MODIFY COLUMN work_used decimal(18,6) NULL COMMENT '已使用工作量',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '资产状态',
    MODIFY COLUMN can_depreciate tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许折旧:1是 0否',
    MODIFY COLUMN last_depr_year int NULL COMMENT '最近折旧年度',
    MODIFY COLUMN last_depr_period int NULL COMMENT '最近折旧期间',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN updated_by varchar(64) NULL COMMENT '更新人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产卡片表';

ALTER TABLE fa_asset_category
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN category_code varchar(32) NOT NULL COMMENT '分类编码',
    MODIFY COLUMN category_name varchar(64) NOT NULL COMMENT '分类名称',
    MODIFY COLUMN share_scope varchar(16) NOT NULL DEFAULT 'COMPANY' COMMENT '共享范围:GLOBAL全局/COMPANY公司',
    MODIFY COLUMN depreciation_method varchar(32) NOT NULL COMMENT '折旧方法',
    MODIFY COLUMN useful_life_months int NOT NULL COMMENT '使用寿命月数',
    MODIFY COLUMN residual_rate decimal(10,4) NOT NULL DEFAULT 0.0500 COMMENT '净残值率',
    MODIFY COLUMN depreciable tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否计提折旧:1是 0否',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN updated_by varchar(64) NULL COMMENT '更新人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产分类表';

ALTER TABLE fa_asset_change_bill
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN bill_no varchar(32) NOT NULL COMMENT '单据编号',
    MODIFY COLUMN bill_type varchar(32) NOT NULL COMMENT '单据类型',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN fiscal_year int NOT NULL COMMENT '会计年度',
    MODIFY COLUMN fiscal_period int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN bill_date date NOT NULL COMMENT '单据日期',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    MODIFY COLUMN total_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN posted_by varchar(64) NULL COMMENT '过账人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN posted_at datetime NULL COMMENT '过账时间',
    COMMENT = '固定资产变动单据表';

ALTER TABLE fa_asset_change_line
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN bill_id bigint NOT NULL COMMENT '单据ID',
    MODIFY COLUMN asset_id bigint NULL COMMENT '资产ID',
    MODIFY COLUMN asset_code varchar(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name varchar(64) NULL COMMENT '资产名称',
    MODIFY COLUMN change_type varchar(32) NOT NULL COMMENT '变动类型',
    MODIFY COLUMN category_id bigint NULL COMMENT '分类ID',
    MODIFY COLUMN category_code varchar(32) NULL COMMENT '分类编码',
    MODIFY COLUMN use_company_id varchar(64) NULL COMMENT '使用公司主体编码',
    MODIFY COLUMN use_dept_id bigint NULL COMMENT '使用部门ID',
    MODIFY COLUMN keeper_user_id bigint NULL COMMENT '保管人用户ID',
    MODIFY COLUMN in_service_date date NULL COMMENT '投入使用日期',
    MODIFY COLUMN change_amount decimal(18,2) NULL COMMENT '变动金额',
    MODIFY COLUMN old_value decimal(18,2) NULL COMMENT '变动前值',
    MODIFY COLUMN new_value decimal(18,2) NULL COMMENT '变动后值',
    MODIFY COLUMN old_salvage_amount decimal(18,2) NULL COMMENT '变动前残值金额',
    MODIFY COLUMN new_salvage_amount decimal(18,2) NULL COMMENT '变动后残值金额',
    MODIFY COLUMN old_useful_life_months int NULL COMMENT '变动前使用寿命月数',
    MODIFY COLUMN new_useful_life_months int NULL COMMENT '变动后使用寿命月数',
    MODIFY COLUMN old_remaining_months int NULL COMMENT '变动前剩余月数',
    MODIFY COLUMN new_remaining_months int NULL COMMENT '变动后剩余月数',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产变动明细表';

ALTER TABLE fa_asset_depr_line
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN run_id bigint NOT NULL COMMENT '批次ID',
    MODIFY COLUMN asset_id bigint NOT NULL COMMENT '资产ID',
    MODIFY COLUMN asset_code varchar(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name varchar(64) NOT NULL COMMENT '资产名称',
    MODIFY COLUMN category_id bigint NOT NULL COMMENT '分类ID',
    MODIFY COLUMN depreciation_method varchar(32) NOT NULL COMMENT '折旧方法',
    MODIFY COLUMN work_amount decimal(18,6) NULL COMMENT '本次工作量',
    MODIFY COLUMN depreciation_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '本次折旧金额',
    MODIFY COLUMN before_accum_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '折旧前累计折旧金额',
    MODIFY COLUMN after_accum_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '折旧后累计折旧金额',
    MODIFY COLUMN before_net_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '折旧前净值金额',
    MODIFY COLUMN after_net_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '折旧后净值金额',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产折旧计提明细表';

ALTER TABLE fa_asset_depr_run
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN run_no varchar(32) NOT NULL COMMENT '运行编号',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN fiscal_year int NOT NULL COMMENT '会计年度',
    MODIFY COLUMN fiscal_period int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态',
    MODIFY COLUMN asset_count int NOT NULL DEFAULT 0 COMMENT '资产数量',
    MODIFY COLUMN total_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN posted_by varchar(64) NULL COMMENT '过账人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN posted_at datetime NULL COMMENT '过账时间',
    COMMENT = '固定资产折旧计提批次表';

ALTER TABLE fa_asset_disposal_bill
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN bill_no varchar(32) NOT NULL COMMENT '单据编号',
    MODIFY COLUMN bill_type varchar(32) NOT NULL DEFAULT 'DISPOSAL' COMMENT '单据类型',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN fiscal_year int NOT NULL COMMENT '会计年度',
    MODIFY COLUMN fiscal_period int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN bill_date date NOT NULL COMMENT '单据日期',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    MODIFY COLUMN total_original_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '原值合计金额',
    MODIFY COLUMN total_accum_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计折旧合计金额',
    MODIFY COLUMN total_net_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '净值合计金额',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN posted_by varchar(64) NULL COMMENT '过账人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN posted_at datetime NULL COMMENT '过账时间',
    COMMENT = '固定资产处置单据表';

ALTER TABLE fa_asset_disposal_line
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN bill_id bigint NOT NULL COMMENT '单据ID',
    MODIFY COLUMN asset_id bigint NOT NULL COMMENT '资产ID',
    MODIFY COLUMN asset_code varchar(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name varchar(64) NOT NULL COMMENT '资产名称',
    MODIFY COLUMN category_id bigint NOT NULL COMMENT '分类ID',
    MODIFY COLUMN original_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '原值金额',
    MODIFY COLUMN accum_depr_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计折旧金额',
    MODIFY COLUMN net_amount decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '净值金额',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产处置明细表';

ALTER TABLE fa_asset_opening_import
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN batch_no varchar(32) NOT NULL COMMENT '批次号',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN fiscal_year int NOT NULL COMMENT '会计年度',
    MODIFY COLUMN fiscal_period int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '导入状态',
    MODIFY COLUMN total_rows int NOT NULL DEFAULT 0 COMMENT '总行数',
    MODIFY COLUMN success_rows int NOT NULL DEFAULT 0 COMMENT '成功行数',
    MODIFY COLUMN failed_rows int NOT NULL DEFAULT 0 COMMENT '失败行数',
    MODIFY COLUMN created_by varchar(64) NULL COMMENT '创建人',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产期初导入批次表';

ALTER TABLE fa_asset_opening_import_line
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN batch_id bigint NOT NULL COMMENT '批次ID',
    MODIFY COLUMN row_no int NOT NULL COMMENT '行号',
    MODIFY COLUMN asset_code varchar(32) NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name varchar(64) NULL COMMENT '资产名称',
    MODIFY COLUMN category_code varchar(32) NULL COMMENT '分类编码',
    MODIFY COLUMN result_status varchar(16) NOT NULL COMMENT '处理结果状态',
    MODIFY COLUMN error_message varchar(500) NULL COMMENT '错误信息',
    MODIFY COLUMN imported_asset_id bigint NULL COMMENT '导入资产ID',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产期初导入明细表';

ALTER TABLE fa_asset_period_close
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN book_code varchar(32) NOT NULL DEFAULT 'FINANCE' COMMENT '账簿编码',
    MODIFY COLUMN fiscal_year int NOT NULL COMMENT '会计年度',
    MODIFY COLUMN fiscal_period int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN status varchar(16) NOT NULL DEFAULT 'CLOSED' COMMENT '关账状态',
    MODIFY COLUMN closed_by varchar(64) NULL COMMENT '关账人',
    MODIFY COLUMN closed_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关账时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产期间关账表';

ALTER TABLE fa_asset_voucher_link
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN company_id varchar(64) NOT NULL COMMENT '公司主体编码',
    MODIFY COLUMN business_type varchar(32) NOT NULL COMMENT '业务类型',
    MODIFY COLUMN business_id bigint NOT NULL COMMENT '业务ID',
    MODIFY COLUMN voucher_no varchar(128) NOT NULL COMMENT '凭证号',
    MODIFY COLUMN iperiod int NOT NULL COMMENT '会计期间',
    MODIFY COLUMN csign varchar(32) NOT NULL COMMENT '凭证类别字',
    MODIFY COLUMN ino_id int NOT NULL COMMENT '凭证编号',
    MODIFY COLUMN remark varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '固定资产凭证关联表';

-- comment standardization end
