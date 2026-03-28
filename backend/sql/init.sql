CREATE DATABASE IF NOT EXISTS finex_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_company (
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    company_code VARCHAR(64) NOT NULL COMMENT '公司主体编号',
    company_name VARCHAR(128) NOT NULL COMMENT '公司主体名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (company_id),
    CONSTRAINT uk_sys_company_company_code UNIQUE (company_code),
    KEY idx_sys_company_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司主体主数据表';

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    company_id VARCHAR(64) NULL COMMENT '公司主体编码',
    dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
    leader_user_id BIGINT NULL COMMENT '部门负责人用户ID',
    dept_name VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NULL COMMENT '上级部门ID',
    wecom_department_id VARCHAR(100) NULL COMMENT '企微部门ID',
    dingtalk_department_id VARCHAR(100) NULL COMMENT '钉钉部门ID',
    feishu_department_id VARCHAR(100) NULL COMMENT '飞书部门ID',
    sync_source VARCHAR(32) NULL COMMENT '同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED',
    sync_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用同步',
    last_sync_at DATETIME NULL COMMENT '最近同步时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_sys_department_dept_code UNIQUE (dept_code),
    CONSTRAINT uk_sys_department_wecom_department_id UNIQUE (wecom_department_id),
    CONSTRAINT uk_sys_department_dingtalk_department_id UNIQUE (dingtalk_department_id),
    CONSTRAINT uk_sys_department_feishu_department_id UNIQUE (feishu_department_id),
    KEY idx_sys_department_company_id (company_id),
    KEY idx_sys_department_leader_user_id (leader_user_id),
    KEY idx_sys_department_parent_id (parent_id),
    KEY idx_sys_department_status_sort (status, sort_order),
    KEY idx_sys_department_company_status_sort (company_id, status, sort_order),
    CONSTRAINT fk_sys_department_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id),
    CONSTRAINT fk_sys_department_parent_id FOREIGN KEY (parent_id) REFERENCES sys_department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局部门树主数据表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(64) NOT NULL COMMENT '密码(MD5)',
    name VARCHAR(50) COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    company_id VARCHAR(64) NULL COMMENT '公司主体编码',
    dept_id BIGINT COMMENT '部门ID',
    position VARCHAR(50) COMMENT '岗位',
    labor_relation_belong VARCHAR(100) COMMENT '劳动关系归属',
    status TINYINT DEFAULT 1 COMMENT '状态:1正常 0停用',
    wecom_user_id VARCHAR(100) COMMENT '企微用户ID',
    dingtalk_user_id VARCHAR(100) COMMENT '钉钉用户ID',
    feishu_user_id VARCHAR(100) COMMENT '飞书用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_company_id (company_id),
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_dept (dept_id),
    INDEX idx_feishu_user_id (feishu_user_id),
    CONSTRAINT fk_sys_user_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id),
    CONSTRAINT fk_sys_user_dept_id FOREIGN KEY (dept_id) REFERENCES sys_department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS labor_relation_belong VARCHAR(100) COMMENT '劳动关系归属' AFTER position;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS feishu_user_id VARCHAR(100) COMMENT '飞书用户ID' AFTER dingtalk_user_id;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND CONSTRAINT_NAME = 'fk_sys_department_leader_user_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_department ADD CONSTRAINT fk_sys_department_leader_user_id FOREIGN KEY (leader_user_id) REFERENCES sys_user(id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_user_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '閾惰璐︽埛ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    bank_name VARCHAR(100) NOT NULL COMMENT '閾惰鍚嶇О',
    branch_name VARCHAR(100) COMMENT '鏀鍚嶇О',
    account_name VARCHAR(100) NOT NULL COMMENT '璐︽埛鍚?,
    account_no VARCHAR(50) NOT NULL COMMENT '閾惰鍗″彿',
    account_type VARCHAR(50) DEFAULT '瀵圭璐︽埛' COMMENT '璐︽埛绫诲瀷',
    default_account TINYINT DEFAULT 0 COMMENT '鏄惁榛樿璐︽埛',
    status TINYINT DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0鍋滅敤',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛鏀舵璐︽埛琛?;

