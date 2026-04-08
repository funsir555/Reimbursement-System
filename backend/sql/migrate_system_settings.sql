USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP FOREIGN KEY fk_sys_company_parent_company_id',
        'SELECT ''sys_company.fk_sys_company_parent_company_id not exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND CONSTRAINT_NAME = 'fk_sys_company_parent_company_id'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP INDEX idx_sys_company_parent_company_id',
        'SELECT ''sys_company.idx_sys_company_parent_company_id not exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_company'
      AND INDEX_NAME = 'idx_sys_company_parent_company_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN parent_company_id',
        'SELECT ''sys_company.parent_company_id not exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'parent_company_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN invoice_title VARCHAR(200) NULL COMMENT ''invoice title'' AFTER company_name',
        'SELECT ''sys_company.invoice_title exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'invoice_title'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_company ADD COLUMN tax_no VARCHAR(100) NULL COMMENT ''tax number'' AFTER invoice_title',
        'SELECT ''sys_company.tax_no exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'tax_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_name',
        'SELECT ''sys_company.bank_name not exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'bank_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_account_name',
        'SELECT ''sys_company.bank_account_name not exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'bank_account_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 1,
        'ALTER TABLE sys_company DROP COLUMN bank_account_no',
        'SELECT ''sys_company.bank_account_no not exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_company' AND COLUMN_NAME = 'bank_account_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_company_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'company bank account id',
    company_id VARCHAR(64) NOT NULL COMMENT 'company id',
    bank_name VARCHAR(200) NOT NULL COMMENT 'bank name',
    branch_name VARCHAR(200) NULL COMMENT 'branch name',
    bank_code VARCHAR(64) NULL COMMENT 'bank code',
    branch_code VARCHAR(64) NULL COMMENT 'branch code',
    cnaps_code VARCHAR(64) NULL COMMENT 'cnaps code',
    account_name VARCHAR(200) NOT NULL COMMENT 'account name',
    account_no VARCHAR(100) NOT NULL COMMENT 'account no',
    account_type VARCHAR(64) NULL COMMENT 'account type',
    account_usage VARCHAR(100) NULL COMMENT 'account usage',
    currency_code VARCHAR(32) NULL COMMENT 'currency code',
    default_account TINYINT NOT NULL DEFAULT 0 COMMENT 'default account',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status',
    remark VARCHAR(500) NULL COMMENT 'remark',
    direct_connect_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'direct connect enabled',
    direct_connect_provider VARCHAR(100) NULL COMMENT 'direct connect provider',
    direct_connect_channel VARCHAR(100) NULL COMMENT 'direct connect channel',
    direct_connect_protocol VARCHAR(100) NULL COMMENT 'direct connect protocol',
    direct_connect_customer_no VARCHAR(100) NULL COMMENT 'direct connect customer no',
    direct_connect_app_id VARCHAR(100) NULL COMMENT 'direct connect app id',
    direct_connect_account_alias VARCHAR(100) NULL COMMENT 'direct connect account alias',
    direct_connect_auth_mode VARCHAR(100) NULL COMMENT 'direct connect auth mode',
    direct_connect_api_base_url VARCHAR(500) NULL COMMENT 'direct connect api base url',
    direct_connect_cert_ref VARCHAR(200) NULL COMMENT 'direct connect cert ref',
    direct_connect_secret_ref VARCHAR(200) NULL COMMENT 'direct connect secret ref',
    direct_connect_sign_type VARCHAR(100) NULL COMMENT 'direct connect sign type',
    direct_connect_encrypt_type VARCHAR(100) NULL COMMENT 'direct connect encrypt type',
    direct_connect_last_sync_at DATETIME NULL COMMENT 'direct connect last sync at',
    direct_connect_last_sync_status VARCHAR(64) NULL COMMENT 'direct connect last sync status',
    direct_connect_last_error_msg VARCHAR(1000) NULL COMMENT 'direct connect last error message',
    direct_connect_ext_json JSON NULL COMMENT 'direct connect ext json',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    CONSTRAINT fk_sys_company_bank_account_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id),
    CONSTRAINT uk_sys_company_bank_account_company_no UNIQUE (company_id, account_no),
    KEY idx_sys_company_bank_account_company_status (company_id, status),
    KEY idx_sys_company_bank_account_company_default (company_id, default_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='company bank account';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN leader_user_id BIGINT NULL COMMENT ''department leader user id'' AFTER dept_code',
        'SELECT ''sys_department.leader_user_id exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'leader_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_sys_department_leader_user_id ON sys_department (leader_user_id)',
        'SELECT ''idx_sys_department_leader_user_id exists'''
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND INDEX_NAME = 'idx_sys_department_leader_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD CONSTRAINT fk_sys_department_leader_user_id FOREIGN KEY (leader_user_id) REFERENCES sys_user(id)',
        'SELECT ''fk_sys_department_leader_user_id exists'''
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND CONSTRAINT_NAME = 'fk_sys_department_leader_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN sync_managed TINYINT NOT NULL DEFAULT 0 COMMENT ''sync managed'' AFTER sync_enabled',
        'SELECT ''sys_department.sync_managed exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'sync_managed'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN sync_status VARCHAR(32) NULL COMMENT ''last sync status'' AFTER sync_managed',
        'SELECT ''sys_department.sync_status exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'sync_status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN sync_remark VARCHAR(500) NULL COMMENT ''sync remark'' AFTER sync_status',
        'SELECT ''sys_department.sync_remark exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'sync_remark'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''stat department belong'' AFTER sync_remark',
        'SELECT ''sys_department.stat_department_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'stat_department_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''stat region belong'' AFTER stat_department_belong',
        'SELECT ''sys_department.stat_region_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'stat_region_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_department ADD COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''stat area belong'' AFTER stat_region_belong',
        'SELECT ''sys_department.stat_area_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_department' AND COLUMN_NAME = 'stat_area_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN source_type VARCHAR(32) NULL COMMENT ''MANUAL DINGTALK WECOM FEISHU'' AFTER status',
        'SELECT ''sys_user.source_type exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'source_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN sync_managed TINYINT NOT NULL DEFAULT 0 COMMENT ''sync managed'' AFTER source_type',
        'SELECT ''sys_user.sync_managed exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'sync_managed'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN last_sync_at DATETIME NULL COMMENT ''last sync at'' AFTER feishu_user_id',
        'SELECT ''sys_user.last_sync_at exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'last_sync_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''stat department belong'' AFTER labor_relation_belong',
        'SELECT ''sys_user.stat_department_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'stat_department_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''stat region belong'' AFTER stat_department_belong',
        'SELECT ''sys_user.stat_region_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'stat_region_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_user ADD COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''stat area belong'' AFTER stat_region_belong',
        'SELECT ''sys_user.stat_area_belong exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'stat_area_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_description VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_role_code UNIQUE (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system roles';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(32) NOT NULL,
    parent_id BIGINT NULL,
    module_code VARCHAR(64) NULL,
    route_path VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_permission_permission_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system permissions';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_sys_role_permission_permission_id FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role permission mapping';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_sys_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user role mapping';

CREATE TABLE IF NOT EXISTS sys_sync_connector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_code VARCHAR(32) NOT NULL,
    platform_name VARCHAR(50) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    auto_sync_enabled TINYINT NOT NULL DEFAULT 0,
    sync_interval_minutes INT NOT NULL DEFAULT 60,
    config_json JSON NULL,
    last_sync_at DATETIME NULL,
    last_sync_status VARCHAR(32) NULL,
    last_sync_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_sync_connector_platform_code UNIQUE (platform_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sync connectors';

CREATE TABLE IF NOT EXISTS sys_sync_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_no VARCHAR(64) NOT NULL,
    platform_code VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    deleted_count INT NOT NULL DEFAULT 0,
    summary VARCHAR(500) NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_sync_job_job_no UNIQUE (job_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sync jobs';

CREATE TABLE IF NOT EXISTS sys_sync_job_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    detail_type VARCHAR(32) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    business_key VARCHAR(128) NULL,
    detail_status VARCHAR(32) NOT NULL,
    detail_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_sync_job_detail_job_id FOREIGN KEY (job_id) REFERENCES sys_sync_job(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='sync job details';

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, module_code, route_path, sort_order, status)
SELECT permission_code, permission_name, permission_type, parent_id, module_code, route_path, sort_order, status
FROM (
    SELECT 'settings:menu' AS permission_code, 'System Settings' AS permission_name, 'MENU' AS permission_type, NULL AS parent_id, 'settings' AS module_code, '/settings' AS route_path, 1 AS sort_order, 1 AS status
    UNION ALL SELECT 'dashboard:menu', 'Dashboard', 'MENU', NULL, 'dashboard', '/dashboard', 100, 1
    UNION ALL SELECT 'profile:menu', 'Profile', 'MENU', NULL, 'profile', '/profile', 200, 1
    UNION ALL SELECT 'expense:menu', 'Expense', 'MENU', NULL, 'expense', '/expense', 300, 1
    UNION ALL SELECT 'finance:menu', 'Finance', 'MENU', NULL, 'finance', '/finance', 400, 1
    UNION ALL SELECT 'archives:menu', 'Archives', 'MENU', NULL, 'archives', '/archives', 500, 1
    UNION ALL SELECT 'agents:menu', 'Agent', 'MENU', NULL, 'agents', '/archives/agents', 550, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission x WHERE x.permission_code = t.permission_code);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, module_code, route_path, sort_order, status)
SELECT permission_code, permission_name, permission_type, parent_id, module_code, route_path, sort_order, status
FROM (
    SELECT 'settings:organization:view' AS permission_code, 'Organization' AS permission_name, 'MENU' AS permission_type, p.id AS parent_id, 'organization' AS module_code, '/settings?tab=organization' AS route_path, 10 AS sort_order, 1 AS status FROM sys_permission p WHERE p.permission_code = 'settings:menu'
    UNION ALL SELECT 'settings:employees:view', 'Employees', 'MENU', p.id, 'employees', '/settings?tab=employees', 20, 1 FROM sys_permission p WHERE p.permission_code = 'settings:menu'
    UNION ALL SELECT 'settings:roles:view', 'Roles', 'MENU', p.id, 'roles', '/settings?tab=roles', 30, 1 FROM sys_permission p WHERE p.permission_code = 'settings:menu'
    UNION ALL SELECT 'settings:companies:view', 'Companies', 'MENU', p.id, 'companies', '/settings?tab=companies', 40, 1 FROM sys_permission p WHERE p.permission_code = 'settings:menu'
    UNION ALL SELECT 'settings:company_accounts:view', 'Company Accounts', 'MENU', p.id, 'companyAccounts', '/settings?tab=companyAccounts', 605, 1 FROM sys_permission p WHERE p.permission_code = 'settings:menu'
    UNION ALL SELECT 'dashboard:view', 'Dashboard Home', 'MENU', p.id, 'dashboard', '/dashboard', 101, 1 FROM sys_permission p WHERE p.permission_code = 'dashboard:menu'
    UNION ALL SELECT 'profile:view', 'Profile Center', 'MENU', p.id, 'profile', '/profile', 201, 1 FROM sys_permission p WHERE p.permission_code = 'profile:menu'
    UNION ALL SELECT 'expense:create:view', 'Expense Create', 'MENU', p.id, 'expense', '/expense/create', 301, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:list:view', 'Expense List', 'MENU', p.id, 'expense', '/expense/list', 302, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:approval:view', 'Expense Approval', 'MENU', p.id, 'expense', '/expense/approval', 303, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:payment:bank_link:view', 'Expense Bank Link', 'MENU', p.id, 'expense', '/expense/payment/bank-link', 304, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:payment:payment_order:view', 'Expense Payment Orders', 'MENU', p.id, 'expense', '/expense/payment/orders', 3042, 1 FROM sys_permission p WHERE p.permission_code = 'expense:payment:menu'
    UNION ALL SELECT 'expense:documents:view', 'Expense Documents', 'MENU', p.id, 'expense', '/expense/documents', 305, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:voucher_generation:view', 'Expense Voucher Generation', 'MENU', p.id, 'expense', '/expense/workbench/process-management', 306, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:process_management:view', 'Expense Process Management', 'MENU', p.id, 'expense', '/expense/workbench/process-management', 307, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'expense:budget_management:view', 'Expense Budget Management', 'MENU', p.id, 'expense', '/expense/workbench/budget-management', 308, 1 FROM sys_permission p WHERE p.permission_code = 'expense:menu'
    UNION ALL SELECT 'finance:general_ledger:new_voucher:view', 'Finance New Voucher', 'MENU', p.id, 'finance', '/finance/general-ledger/new-voucher', 401, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:general_ledger:query_voucher:view', 'Finance Query Voucher', 'MENU', p.id, 'finance', '/finance/general-ledger/query-voucher', 402, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:general_ledger:review_voucher:view', 'Finance Review Voucher', 'MENU', p.id, 'finance', '/finance/general-ledger/review-voucher', 403, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:general_ledger:balance_sheet:view', 'Finance Ledger Balance Sheet', 'MENU', p.id, 'finance', '/finance/general-ledger/balance-sheet', 404, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:fixed_assets:view', 'Finance Fixed Assets', 'MENU', p.id, 'finance', '/finance/fixed-assets', 405, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:reports:balance_sheet:view', 'Finance Balance Sheet Report', 'MENU', p.id, 'finance', '/finance/reports/balance-sheet', 406, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:reports:income_statement:view', 'Finance Income Statement', 'MENU', p.id, 'finance', '/finance/reports/income-statement', 407, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:reports:cash_flow:view', 'Finance Cash Flow', 'MENU', p.id, 'finance', '/finance/reports/cash-flow', 408, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:customers:view', 'Finance Customer Archive', 'MENU', p.id, 'finance', '/finance/archives/customers', 409, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:suppliers:view', 'Finance Supplier Archive', 'MENU', p.id, 'finance', '/finance/archives/suppliers', 410, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:employees:view', 'Finance Employee Archive', 'MENU', p.id, 'finance', '/finance/archives/employees', 411, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:departments:view', 'Finance Department Archive', 'MENU', p.id, 'finance', '/finance/archives/departments', 412, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:account_subjects:view', 'Finance Account Subjects', 'MENU', p.id, 'finance', '/finance/archives/account-subjects', 413, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:archives:account_subjects:create', 'Create Finance Account Subject', 'BUTTON', p.id, 'finance', NULL, 4131, 1 FROM sys_permission p WHERE p.permission_code = 'finance:archives:account_subjects:view'
    UNION ALL SELECT 'finance:archives:account_subjects:edit', 'Edit Finance Account Subject', 'BUTTON', p.id, 'finance', NULL, 4132, 1 FROM sys_permission p WHERE p.permission_code = 'finance:archives:account_subjects:view'
    UNION ALL SELECT 'finance:archives:account_subjects:disable', 'Toggle Finance Account Subject Status', 'BUTTON', p.id, 'finance', NULL, 4133, 1 FROM sys_permission p WHERE p.permission_code = 'finance:archives:account_subjects:view'
    UNION ALL SELECT 'finance:archives:account_subjects:close', 'Close Finance Account Subject', 'BUTTON', p.id, 'finance', NULL, 4134, 1 FROM sys_permission p WHERE p.permission_code = 'finance:archives:account_subjects:view'
    UNION ALL SELECT 'finance:archives:projects:view', 'Finance Project Archive', 'MENU', p.id, 'finance', '/finance/archives/projects', 414, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'finance:system_management:view', 'Finance System Management', 'MENU', p.id, 'finance', '/finance/system-management', 415, 1 FROM sys_permission p WHERE p.permission_code = 'finance:menu'
    UNION ALL SELECT 'archives:invoices:view', 'Invoice Archive', 'MENU', p.id, 'archives', '/archives/invoices', 501, 1 FROM sys_permission p WHERE p.permission_code = 'archives:menu'
    UNION ALL SELECT 'archives:account_books:view', 'Account Books', 'MENU', p.id, 'archives', '/archives/account-books', 502, 1 FROM sys_permission p WHERE p.permission_code = 'archives:menu'
    UNION ALL SELECT 'agents:view', 'Agent Workbench', 'MENU', p.id, 'agents', '/archives/agents', 551, 1 FROM sys_permission p WHERE p.permission_code = 'agents:menu'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission x WHERE x.permission_code = t.permission_code);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, module_code, route_path, sort_order, status)
SELECT permission_code, permission_name, 'BUTTON', parent_id, module_code, NULL, sort_order, 1
FROM (
    SELECT 'settings:organization:create' AS permission_code, 'Organization Create' AS permission_name, p.id AS parent_id, 'organization' AS module_code, 11 AS sort_order FROM sys_permission p WHERE p.permission_code = 'settings:organization:view'
    UNION ALL SELECT 'settings:organization:edit', 'Organization Edit', p.id, 'organization', 12 FROM sys_permission p WHERE p.permission_code = 'settings:organization:view'
    UNION ALL SELECT 'settings:organization:delete', 'Organization Delete', p.id, 'organization', 13 FROM sys_permission p WHERE p.permission_code = 'settings:organization:view'
    UNION ALL SELECT 'settings:organization:sync_config', 'Organization Sync Config', p.id, 'organization', 14 FROM sys_permission p WHERE p.permission_code = 'settings:organization:view'
    UNION ALL SELECT 'settings:organization:run_sync', 'Organization Run Sync', p.id, 'organization', 15 FROM sys_permission p WHERE p.permission_code = 'settings:organization:view'
    UNION ALL SELECT 'settings:employees:create', 'Employee Create', p.id, 'employees', 21 FROM sys_permission p WHERE p.permission_code = 'settings:employees:view'
    UNION ALL SELECT 'settings:employees:edit', 'Employee Edit', p.id, 'employees', 22 FROM sys_permission p WHERE p.permission_code = 'settings:employees:view'
    UNION ALL SELECT 'settings:employees:delete', 'Employee Delete', p.id, 'employees', 23 FROM sys_permission p WHERE p.permission_code = 'settings:employees:view'
    UNION ALL SELECT 'settings:roles:create', 'Role Create', p.id, 'roles', 31 FROM sys_permission p WHERE p.permission_code = 'settings:roles:view'
    UNION ALL SELECT 'settings:roles:edit', 'Role Edit', p.id, 'roles', 32 FROM sys_permission p WHERE p.permission_code = 'settings:roles:view'
    UNION ALL SELECT 'settings:roles:delete', 'Role Delete', p.id, 'roles', 33 FROM sys_permission p WHERE p.permission_code = 'settings:roles:view'
    UNION ALL SELECT 'settings:roles:assign_permissions', 'Role Assign Permissions', p.id, 'roles', 34 FROM sys_permission p WHERE p.permission_code = 'settings:roles:view'
    UNION ALL SELECT 'settings:roles:assign_users', 'Role Assign Users', p.id, 'roles', 35 FROM sys_permission p WHERE p.permission_code = 'settings:roles:view'
    UNION ALL SELECT 'settings:companies:create', 'Company Create', p.id, 'companies', 41 FROM sys_permission p WHERE p.permission_code = 'settings:companies:view'
    UNION ALL SELECT 'settings:companies:edit', 'Company Edit', p.id, 'companies', 42 FROM sys_permission p WHERE p.permission_code = 'settings:companies:view'
    UNION ALL SELECT 'settings:companies:delete', 'Company Delete', p.id, 'companies', 43 FROM sys_permission p WHERE p.permission_code = 'settings:companies:view'
    UNION ALL SELECT 'settings:company_accounts:create', 'Company Account Create', p.id, 'companyAccounts', 6051 FROM sys_permission p WHERE p.permission_code = 'settings:company_accounts:view'
    UNION ALL SELECT 'settings:company_accounts:edit', 'Company Account Edit', p.id, 'companyAccounts', 6052 FROM sys_permission p WHERE p.permission_code = 'settings:company_accounts:view'
    UNION ALL SELECT 'settings:company_accounts:delete', 'Company Account Delete', p.id, 'companyAccounts', 6053 FROM sys_permission p WHERE p.permission_code = 'settings:company_accounts:view'
    UNION ALL SELECT 'profile:password:update', 'Profile Update Password', p.id, 'profile', 211 FROM sys_permission p WHERE p.permission_code = 'profile:view'
    UNION ALL SELECT 'profile:downloads:view', 'Profile View Downloads', p.id, 'profile', 212 FROM sys_permission p WHERE p.permission_code = 'profile:view'
    UNION ALL SELECT 'expense:create:create', 'Expense Create Record', p.id, 'expense', 311 FROM sys_permission p WHERE p.permission_code = 'expense:create:view'
    UNION ALL SELECT 'expense:create:submit', 'Expense Submit', p.id, 'expense', 312 FROM sys_permission p WHERE p.permission_code = 'expense:create:view'
    UNION ALL SELECT 'expense:create:save_draft', 'Expense Save Draft', p.id, 'expense', 313 FROM sys_permission p WHERE p.permission_code = 'expense:create:view'
    UNION ALL SELECT 'expense:list:edit', 'Expense List Edit', p.id, 'expense', 321 FROM sys_permission p WHERE p.permission_code = 'expense:list:view'
    UNION ALL SELECT 'expense:list:delete', 'Expense List Delete', p.id, 'expense', 322 FROM sys_permission p WHERE p.permission_code = 'expense:list:view'
    UNION ALL SELECT 'expense:list:submit', 'Expense List Resubmit', p.id, 'expense', 323 FROM sys_permission p WHERE p.permission_code = 'expense:list:view'
    UNION ALL SELECT 'expense:approval:approve', 'Expense Approval Approve', p.id, 'expense', 331 FROM sys_permission p WHERE p.permission_code = 'expense:approval:view'
    UNION ALL SELECT 'expense:approval:reject', 'Expense Approval Reject', p.id, 'expense', 332 FROM sys_permission p WHERE p.permission_code = 'expense:approval:view'
    UNION ALL SELECT 'expense:payment:bank_link:pay', 'Expense Bank Link Pay', p.id, 'expense', 341 FROM sys_permission p WHERE p.permission_code = 'expense:payment:bank_link:view'
    UNION ALL SELECT 'expense:payment:payment_order:execute', 'Expense Payment Order Execute', p.id, 'expense', 342 FROM sys_permission p WHERE p.permission_code = 'expense:payment:payment_order:view'
    UNION ALL SELECT 'expense:voucher_generation:generate', 'Expense Voucher Generate', p.id, 'expense', 351 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:voucher_generation:mapping:view', 'Expense Voucher Mapping View', p.id, 'expense', 352 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:voucher_generation:mapping:edit', 'Expense Voucher Mapping Edit', p.id, 'expense', 353 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:voucher_generation:push:view', 'Expense Voucher Push View', p.id, 'expense', 354 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:voucher_generation:push:execute', 'Expense Voucher Push Execute', p.id, 'expense', 355 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:voucher_generation:query:view', 'Expense Voucher Query View', p.id, 'expense', 356 FROM sys_permission p WHERE p.permission_code = 'expense:voucher_generation:view'
    UNION ALL SELECT 'expense:process_management:create', 'Process Management Create', p.id, 'expense', 361 FROM sys_permission p WHERE p.permission_code = 'expense:process_management:view'
    UNION ALL SELECT 'expense:process_management:edit', 'Process Management Edit', p.id, 'expense', 362 FROM sys_permission p WHERE p.permission_code = 'expense:process_management:view'
    UNION ALL SELECT 'expense:process_management:publish', 'Process Management Publish', p.id, 'expense', 363 FROM sys_permission p WHERE p.permission_code = 'expense:process_management:view'
    UNION ALL SELECT 'expense:process_management:disable', 'Process Management Disable', p.id, 'expense', 364 FROM sys_permission p WHERE p.permission_code = 'expense:process_management:view'
    UNION ALL SELECT 'finance:general_ledger:new_voucher:create', 'Finance New Voucher Create', p.id, 'finance', 421 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:new_voucher:view'
    UNION ALL SELECT 'finance:general_ledger:query_voucher:export', 'Finance Query Voucher Export', p.id, 'finance', 431 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:query_voucher:view'
    UNION ALL SELECT 'finance:general_ledger:query_voucher:edit', 'Finance Query Voucher Edit', p.id, 'finance', 432 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:query_voucher:view'
    UNION ALL SELECT 'finance:general_ledger:review_voucher:review', 'Finance Review Voucher Review', p.id, 'finance', 441 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:review_voucher:view'
    UNION ALL SELECT 'finance:general_ledger:review_voucher:unreview', 'Finance Review Voucher Unreview', p.id, 'finance', 442 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:review_voucher:view'
    UNION ALL SELECT 'finance:general_ledger:balance_sheet:export', 'Finance Ledger Balance Sheet Export', p.id, 'finance', 451 FROM sys_permission p WHERE p.permission_code = 'finance:general_ledger:balance_sheet:view'
    UNION ALL SELECT 'finance:fixed_assets:create', 'Finance Fixed Assets Create', p.id, 'finance', 461 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:edit', 'Finance Fixed Assets Edit', p.id, 'finance', 462 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:delete', 'Finance Fixed Assets Delete', p.id, 'finance', 463 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:import', 'Finance Fixed Assets Import', p.id, 'finance', 464 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:change', 'Finance Fixed Assets Change', p.id, 'finance', 465 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:depreciate', 'Finance Fixed Assets Depreciate', p.id, 'finance', 466 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:dispose', 'Finance Fixed Assets Dispose', p.id, 'finance', 467 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:close_period', 'Finance Fixed Assets Close Period', p.id, 'finance', 468 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:fixed_assets:view_voucher_link', 'Finance Fixed Assets View Voucher Link', p.id, 'finance', 469 FROM sys_permission p WHERE p.permission_code = 'finance:fixed_assets:view'
    UNION ALL SELECT 'finance:reports:balance_sheet:export', 'Finance Balance Sheet Export', p.id, 'finance', 471 FROM sys_permission p WHERE p.permission_code = 'finance:reports:balance_sheet:view'
    UNION ALL SELECT 'finance:reports:income_statement:export', 'Finance Income Statement Export', p.id, 'finance', 481 FROM sys_permission p WHERE p.permission_code = 'finance:reports:income_statement:view'
    UNION ALL SELECT 'finance:reports:cash_flow:export', 'Finance Cash Flow Export', p.id, 'finance', 491 FROM sys_permission p WHERE p.permission_code = 'finance:reports:cash_flow:view'
    UNION ALL SELECT 'finance:archives:customers:create', 'Finance Customer Archive Create', p.id, 'finance', 5011 FROM sys_permission p WHERE p.permission_code = 'finance:archives:customers:view'
    UNION ALL SELECT 'finance:archives:customers:edit', 'Finance Customer Archive Edit', p.id, 'finance', 5012 FROM sys_permission p WHERE p.permission_code = 'finance:archives:customers:view'
    UNION ALL SELECT 'finance:archives:customers:delete', 'Finance Customer Archive Delete', p.id, 'finance', 5013 FROM sys_permission p WHERE p.permission_code = 'finance:archives:customers:view'
    UNION ALL SELECT 'finance:archives:customers:import', 'Finance Customer Archive Import', p.id, 'finance', 5014 FROM sys_permission p WHERE p.permission_code = 'finance:archives:customers:view'
    UNION ALL SELECT 'finance:archives:customers:export', 'Finance Customer Archive Export', p.id, 'finance', 5015 FROM sys_permission p WHERE p.permission_code = 'finance:archives:customers:view'
    UNION ALL SELECT 'finance:archives:suppliers:create', 'Finance Supplier Archive Create', p.id, 'finance', 5021 FROM sys_permission p WHERE p.permission_code = 'finance:archives:suppliers:view'
    UNION ALL SELECT 'finance:archives:suppliers:edit', 'Finance Supplier Archive Edit', p.id, 'finance', 5022 FROM sys_permission p WHERE p.permission_code = 'finance:archives:suppliers:view'
    UNION ALL SELECT 'finance:archives:suppliers:delete', 'Finance Supplier Archive Delete', p.id, 'finance', 5023 FROM sys_permission p WHERE p.permission_code = 'finance:archives:suppliers:view'
    UNION ALL SELECT 'finance:archives:suppliers:import', 'Finance Supplier Archive Import', p.id, 'finance', 5024 FROM sys_permission p WHERE p.permission_code = 'finance:archives:suppliers:view'
    UNION ALL SELECT 'finance:archives:suppliers:export', 'Finance Supplier Archive Export', p.id, 'finance', 5025 FROM sys_permission p WHERE p.permission_code = 'finance:archives:suppliers:view'
    UNION ALL SELECT 'finance:archives:employees:create', 'Finance Employee Archive Create', p.id, 'finance', 5031 FROM sys_permission p WHERE p.permission_code = 'finance:archives:employees:view'
    UNION ALL SELECT 'finance:archives:employees:edit', 'Finance Employee Archive Edit', p.id, 'finance', 5032 FROM sys_permission p WHERE p.permission_code = 'finance:archives:employees:view'
    UNION ALL SELECT 'finance:archives:employees:delete', 'Finance Employee Archive Delete', p.id, 'finance', 5033 FROM sys_permission p WHERE p.permission_code = 'finance:archives:employees:view'
    UNION ALL SELECT 'finance:archives:employees:import', 'Finance Employee Archive Import', p.id, 'finance', 5034 FROM sys_permission p WHERE p.permission_code = 'finance:archives:employees:view'
    UNION ALL SELECT 'finance:archives:employees:export', 'Finance Employee Archive Export', p.id, 'finance', 5035 FROM sys_permission p WHERE p.permission_code = 'finance:archives:employees:view'
    UNION ALL SELECT 'finance:archives:departments:create', 'Finance Department Archive Create', p.id, 'finance', 5041 FROM sys_permission p WHERE p.permission_code = 'finance:archives:departments:view'
    UNION ALL SELECT 'finance:archives:departments:edit', 'Finance Department Archive Edit', p.id, 'finance', 5042 FROM sys_permission p WHERE p.permission_code = 'finance:archives:departments:view'
    UNION ALL SELECT 'finance:archives:departments:delete', 'Finance Department Archive Delete', p.id, 'finance', 5043 FROM sys_permission p WHERE p.permission_code = 'finance:archives:departments:view'
    UNION ALL SELECT 'finance:archives:departments:import', 'Finance Department Archive Import', p.id, 'finance', 5044 FROM sys_permission p WHERE p.permission_code = 'finance:archives:departments:view'
    UNION ALL SELECT 'finance:archives:departments:export', 'Finance Department Archive Export', p.id, 'finance', 5045 FROM sys_permission p WHERE p.permission_code = 'finance:archives:departments:view'
    UNION ALL SELECT 'archives:invoices:upload', 'Invoice Upload', p.id, 'archives', 511 FROM sys_permission p WHERE p.permission_code = 'archives:invoices:view'
    UNION ALL SELECT 'archives:invoices:export', 'Invoice Export', p.id, 'archives', 512 FROM sys_permission p WHERE p.permission_code = 'archives:invoices:view'
    UNION ALL SELECT 'archives:invoices:verify', 'Invoice Verify', p.id, 'archives', 513 FROM sys_permission p WHERE p.permission_code = 'archives:invoices:view'
    UNION ALL SELECT 'archives:invoices:ocr', 'Invoice OCR', p.id, 'archives', 514 FROM sys_permission p WHERE p.permission_code = 'archives:invoices:view'
    UNION ALL SELECT 'archives:invoices:delete', 'Invoice Delete', p.id, 'archives', 515 FROM sys_permission p WHERE p.permission_code = 'archives:invoices:view'
    UNION ALL SELECT 'archives:account_books:create', 'Account Books Create', p.id, 'archives', 521 FROM sys_permission p WHERE p.permission_code = 'archives:account_books:view'
    UNION ALL SELECT 'archives:account_books:edit', 'Account Books Edit', p.id, 'archives', 522 FROM sys_permission p WHERE p.permission_code = 'archives:account_books:view'
    UNION ALL SELECT 'archives:account_books:delete', 'Account Books Delete', p.id, 'archives', 523 FROM sys_permission p WHERE p.permission_code = 'archives:account_books:view'
    UNION ALL SELECT 'agents:create', 'Agent Create', p.id, 'agents', 5511 FROM sys_permission p WHERE p.permission_code = 'agents:view'
    UNION ALL SELECT 'agents:edit', 'Agent Edit', p.id, 'agents', 5512 FROM sys_permission p WHERE p.permission_code = 'agents:view'
    UNION ALL SELECT 'agents:delete', 'Agent Delete', p.id, 'agents', 5513 FROM sys_permission p WHERE p.permission_code = 'agents:view'
    UNION ALL SELECT 'agents:run', 'Agent Run', p.id, 'agents', 5514 FROM sys_permission p WHERE p.permission_code = 'agents:view'
    UNION ALL SELECT 'agents:publish', 'Agent Publish', p.id, 'agents', 5515 FROM sys_permission p WHERE p.permission_code = 'agents:view'
    UNION ALL SELECT 'agents:view_logs', 'Agent Logs', p.id, 'agents', 5516 FROM sys_permission p WHERE p.permission_code = 'agents:view'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission x WHERE x.permission_code = t.permission_code);

INSERT INTO sys_role (role_code, role_name, role_description, status)
SELECT 'SUPER_ADMIN', 'SUPER_ADMIN', 'Has all active permissions', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.status = 1
WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'SUPER_ADMIN'
WHERE u.username = 'admin';

INSERT INTO sys_sync_connector (platform_code, platform_name, enabled, auto_sync_enabled, sync_interval_minutes, config_json, last_sync_status, last_sync_message)
SELECT 'DINGTALK', 'DingTalk', 1, 0, 60, JSON_OBJECT(), 'IDLE', 'Not synced yet'
WHERE NOT EXISTS (SELECT 1 FROM sys_sync_connector WHERE platform_code = 'DINGTALK');

INSERT INTO sys_sync_connector (platform_code, platform_name, enabled, auto_sync_enabled, sync_interval_minutes, config_json, last_sync_status, last_sync_message)
SELECT 'WECOM', 'WeCom', 1, 0, 60, JSON_OBJECT(), 'IDLE', 'Not synced yet'
WHERE NOT EXISTS (SELECT 1 FROM sys_sync_connector WHERE platform_code = 'WECOM');

INSERT INTO sys_sync_connector (platform_code, platform_name, enabled, auto_sync_enabled, sync_interval_minutes, config_json, last_sync_status, last_sync_message)
SELECT 'FEISHU', 'Feishu', 1, 0, 60, JSON_OBJECT(), 'IDLE', 'Not synced yet'
WHERE NOT EXISTS (SELECT 1 FROM sys_sync_connector WHERE platform_code = 'FEISHU');

/*
权限目录最终规范化:
1. 统一改成中文名称
2. 按前端当前菜单层级重排
3. 兼容流程管理重构后的“管理工作台”结构
4. 确保 SUPER_ADMIN / admin 权限关系正确
*/

DROP TEMPORARY TABLE IF EXISTS tmp_permission_seed;

CREATE TEMPORARY TABLE tmp_permission_seed (
    permission_code VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
    permission_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    permission_type VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    parent_code VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    module_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    route_path VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    sort_order INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_permission_seed (
    permission_code,
    permission_name,
    permission_type,
    parent_code,
    module_code,
    route_path,
    sort_order,
    status
) VALUES
    ('dashboard:menu', '首页', 'MENU', NULL, 'dashboard', '/dashboard', 10, 1),
    ('dashboard:view', '首页', 'MENU', 'dashboard:menu', 'dashboard', '/dashboard', 101, 1),

    ('profile:menu', '个人中心', 'MENU', NULL, 'profile', '/profile', 20, 1),
    ('profile:view', '个人中心', 'MENU', 'profile:menu', 'profile', '/profile', 201, 1),
    ('profile:password:update', '修改密码', 'BUTTON', 'profile:view', 'profile', NULL, 2011, 1),
    ('profile:downloads:view', '下载中心', 'BUTTON', 'profile:view', 'profile', NULL, 2012, 1),

    ('expense:menu', '报销管理', 'MENU', NULL, 'expense', '/expense', 30, 1),
    ('expense:create:view', '新建报销', 'MENU', 'expense:menu', 'expense', '/expense/create', 301, 1),
    ('expense:create:create', '创建报销单', 'BUTTON', 'expense:create:view', 'expense', NULL, 3011, 1),
    ('expense:create:submit', '提交报销单', 'BUTTON', 'expense:create:view', 'expense', NULL, 3012, 1),
    ('expense:create:save_draft', '保存草稿', 'BUTTON', 'expense:create:view', 'expense', NULL, 3013, 1),
    ('expense:list:view', '我的报销', 'MENU', 'expense:menu', 'expense', '/expense/list', 302, 1),
    ('expense:list:edit', '编辑报销单', 'BUTTON', 'expense:list:view', 'expense', NULL, 3021, 1),
    ('expense:list:delete', '删除报销单', 'BUTTON', 'expense:list:view', 'expense', NULL, 3022, 1),
    ('expense:list:submit', '重新提交', 'BUTTON', 'expense:list:view', 'expense', NULL, 3023, 1),
    ('expense:approval:view', '待我审批', 'MENU', 'expense:menu', 'expense', '/expense/approval', 303, 1),
    ('expense:approval:approve', '审批通过', 'BUTTON', 'expense:approval:view', 'expense', NULL, 3031, 1),
    ('expense:approval:reject', '审批驳回', 'BUTTON', 'expense:approval:view', 'expense', NULL, 3032, 1),
    ('expense:payment:menu', '支付', 'MENU', 'expense:menu', 'expense-payment', '/expense/payment', 304, 1),
    ('expense:payment:bank_link:view', '银企直连', 'MENU', 'expense:payment:menu', 'expense', '/expense/payment/bank-link', 3041, 1),
    ('expense:payment:bank_link:pay', '发起支付', 'BUTTON', 'expense:payment:bank_link:view', 'expense', NULL, 30411, 1),
    ('expense:payment:payment_order:view', '???', 'MENU', 'expense:payment:menu', 'expense', '/expense/payment/orders', 3042, 1),
    ('expense:payment:payment_order:execute', '????', 'BUTTON', 'expense:payment:payment_order:view', 'expense', NULL, 30421, 1),
    ('expense:documents:view', '单据查询', 'MENU', 'expense:menu', 'expense', '/expense/documents', 305, 1),
    ('expense:voucher_generation:view', CONVERT(0xe587ade8af81e7949fe68890 USING utf8mb4), 'MENU', 'expense:menu', 'expense', '/expense/workbench/process-management', 306, 1),
    ('expense:voucher_generation:generate', '生成凭证', 'BUTTON', 'expense:voucher_generation:view', 'expense', NULL, 3061, 1),
    ('expense:workbench:menu', '管理工作台', 'MENU', 'expense:menu', 'expense-workbench', '/expense/workbench', 307, 1),
    ('expense:process_management:view', '流程管理', 'MENU', 'expense:workbench:menu', 'expense', '/expense/workbench/process-management', 3071, 1),
    ('expense:process_management:create', '新增流程配置', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30711, 1),
    ('expense:process_management:edit', '编辑流程配置', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30712, 1),
    ('expense:process_management:publish', '发布流程配置', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30713, 1),
    ('expense:process_management:disable', '停用流程配置', 'BUTTON', 'expense:process_management:view', 'expense', NULL, 30714, 1),
    ('expense:budget_management:view', '预算管理', 'MENU', 'expense:workbench:menu', 'expense', '/expense/workbench/budget-management', 3072, 1),

    ('finance:menu', '财务管理', 'MENU', NULL, 'finance', '/finance', 40, 1),
    ('finance:general_ledger:menu', '总账', 'MENU', 'finance:menu', 'finance-general-ledger', '/finance/general-ledger', 401, 1),
    ('finance:general_ledger:new_voucher:view', '新建凭证', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/new-voucher', 4011, 1),
    ('finance:general_ledger:new_voucher:create', '新增凭证', 'BUTTON', 'finance:general_ledger:new_voucher:view', 'finance', NULL, 40111, 1),
    ('finance:general_ledger:query_voucher:view', '查询凭证', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/query-voucher', 4012, 1),
    ('finance:general_ledger:query_voucher:export', '导出凭证', 'BUTTON', 'finance:general_ledger:query_voucher:view', 'finance', NULL, 40121, 1),
    ('finance:general_ledger:query_voucher:edit', '修改凭证', 'BUTTON', 'finance:general_ledger:query_voucher:view', 'finance', NULL, 40122, 1),
    ('finance:general_ledger:review_voucher:view', '审核凭证', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/review-voucher', 4013, 1),
    ('finance:general_ledger:review_voucher:review', '审核通过', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40131, 1),
    ('finance:general_ledger:review_voucher:unreview', '取消审核', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40132, 1),
    ('finance:general_ledger:balance_sheet:view', '总账余额表', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/balance-sheet', 4014, 1),
    ('finance:general_ledger:balance_sheet:export', '导出总账余额表', 'BUTTON', 'finance:general_ledger:balance_sheet:view', 'finance', NULL, 40141, 1),
    ('finance:general_ledger:detail_ledger:view', '明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/detail-ledger', 4015, 1),
    ('finance:general_ledger:general_ledger:view', '总分类账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/general-ledger', 4016, 1),
    ('finance:general_ledger:project_detail_ledger:view', '项目明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/project-detail-ledger', 4017, 1),
    ('finance:general_ledger:supplier_detail_ledger:view', '供应商明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/supplier-detail-ledger', 4018, 1),
    ('finance:general_ledger:customer_detail_ledger:view', '客户明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/customer-detail-ledger', 4019, 1),
    ('finance:general_ledger:personal_detail_ledger:view', '个人明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/personal-detail-ledger', 4020, 1),
    ('finance:general_ledger:quantity_amount_detail_ledger:view', '数量金额明细账', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/quantity-amount-detail-ledger', 4021, 1),
    ('finance:fixed_assets:view', '固定资产', 'MENU', 'finance:menu', 'finance', '/finance/fixed-assets', 402, 1),
    ('finance:fixed_assets:create', '新增固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4021, 1),
    ('finance:fixed_assets:edit', '编辑固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4022, 1),
    ('finance:fixed_assets:delete', '删除固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4023, 1),
    ('finance:fixed_assets:import', '固定资产期初导入', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4024, 1),
    ('finance:fixed_assets:change', '固定资产变动', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4025, 1),
    ('finance:fixed_assets:depreciate', '固定资产折旧', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4026, 1),
    ('finance:fixed_assets:dispose', '固定资产处置', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4027, 1),
    ('finance:fixed_assets:close_period', '固定资产期间结账', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4028, 1),
    ('finance:fixed_assets:view_voucher_link', '固定资产凭证联查', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4029, 1),
    ('finance:reports:menu', '财务报表', 'MENU', 'finance:menu', 'finance-reports', '/finance/reports', 403, 1),
    ('finance:reports:balance_sheet:view', '资产负债表', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/balance-sheet', 4031, 1),
    ('finance:reports:balance_sheet:export', '导出资产负债表', 'BUTTON', 'finance:reports:balance_sheet:view', 'finance', NULL, 40311, 1),
    ('finance:reports:income_statement:view', '利润表', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/income-statement', 4032, 1),
    ('finance:reports:income_statement:export', '导出利润表', 'BUTTON', 'finance:reports:income_statement:view', 'finance', NULL, 40321, 1),
    ('finance:reports:cash_flow:view', '现金流量表', 'MENU', 'finance:reports:menu', 'finance', '/finance/reports/cash-flow', 4033, 1),
    ('finance:reports:cash_flow:export', '导出现金流量表', 'BUTTON', 'finance:reports:cash_flow:view', 'finance', NULL, 40331, 1),
    ('finance:archives:menu', '会计档案', 'MENU', 'finance:menu', 'finance-archives', '/finance/archives', 404, 1),
    ('finance:archives:customers:view', '客户档案', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/customers', 4041, 1),
    ('finance:archives:customers:create', '新增客户档案', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40411, 1),
    ('finance:archives:customers:edit', '编辑客户档案', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40412, 1),
    ('finance:archives:customers:delete', '删除客户档案', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40413, 1),
    ('finance:archives:customers:import', '导入客户档案', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40414, 1),
    ('finance:archives:customers:export', '导出客户档案', 'BUTTON', 'finance:archives:customers:view', 'finance', NULL, 40415, 1),
    ('finance:archives:suppliers:view', '供应商档案', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/suppliers', 4042, 1),
    ('finance:archives:suppliers:create', '新增供应商档案', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40421, 1),
    ('finance:archives:suppliers:edit', '编辑供应商档案', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40422, 1),
    ('finance:archives:suppliers:delete', '删除供应商档案', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40423, 1),
    ('finance:archives:suppliers:import', '导入供应商档案', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40424, 1),
    ('finance:archives:suppliers:export', '导出供应商档案', 'BUTTON', 'finance:archives:suppliers:view', 'finance', NULL, 40425, 1),
    ('finance:archives:employees:view', '员工档案', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/employees', 4043, 1),
    ('finance:archives:employees:create', '新增员工档案', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40431, 1),
    ('finance:archives:employees:edit', '编辑员工档案', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40432, 1),
    ('finance:archives:employees:delete', '删除员工档案', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40433, 1),
    ('finance:archives:employees:import', '导入员工档案', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40434, 1),
    ('finance:archives:employees:export', '导出员工档案', 'BUTTON', 'finance:archives:employees:view', 'finance', NULL, 40435, 1),
    ('finance:archives:departments:view', '部门档案', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/departments', 4044, 1),
    ('finance:archives:departments:create', '新增部门档案', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40441, 1),
    ('finance:archives:departments:edit', '编辑部门档案', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40442, 1),
    ('finance:archives:departments:delete', '删除部门档案', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40443, 1),
    ('finance:archives:departments:import', '导入部门档案', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40444, 1),
    ('finance:archives:departments:export', '导出部门档案', 'BUTTON', 'finance:archives:departments:view', 'finance', NULL, 40445, 1),
    ('finance:archives:account_subjects:view', '会计科目', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/account-subjects', 4045, 1),
    ('finance:archives:account_subjects:create', '新增会计科目', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40451, 1),
    ('finance:archives:account_subjects:edit', '编辑会计科目', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40452, 1),
    ('finance:archives:account_subjects:disable', '启停会计科目', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40453, 1),
    ('finance:archives:account_subjects:close', '封存会计科目', 'BUTTON', 'finance:archives:account_subjects:view', 'finance', NULL, 40454, 1),
    ('finance:archives:projects:view', '项目档案', 'MENU', 'finance:archives:menu', 'finance', '/finance/archives/projects', 4046, 1),
    ('finance:system_management:view', '财务系统管理', 'MENU', 'finance:menu', 'finance', '/finance/system-management', 405, 1),

    ('archives:menu', '电子档案', 'MENU', NULL, 'archives', '/archives', 50, 1),
    ('archives:invoices:view', '发票管理', 'MENU', 'archives:menu', 'archives', '/archives/invoices', 501, 1),
    ('archives:invoices:upload', '上传发票', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5011, 1),
    ('archives:invoices:export', '导出发票', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5012, 1),
    ('archives:invoices:verify', '发票验真', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5013, 1),
    ('archives:invoices:ocr', '发票识别', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5014, 1),
    ('archives:invoices:delete', '删除发票', 'BUTTON', 'archives:invoices:view', 'archives', NULL, 5015, 1),
    ('archives:account_books:view', '账套管理', 'MENU', 'archives:menu', 'archives', '/archives/account-books', 502, 1),
    ('archives:account_books:create', '新增账套', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5021, 1),
    ('archives:account_books:edit', '编辑账套', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5022, 1),
    ('archives:account_books:delete', '删除账套', 'BUTTON', 'archives:account_books:view', 'archives', NULL, 5023, 1),

    ('agents:menu', 'Agent', 'MENU', NULL, 'agents', '/archives/agents', 55, 1),
    ('agents:view', CONVERT(0x4167656e74e5b7a5e4bd9ce58fb0 USING utf8mb4), 'MENU', 'agents:menu', 'agents', '/archives/agents', 551, 1),
    ('agents:create', CONVERT(0xe696b0e5bbba204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5511, 1),
    ('agents:edit', CONVERT(0xe7bc96e8be91204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5512, 1),
    ('agents:delete', CONVERT(0xe588a0e999a4204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5513, 1),
    ('agents:run', CONVERT(0xe8bf90e8a18c204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5514, 1),
    ('agents:publish', CONVERT(0xe58f91e5b883204167656e74 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5515, 1),
    ('agents:view_logs', CONVERT(0xe69fa5e79c8be8bf90e8a18ce697a5e5bf97 USING utf8mb4), 'BUTTON', 'agents:view', 'agents', NULL, 5516, 1),

    ('settings:menu', '系统设置', 'MENU', NULL, 'settings', '/settings', 60, 1),
    ('settings:organization:view', '组织架构', 'MENU', 'settings:menu', 'organization', '/settings?tab=organization', 601, 1),
    ('settings:organization:create', '新增部门', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6011, 1),
    ('settings:organization:edit', '编辑部门', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6012, 1),
    ('settings:organization:delete', '删除部门', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6013, 1),
    ('settings:organization:sync_config', '配置同步', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6014, 1),
    ('settings:organization:run_sync', '手动同步', 'BUTTON', 'settings:organization:view', 'organization', NULL, 6015, 1),
    ('settings:employees:view', '员工管理', 'MENU', 'settings:menu', 'employees', '/settings?tab=employees', 602, 1),
    ('settings:employees:create', '新增员工', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6021, 1),
    ('settings:employees:edit', '编辑员工', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6022, 1),
    ('settings:employees:delete', '删除员工', 'BUTTON', 'settings:employees:view', 'employees', NULL, 6023, 1),
    ('settings:roles:view', '权限管理', 'MENU', 'settings:menu', 'roles', '/settings?tab=roles', 603, 1),
    ('settings:roles:create', '新增角色', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6031, 1),
    ('settings:roles:edit', '编辑角色', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6032, 1),
    ('settings:roles:delete', '删除角色', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6033, 1),
    ('settings:roles:assign_permissions', '分配权限', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6034, 1),
    ('settings:roles:assign_users', '分配用户', 'BUTTON', 'settings:roles:view', 'roles', NULL, 6035, 1),
    ('settings:companies:view', '公司管理', 'MENU', 'settings:menu', 'companies', '/settings?tab=companies', 604, 1),
    ('settings:companies:create', '新增公司', 'BUTTON', 'settings:companies:view', 'companies', NULL, 6041, 1),
    ('settings:companies:edit', '编辑公司', 'BUTTON', 'settings:companies:view', 'companies', NULL, 6042, 1),
    ('settings:companies:delete', '删除公司', 'BUTTON', 'settings:companies:view', 'companies', NULL, 6043, 1);

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
    NULL,
    seed.module_code,
    seed.route_path,
    seed.sort_order,
    seed.status
FROM tmp_permission_seed seed
LEFT JOIN sys_permission permission
    ON permission.permission_code COLLATE utf8mb4_unicode_ci = seed.permission_code COLLATE utf8mb4_unicode_ci
WHERE permission.id IS NULL;

UPDATE sys_permission permission
JOIN tmp_permission_seed seed
    ON seed.permission_code COLLATE utf8mb4_unicode_ci = permission.permission_code COLLATE utf8mb4_unicode_ci
SET permission.permission_name = seed.permission_name,
    permission.permission_type = seed.permission_type,
    permission.module_code = seed.module_code,
    permission.route_path = seed.route_path,
    permission.sort_order = seed.sort_order,
    permission.status = seed.status;

UPDATE sys_permission child
JOIN tmp_permission_seed seed
    ON seed.permission_code COLLATE utf8mb4_unicode_ci = child.permission_code COLLATE utf8mb4_unicode_ci
LEFT JOIN sys_permission parent
    ON parent.permission_code COLLATE utf8mb4_unicode_ci = seed.parent_code COLLATE utf8mb4_unicode_ci
SET child.parent_id = parent.id;

UPDATE sys_role
SET role_name = '超级管理员',
    role_description = '拥有系统全部启用权限',
    status = 1
WHERE role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.status = 1
WHERE role.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.id, role.id
FROM sys_user user
JOIN sys_role role ON role.role_code = 'SUPER_ADMIN'
WHERE user.username = 'admin';

DROP TEMPORARY TABLE IF EXISTS tmp_permission_seed;