CREATE TABLE IF NOT EXISTS sys_download_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓嬭浇璁板綍ID',
    user_id BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    file_name VARCHAR(200) NOT NULL COMMENT '鏂囦欢鍚?,
    business_type VARCHAR(100) NOT NULL COMMENT '涓氬姟绫诲瀷',
    status VARCHAR(20) NOT NULL COMMENT '鐘舵€?DOWNLOADING/COMPLETED/FAILED',
    progress INT DEFAULT 0 COMMENT '涓嬭浇杩涘害',
    file_size VARCHAR(30) COMMENT '鏂囦欢澶у皬',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    finished_at DATETIME NULL COMMENT '瀹屾垚鏃堕棿',
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='涓嬭浇璁板綍琛?;

CREATE TABLE IF NOT EXISTS pm_template_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鍒嗙被ID',
    category_code VARCHAR(64) NOT NULL COMMENT '鍒嗙被缂栫爜',
    category_name VARCHAR(64) NOT NULL COMMENT '鍒嗙被鍚嶇О',
    category_description VARCHAR(255) COMMENT '鍒嗙被璇存槑',
    sort_order INT DEFAULT 0 COMMENT '鎺掑簭鍙?,
    status TINYINT DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0鍋滅敤',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='娴佺▼妯℃澘鍒嗙被琛?;

CREATE TABLE IF NOT EXISTS pm_custom_archive_design (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鑷畾涔夋。妗圛D',
    archive_code VARCHAR(64) NOT NULL COMMENT '妗ｆ缂栫爜',
    archive_name VARCHAR(100) NOT NULL COMMENT '妗ｆ鍚嶇О',
    archive_type VARCHAR(32) NOT NULL COMMENT '妗ｆ绫诲瀷:SELECT/AUTO_RULE',
    archive_description VARCHAR(255) COMMENT '妗ｆ璇存槑',
    status TINYINT DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0鍋滅敤',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_archive_code (archive_code),
    KEY idx_archive_type (archive_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='娴佺▼绠＄悊鑷畾涔夋。妗堣璁¤〃';

ALTER TABLE pm_custom_archive_design
    ADD COLUMN IF NOT EXISTS archive_description VARCHAR(255) COMMENT '妗ｆ璇存槑' AFTER archive_type;

ALTER TABLE pm_custom_archive_design
    MODIFY COLUMN archive_type VARCHAR(32) NOT NULL COMMENT '妗ｆ绫诲瀷:SELECT/AUTO_RULE';

ALTER TABLE pm_custom_archive_design
    DROP COLUMN IF EXISTS sort_order;

CREATE TABLE IF NOT EXISTS pm_custom_archive_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '妗ｆ缁撴灉椤笽D',
    archive_id BIGINT NOT NULL COMMENT '妗ｆID',
    item_code VARCHAR(64) NOT NULL COMMENT '缁撴灉椤圭紪鐮?,
    item_name VARCHAR(100) NOT NULL COMMENT '缁撴灉椤瑰悕绉?,
    priority INT DEFAULT 1 COMMENT '浼樺厛绾?,
    status TINYINT DEFAULT 1 COMMENT '鐘舵€?1鍚敤 0鍋滅敤',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_archive_item_code (archive_id, item_code),
    KEY idx_archive_item_status (archive_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鑷畾涔夋。妗堢粨鏋滈」琛?;

ALTER TABLE pm_custom_archive_item
    DROP COLUMN IF EXISTS item_value,
    DROP COLUMN IF EXISTS sort_order;

CREATE TABLE IF NOT EXISTS pm_custom_archive_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鑷姩鍒掑垎瑙勫垯ID',
    archive_item_id BIGINT NOT NULL COMMENT '缁撴灉椤笽D',
    group_no INT DEFAULT 1 COMMENT '瑙勫垯缁勫彿',
    field_key VARCHAR(64) NOT NULL COMMENT '瀛楁Key',
    operator VARCHAR(32) NOT NULL COMMENT '鎿嶄綔绗?,
    compare_value VARCHAR(500) COMMENT '姣旇緝鍊?JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    KEY idx_archive_item_group (archive_item_id, group_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鑷畾涔夋。妗堣嚜鍔ㄥ垝鍒嗚鍒欒〃';

ALTER TABLE pm_custom_archive_rule
    DROP COLUMN IF EXISTS sort_order;

CREATE TABLE IF NOT EXISTS pm_document_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '妯℃澘ID',
    template_code VARCHAR(64) NOT NULL COMMENT '妯℃澘缂栫爜',
    template_name VARCHAR(100) NOT NULL COMMENT '妯℃澘鍚嶇О',
    template_type VARCHAR(32) NOT NULL COMMENT '妯℃澘绫诲瀷:report/application/loan',
    template_type_label VARCHAR(32) NOT NULL COMMENT '妯℃澘绫诲瀷涓枃鍚?,
    category_code VARCHAR(64) NOT NULL COMMENT '鍒嗙被缂栫爜',
    template_description VARCHAR(500) COMMENT '妯℃澘璇存槑',
    numbering_rule VARCHAR(64) COMMENT '缂栧彿瑙勫垯',
    form_design_code VARCHAR(64) COMMENT '表单设计编码',
    icon_color VARCHAR(32) DEFAULT 'blue' COMMENT '涓婚鑹?,
    enabled TINYINT DEFAULT 1 COMMENT '鏄惁鍚敤',
    publish_status VARCHAR(16) DEFAULT 'ENABLED' COMMENT '鍙戝竷鐘舵€?,
    print_mode VARCHAR(64) COMMENT '鎵撳嵃鏂瑰紡',
    approval_flow VARCHAR(64) COMMENT '瀹℃壒娴佺▼缂栫爜',
    flow_name VARCHAR(100) COMMENT '瀹℃壒娴佺▼鍚嶇О',
    payment_mode VARCHAR(64) COMMENT '浠樻鑱斿姩妯″紡',
    allocation_form VARCHAR(64) COMMENT '鍒嗘憡琛ㄥ崟',
    ai_audit_mode VARCHAR(64) DEFAULT 'disabled' COMMENT 'AI瀹℃牳妯″紡',
    highlights VARCHAR(500) COMMENT '鍗＄墖浜偣锛屼娇鐢▅鍒嗛殧',
    owner_name VARCHAR(64) COMMENT '缁存姢浜?,
    sort_order INT DEFAULT 0 COMMENT '鎺掑簭鍙?,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    UNIQUE KEY uk_template_code (template_code),
    KEY idx_category_code (category_code),
    KEY idx_template_type (template_type),
    KEY idx_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍗曟嵁娴佺▼妯℃澘琛?;

ALTER TABLE pm_document_template
    ADD COLUMN IF NOT EXISTS numbering_rule VARCHAR(64) COMMENT '缂栧彿瑙勫垯' AFTER template_description;

ALTER TABLE pm_document_template
    ADD COLUMN IF NOT EXISTS form_design_code VARCHAR(64) COMMENT '表单设计编码' AFTER numbering_rule;

ALTER TABLE pm_document_template
    ADD COLUMN IF NOT EXISTS allocation_form VARCHAR(64) COMMENT '鍒嗘憡琛ㄥ崟' AFTER payment_mode;

ALTER TABLE pm_document_template
    DROP COLUMN IF EXISTS split_payment,
    DROP COLUMN IF EXISTS travel_form,
    DROP COLUMN IF EXISTS relation_remark,
    DROP COLUMN IF EXISTS validation_remark,
    DROP COLUMN IF EXISTS installment_remark;

CREATE TABLE IF NOT EXISTS pm_template_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鑼冨洿鏄庣粏ID',
    template_id BIGINT NOT NULL COMMENT '妯℃澘ID',
    option_type VARCHAR(32) NOT NULL COMMENT '明细类型:SCOPE_DEPARTMENT/SCOPE_EXPENSE_TYPE/SCOPE_AMOUNT_MIN/SCOPE_AMOUNT_MAX/TAG_OPTION/INSTALLMENT_OPTION',
    option_code VARCHAR(64) NOT NULL COMMENT '閫夐」缂栫爜',
    option_label VARCHAR(64) NOT NULL COMMENT '閫夐」鍚嶇О',
    sort_order INT DEFAULT 0 COMMENT '鎺掑簭鍙?,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    KEY idx_template_id (template_id),
    KEY idx_option_type (option_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='妯℃澘鑼冨洿鏍囩鏄庣粏琛?;

INSERT INTO sys_company (
    company_id, company_code, company_name, status
) VALUES
    ('GROUP_HQ', 'GROUP_HQ', '集团总部', 1)
ON DUPLICATE KEY UPDATE
    company_code = VALUES(company_code),
    company_name = VALUES(company_name),
    status = VALUES(status);

INSERT IGNORE INTO sys_user (
    username, password, name, phone, email, company_id, position, labor_relation_belong, status, feishu_user_id
) VALUES
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '13800138000', 'admin@finex.com', 'GROUP_HQ', '系统管理员', '财务共享中心', 1, NULL),
    ('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13800138001', 'zhangsan@finex.com', 'GROUP_HQ', '财务经理', '华东运营中心', 1, NULL),
    ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13800138002', 'lisi@finex.com', 'GROUP_HQ', '报销专员', '总部职能中心', 1, NULL);

INSERT INTO sys_department (
    company_id, dept_code, dept_name, parent_id, wecom_department_id, dingtalk_department_id,
    feishu_department_id, sync_source, sync_enabled, last_sync_at, status, sort_order
) VALUES
    ('GROUP_HQ', 'HEAD_OFFICE', '总部', NULL, NULL, NULL, NULL, 'MANUAL', 1, NULL, 1, 10),
    ('GROUP_HQ', 'FINANCE_CENTER', '财务共享中心', NULL, NULL, NULL, NULL, 'MANUAL', 1, NULL, 1, 20),
    ('GROUP_HQ', 'EAST_OPERATION', '华东运营中心', NULL, NULL, NULL, NULL, 'MANUAL', 1, NULL, 1, 30),
    ('GROUP_HQ', 'HQ_FUNCTION', '总部职能中心', NULL, NULL, NULL, NULL, 'MANUAL', 1, NULL, 1, 40)
ON DUPLICATE KEY UPDATE
    company_id = VALUES(company_id),
    dept_name = VALUES(dept_name),
    parent_id = VALUES(parent_id),
    wecom_department_id = VALUES(wecom_department_id),
    dingtalk_department_id = VALUES(dingtalk_department_id),
    feishu_department_id = VALUES(feishu_department_id),
    sync_source = VALUES(sync_source),
    sync_enabled = VALUES(sync_enabled),
    last_sync_at = VALUES(last_sync_at),
    status = VALUES(status),
    sort_order = VALUES(sort_order);

UPDATE sys_department child
JOIN sys_department parent ON parent.dept_code = 'HEAD_OFFICE'
SET child.parent_id = parent.id
WHERE child.dept_code IN ('FINANCE_CENTER', 'EAST_OPERATION', 'HQ_FUNCTION');

UPDATE sys_department
SET parent_id = NULL
WHERE dept_code = 'HEAD_OFFICE';

UPDATE sys_user
SET labor_relation_belong = COALESCE(NULLIF(labor_relation_belong, ''), '总部')
WHERE labor_relation_belong IS NULL OR labor_relation_belong = '';

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'FINANCE_CENTER'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username = 'admin';

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'EAST_OPERATION'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username = 'zhangsan';

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'HQ_FUNCTION'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username = 'lisi';

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '鎷涘晢閾惰', '涓婃捣闄嗗鍢存敮琛?, u.name, '6225888888881001', '宸ヨ祫鍗?, 1, 1
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6225888888881001'
  );

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '寤鸿閾惰', '涓婃捣寮犳睙鏀', u.name, '6217000012345678', '鎶ラ攢鍗?, 1, 1
FROM sys_user u
WHERE u.username = 'zhangsan'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6217000012345678'
  );

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '宸ュ晢閾惰', '涓婃捣寰愭眹鏀', u.name, '6222000098765432', '鎶ラ攢鍗?, 1, 1
FROM sys_user u
WHERE u.username = 'lisi'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6222000098765432'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '3鏈堟姤閿€鍗曞鍑?xlsx', '鎶ラ攢鏄庣粏瀵煎嚭', 'DOWNLOADING', 68, '4.6 MB', DATE_SUB(NOW(), INTERVAL 3 MINUTE), NULL
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '3鏈堟姤閿€鍗曞鍑?xlsx'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '寰呭鎵瑰崟鎹竻鍗?xlsx', '瀹℃壒娓呭崟瀵煎嚭', 'COMPLETED', 100, '2.1 MB', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 2 MINUTE
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '寰呭鎵瑰崟鎹竻鍗?xlsx'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '鍙戠エ楠岀湡缁撴灉.csv', '鍙戠エ绠＄悊瀵煎嚭', 'COMPLETED', 100, '860 KB', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 MINUTE
FROM sys_user u
WHERE u.username = 'zhangsan'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '鍙戠エ楠岀湡缁撴灉.csv'
  );

INSERT INTO pm_template_category (category_code, category_name, category_description, sort_order, status)
VALUES
    ('enterprise-payment', '浼佷笟寰€鏉ョ被', '閫傜敤浜庡鍏粯娆俱€佸鐢ㄩ噾銆佹娂閲戝拰渚涘簲鍟嗙粨绠楃瓑鍦烘櫙銆?, 10, 1),
    ('employee-expense', '鍛樺伐璐圭敤绫?, '閫傜敤浜庡憳宸ユ姤閿€銆佸€熸敮鍜屽洟闃熻垂鐢ㄥ綊闆嗐€?, 20, 1),
    ('business-application', '浜嬮」鐢宠绫?, '閫傜敤浜庨」鐩敵璇枫€佷粯娆捐Е鍙戝拰涓撻」瀹℃壒銆?, 30, 1)
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    category_description = VALUES(category_description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO pm_custom_archive_design (archive_code, archive_name, archive_type, archive_description, status)
VALUES
    ('PROCESS_TAG_OPTIONS', '标签设置', 'SELECT', '用于流程管理中标签设置的默认选择档案', 1),
    ('PROCESS_INSTALLMENT_OPTIONS', '分期付款', 'SELECT', '用于流程管理中分期付款的默认选择档案', 1),
    ('PROCESS_DEPT_AUTO_RULE', '部门自动划分', 'AUTO_RULE', '根据部门与单据条件自动返回匹配结果项', 1)
ON DUPLICATE KEY UPDATE
    archive_name = VALUES(archive_name),
    archive_type = VALUES(archive_type),
    archive_description = VALUES(archive_description),
    status = VALUES(status);
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'high-frequency', '高频报销', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'high-frequency'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'public-payment', '对公支付', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'public-payment'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'ai-audit', 'AI审核', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'ai-audit'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'phase-payment', '阶段付款', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'phase-payment'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'milestone-payment', '里程碑付款', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'milestone-payment'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'monthly-settlement', '月度结算', NULL, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'monthly-settlement'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'east-operation-large', '华东运营大额', 10, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'east-operation-large'
  );
INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'headquarter-standard', '总部标准流程', 20, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'headquarter-standard'
  );
INSERT INTO pm_custom_archive_rule (archive_item_id, group_no, field_key, operator, compare_value)
SELECT i.id, 1, 'submitterDeptId', 'IN', CONCAT('[\"', CAST(d.id AS CHAR), '\"]')
FROM pm_custom_archive_item i
JOIN pm_custom_archive_design a ON a.id = i.archive_id
JOIN sys_department d ON d.dept_code = 'EAST_OPERATION'
WHERE a.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND i.item_code = 'east-operation-large'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_rule r
      WHERE r.archive_item_id = i.id AND r.group_no = 1 AND r.field_key = 'submitterDeptId' AND r.operator = 'IN'
  );
INSERT INTO pm_custom_archive_rule (archive_item_id, group_no, field_key, operator, compare_value)
SELECT i.id, 1, 'amount', 'GE', '5000'
FROM pm_custom_archive_item i
JOIN pm_custom_archive_design a ON a.id = i.archive_id
WHERE a.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND i.item_code = 'east-operation-large'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_rule r
      WHERE r.archive_item_id = i.id AND r.group_no = 1 AND r.field_key = 'amount' AND r.operator = 'GE'
  );
INSERT INTO pm_custom_archive_rule (archive_item_id, group_no, field_key, operator, compare_value)
SELECT i.id, 1, 'submitterDeptId', 'IN', CONCAT('[\"', CAST(d.id AS CHAR), '\"]')
FROM pm_custom_archive_item i
JOIN pm_custom_archive_design a ON a.id = i.archive_id
JOIN sys_department d ON d.dept_code = 'HQ_FUNCTION'
WHERE a.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND i.item_code = 'headquarter-standard'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_rule r
      WHERE r.archive_item_id = i.id AND r.group_no = 1 AND r.field_key = 'submitterDeptId' AND r.operator = 'IN'
  );
INSERT INTO pm_custom_archive_rule (archive_item_id, group_no, field_key, operator, compare_value)
SELECT i.id, 2, 'documentType', 'EQ', JSON_QUOTE('application')
FROM pm_custom_archive_item i
JOIN pm_custom_archive_design a ON a.id = i.archive_id
WHERE a.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND i.item_code = 'headquarter-standard'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_rule r
      WHERE r.archive_item_id = i.id AND r.group_no = 2 AND r.field_key = 'documentType' AND r.operator = 'EQ'
  );
INSERT INTO pm_custom_archive_rule (archive_item_id, group_no, field_key, operator, compare_value)
SELECT i.id, 2, 'laborRelationBelong', 'CONTAINS', JSON_QUOTE('总部')
FROM pm_custom_archive_item i
JOIN pm_custom_archive_design a ON a.id = i.archive_id
WHERE a.archive_code = 'PROCESS_DEPT_AUTO_RULE'
  AND i.item_code = 'headquarter-standard'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_rule r
      WHERE r.archive_item_id = i.id AND r.group_no = 2 AND r.field_key = 'laborRelationBelong' AND r.operator = 'CONTAINS'
  );

INSERT INTO pm_document_template (
    template_code, template_name, template_type, template_type_label, category_code,
    template_description, numbering_rule, form_design_code, icon_color, enabled, publish_status, print_mode,
    approval_flow, flow_name, payment_mode, allocation_form, ai_audit_mode, highlights,
    owner_name, sort_order
)
VALUES
    (
        'PUB-EXP-01', '瀵瑰叕宸梾浠樻', 'report', '鎶ラ攢鍗?, 'enterprise-payment',
        '閫傜敤浜庝緵搴斿晢鍨粯宸梾璐圭敤鍚庣殑缁熶竴鎶ラ攢涓庝粯娆炬祦杞€?, 'FX_DATE_4SEQ', 'expense-public-form', 'blue', 1, 'ENABLED', 'default-print',
        'public-payment-flow', '瀵瑰叕浠樻娴佺▼', 'public-payment', 'allocation-default', 'standard', '鏀寔绉诲姩绔彁鍗晐鑱斿姩浠樻鍗晐AI 瀹℃牳',
        '娴佺▼涓績', 10
    ),
    (
        'PUB-APP-02', '瀵瑰叕浠樻鐢宠', 'application', '鐢宠鍗?, 'enterprise-payment',
        '鐢ㄤ簬閲囪喘棰勪粯娆俱€佹湇鍔′粯娆惧拰闃舵灏炬鐨勫鎵广€?, 'FX_DATE_4SEQ', 'application-public-form', 'cyan', 1, 'ENABLED', 'finance-archive',
        'public-payment-flow', '瀵瑰叕浠樻娴佺▼', 'public-payment', 'allocation-project', 'standard', '鏀寔绉诲姩绔彁鍗晐鑱斿姩浠樻鍗晐鏍囧噯瀹℃壒閾捐矾',
        '璐㈠姟鍏变韩', 20
    ),
    (
        'EMP-EXP-11', '鏍囧噯鍛樺伐鎶ラ攢', 'report', '鎶ラ攢鍗?, 'employee-expense',
        '瑕嗙洊宸梾銆佷氦閫氥€佷綇瀹裤€佸姙鍏瓑甯歌鍛樺伐璐圭敤銆?, 'FX_DATE_4SEQ', 'expense-standard-form', 'blue', 1, 'ENABLED', 'default-print',
        'normal-expense-flow', '鏍囧噯鎶ラ攢娴佺▼', 'none', 'allocation-default', 'standard', '鏀寔绉诲姩绔彁鍗晐AI 瀹℃牳|鏍囧噯瀹℃壒閾捐矾',
        '璐圭敤涓績', 10
    ),
    (
        'EMP-LOAN-13', '鍛樺伐鍊熸鍗?, 'loan', '鍊熸鍗?, 'employee-expense',
        '閫傜敤浜庝复鏃跺€熸敮銆佸樊鏃呭€熸鍜屽鐢ㄩ噾鏍搁攢銆?, 'FX_DATE_4SEQ', 'loan-standard-form', 'orange', 1, 'ENABLED', 'default-print',
        'loan-return-flow', '鍊熸涓庡綊杩樻祦绋?, 'private-payment', 'allocation-default', 'standard', '鏀寔绉诲姩绔彁鍗晐鑱斿姩浠樻鍗晐鍒嗘湡浠樻',
        '璐圭敤涓績', 20
    ),
    (
        'BIZ-APP-21', '椤圭洰绔嬮」鐢宠', 'application', '鐢宠鍗?, 'business-application',
        '鐢ㄤ簬椤圭洰绔嬮」銆侀绠楀喕缁撳拰璺ㄩ儴闂ㄥ崗鍚屽鎵广€?, 'FX_DATE_4SEQ', 'application-project-form', 'blue', 1, 'ENABLED', 'finance-archive',
        'normal-expense-flow', '椤圭洰绔嬮」娴佺▼', 'none', 'allocation-project', 'strict', '鏀寔绉诲姩绔彁鍗晐AI 瀹℃牳|鏍囧噯瀹℃壒閾捐矾',
        '椤圭洰绠＄悊', 10
    ),
    (
        'BIZ-EXP-23', '鍚堝悓浠樻鎶ラ攢', 'report', '鎶ラ攢鍗?, 'business-application',
        '鐢ㄤ簬鍚堝悓鎵ц涓殑浠樻鎶ラ攢涓庡彴璐﹁褰曘€?, 'FX_DATE_4SEQ', 'expense-public-form', 'blue', 0, 'DRAFT', 'finance-archive',
        'public-payment-flow', '鍚堝悓浠樻娴佺▼', 'public-payment', 'allocation-project', 'standard', '鏀寔绉诲姩绔彁鍗晐鑱斿姩浠樻鍗晐AI 瀹℃牳',
        '鍚堝悓绠＄悊', 20
    )
ON DUPLICATE KEY UPDATE
    template_name = VALUES(template_name),
    template_type = VALUES(template_type),
    template_type_label = VALUES(template_type_label),
    category_code = VALUES(category_code),
    template_description = VALUES(template_description),
    numbering_rule = VALUES(numbering_rule),
    form_design_code = VALUES(form_design_code),
    icon_color = VALUES(icon_color),
    enabled = VALUES(enabled),
    publish_status = VALUES(publish_status),
    print_mode = VALUES(print_mode),
    approval_flow = VALUES(approval_flow),
    flow_name = VALUES(flow_name),
    payment_mode = VALUES(payment_mode),
    allocation_form = VALUES(allocation_form),
    ai_audit_mode = VALUES(ai_audit_mode),
    highlights = VALUES(highlights),
    owner_name = VALUES(owner_name),
    sort_order = VALUES(sort_order);

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'EXPENSE_TYPE', 'travel', '宸梾璐?, 10
FROM pm_document_template t
WHERE t.template_code = 'EMP-EXP-11'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'EXPENSE_TYPE' AND s.option_code = 'travel'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'TAG_OPTION', 'ai-audit', 'AI瀹℃牳', 10
FROM pm_document_template t
WHERE t.template_code = 'EMP-EXP-11'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'TAG_OPTION' AND s.option_code = 'ai-audit'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'TAG_OPTION', 'public-payment', '瀵瑰叕涓氬姟', 10
FROM pm_document_template t
WHERE t.template_code = 'PUB-APP-02'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'TAG_OPTION' AND s.option_code = 'public-payment'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'INSTALLMENT_OPTION', 'milestone-payment', '閲岀▼纰戜粯娆?, 10
FROM pm_document_template t
WHERE t.template_code = 'EMP-LOAN-13'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'INSTALLMENT_OPTION' AND s.option_code = 'milestone-payment'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'SCOPE_OPTION', 'department', '闄愬畾閮ㄩ棬浣跨敤', 10
FROM pm_document_template t
WHERE t.template_code = 'BIZ-APP-21'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'SCOPE_OPTION' AND s.option_code = 'department'
  );

-- 鎵€鏈夊垵濮嬪寲璐﹀彿鐨勫師濮嬪瘑鐮侀兘鏄?123456

