CREATE DATABASE IF NOT EXISTS finex_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_company (
    company_id VARCHAR(64) NOT NULL COMMENT 'company id',
    company_code VARCHAR(64) NOT NULL COMMENT 'company code',
    company_name VARCHAR(128) NOT NULL COMMENT 'company name',
    invoice_title VARCHAR(200) NULL COMMENT 'invoice title',
    tax_no VARCHAR(100) NULL COMMENT 'tax number',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status:1 enabled 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (company_id),
    CONSTRAINT uk_sys_company_company_code UNIQUE (company_code),
    KEY idx_sys_company_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='company master data';

CREATE TABLE IF NOT EXISTS sys_company_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'company bank account id',
    company_id VARCHAR(64) NOT NULL COMMENT 'company id',
    bank_name VARCHAR(200) NOT NULL COMMENT 'bank name',
    branch_name VARCHAR(200) NULL COMMENT 'branch name',
    bank_code VARCHAR(64) NULL COMMENT 'bank code',
    branch_code VARCHAR(64) NULL COMMENT 'branch code',
    cnaps_code VARCHAR(64) NULL COMMENT 'cnaps code',
    account_name VARCHAR(200) NOT NULL COMMENT 'account name',
    account_no VARCHAR(100) NOT NULL COMMENT 'account number',
    account_type VARCHAR(64) NULL COMMENT 'account type',
    account_usage VARCHAR(100) NULL COMMENT 'account usage',
    currency_code VARCHAR(32) NULL COMMENT 'currency code',
    default_account TINYINT NOT NULL DEFAULT 0 COMMENT 'default account',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status:1 enabled 0 disabled',
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

CREATE TABLE IF NOT EXISTS sys_bank_catalog (
    bank_code VARCHAR(64) NOT NULL COMMENT 'bank code',
    bank_name VARCHAR(200) NOT NULL COMMENT 'bank name',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status:1 enabled 0 disabled',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    PRIMARY KEY (bank_code),
    KEY idx_sys_bank_catalog_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='domestic bank catalog';

CREATE TABLE IF NOT EXISTS sys_bank_branch_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'bank branch id',
    bank_code VARCHAR(64) NOT NULL COMMENT 'bank code',
    bank_name VARCHAR(200) NOT NULL COMMENT 'bank name',
    province VARCHAR(64) NOT NULL COMMENT 'province',
    city VARCHAR(64) NOT NULL COMMENT 'city',
    branch_code VARCHAR(64) NOT NULL COMMENT 'branch code',
    branch_name VARCHAR(200) NOT NULL COMMENT 'branch name',
    cnaps_code VARCHAR(64) NULL COMMENT 'cnaps code',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status:1 enabled 0 disabled',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    CONSTRAINT uk_sys_bank_branch_catalog_code UNIQUE (branch_code),
    CONSTRAINT uk_sys_bank_branch_catalog_cnaps UNIQUE (cnaps_code),
    KEY idx_sys_bank_branch_catalog_bank_area (bank_code, province, city, status),
    KEY idx_sys_bank_branch_catalog_bank_branch (bank_code, branch_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='domestic bank branch catalog';

INSERT INTO sys_bank_catalog (bank_code, bank_name, status, sort_order) VALUES
    ('ICBC', '中国工商银行', 1, 10),
    ('ABC', '中国农业银行', 1, 20),
    ('BOC', '中国银行', 1, 30),
    ('CCB', '中国建设银行', 1, 40),
    ('CMB', '招商银行', 1, 50)
ON DUPLICATE KEY UPDATE
    bank_name = VALUES(bank_name),
    status = VALUES(status),
    sort_order = VALUES(sort_order);

INSERT INTO sys_bank_branch_catalog (bank_code, bank_name, province, city, branch_code, branch_name, cnaps_code, status, sort_order) VALUES
    ('ICBC', '中国工商银行', '上海市', '上海市', 'ICBC-SH-PD', '中国工商银行上海浦东支行', '102290040011', 1, 10),
    ('ICBC', '中国工商银行', '北京市', '北京市', 'ICBC-BJ-HD', '中国工商银行北京海淀支行', '102100099996', 1, 20),
    ('ABC', '中国农业银行', '广东省', '广州市', 'ABC-GZ-TH', '中国农业银行广州天河支行', '103581000123', 1, 10),
    ('ABC', '中国农业银行', '浙江省', '杭州市', 'ABC-HZ-XH', '中国农业银行杭州西湖支行', '103331057771', 1, 20),
    ('BOC', '中国银行', '广东省', '深圳市', 'BOC-SZ-NS', '中国银行深圳南山支行', '104584003210', 1, 10),
    ('BOC', '中国银行', '上海市', '上海市', 'BOC-SH-MH', '中国银行上海闵行支行', '104290045678', 1, 20),
    ('CCB', '中国建设银行', '北京市', '北京市', 'CCB-BJ-CY', '中国建设银行北京朝阳支行', '105100000017', 1, 10),
    ('CCB', '中国建设银行', '四川省', '成都市', 'CCB-CD-GX', '中国建设银行成都高新支行', '105651001888', 1, 20),
    ('CMB', '招商银行', '广东省', '深圳市', 'CMB-SZ-FH', '招商银行深圳福华支行', '308584000013', 1, 10),
    ('CMB', '招商银行', '上海市', '上海市', 'CMB-SH-LJ', '招商银行上海陆家嘴支行', '308290003456', 1, 20)
ON DUPLICATE KEY UPDATE
    bank_name = VALUES(bank_name),
    province = VALUES(province),
    city = VALUES(city),
    branch_name = VALUES(branch_name),
    cnaps_code = VALUES(cnaps_code),
    status = VALUES(status),
    sort_order = VALUES(sort_order);

CREATE TABLE IF NOT EXISTS pm_bank_payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'bank payment record id',
    task_id BIGINT NOT NULL COMMENT 'payment task id',
    document_code VARCHAR(64) NOT NULL COMMENT 'document code',
    company_bank_account_id BIGINT NULL COMMENT 'company bank account id',
    bank_provider VARCHAR(64) NOT NULL COMMENT 'bank provider',
    bank_channel VARCHAR(64) NOT NULL COMMENT 'bank channel',
    manual_paid TINYINT NOT NULL DEFAULT 0 COMMENT 'manual paid flag',
    push_request_no VARCHAR(128) NULL COMMENT 'push request no',
    bank_order_no VARCHAR(128) NULL COMMENT 'bank order no',
    bank_flow_no VARCHAR(128) NULL COMMENT 'bank flow no',
    push_payload_json LONGTEXT NULL COMMENT 'push payload json',
    push_result_json LONGTEXT NULL COMMENT 'push result json',
    callback_payload_json LONGTEXT NULL COMMENT 'callback payload json',
    callback_received_at DATETIME NULL COMMENT 'callback received at',
    paid_at DATETIME NULL COMMENT 'paid at',
    receipt_status VARCHAR(64) NULL COMMENT 'receipt status',
    receipt_received_at DATETIME NULL COMMENT 'receipt received at',
    last_receipt_query_at DATETIME NULL COMMENT 'last receipt query at',
    receipt_query_count INT NOT NULL DEFAULT 0 COMMENT 'receipt query count',
    receipt_attachment_id VARCHAR(64) NULL COMMENT 'receipt attachment id',
    receipt_file_name VARCHAR(255) NULL COMMENT 'receipt file name',
    receipt_result_json LONGTEXT NULL COMMENT 'receipt result json',
    last_error_message VARCHAR(1000) NULL COMMENT 'last error message',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
    KEY idx_pm_bank_payment_record_task (task_id),
    KEY idx_pm_bank_payment_record_document (document_code),
    KEY idx_pm_bank_payment_record_account (company_bank_account_id),
    KEY idx_pm_bank_payment_record_receipt (receipt_status, manual_paid),
    CONSTRAINT fk_pm_bank_payment_record_task FOREIGN KEY (task_id) REFERENCES pm_document_task(id),
    CONSTRAINT fk_pm_bank_payment_record_document FOREIGN KEY (document_code) REFERENCES pm_document_instance(document_code),
    CONSTRAINT fk_pm_bank_payment_record_account FOREIGN KEY (company_bank_account_id) REFERENCES sys_company_bank_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bank payment tracking record';

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
    stat_department_belong VARCHAR(100) NULL COMMENT '统计部门归属',
    stat_region_belong VARCHAR(100) NULL COMMENT '统计大区归属',
    stat_area_belong VARCHAR(100) NULL COMMENT '统计区域归属',
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
    stat_department_belong VARCHAR(100) COMMENT '统计部门归属',
    stat_region_belong VARCHAR(100) COMMENT '统计大区归属',
    stat_area_belong VARCHAR(100) COMMENT '统计区域归属',
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

ALTER TABLE sys_department
    ADD COLUMN IF NOT EXISTS stat_department_belong VARCHAR(100) COMMENT '统计部门归属' AFTER sync_enabled;

ALTER TABLE sys_department
    ADD COLUMN IF NOT EXISTS stat_region_belong VARCHAR(100) COMMENT '统计大区归属' AFTER stat_department_belong;

ALTER TABLE sys_department
    ADD COLUMN IF NOT EXISTS stat_area_belong VARCHAR(100) COMMENT '统计区域归属' AFTER stat_region_belong;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS labor_relation_belong VARCHAR(100) COMMENT '劳动关系归属' AFTER position;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS stat_department_belong VARCHAR(100) COMMENT '统计部门归属' AFTER labor_relation_belong;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS stat_region_belong VARCHAR(100) COMMENT '统计大区归属' AFTER stat_department_belong;

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS stat_area_belong VARCHAR(100) COMMENT '统计区域归属' AFTER stat_region_belong;

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
    bank_code VARCHAR(64) NULL COMMENT '寮€鎴烽摱琛岀紪鐮?',
    branch_code VARCHAR(64) NULL COMMENT '鍒嗘敮琛岀紪鐮?',
    cnaps_code VARCHAR(64) NULL COMMENT '鑱旇鍙?',
    province VARCHAR(64) NULL COMMENT '寮€鎴风渷',
    city VARCHAR(64) NULL COMMENT '寮€鎴峰競',
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
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自定义档案ID',
    archive_code VARCHAR(64) NOT NULL COMMENT '档案编码',
    archive_name VARCHAR(100) NOT NULL COMMENT '档案名称',
    archive_type VARCHAR(32) NOT NULL COMMENT '档案类型:SELECT可选档案/AUTO_RULE自动划分',
    archive_description VARCHAR(255) COMMENT '档案说明',
    status TINYINT DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_archive_code (archive_code),
    KEY idx_archive_type (archive_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案设计表';

ALTER TABLE pm_custom_archive_design
    ADD COLUMN IF NOT EXISTS archive_description VARCHAR(255) COMMENT '档案说明' AFTER archive_type;

ALTER TABLE pm_custom_archive_design
    MODIFY COLUMN archive_type VARCHAR(32) NOT NULL COMMENT '档案类型:SELECT可选档案/AUTO_RULE自动划分';

ALTER TABLE pm_custom_archive_design
    DROP COLUMN IF EXISTS sort_order;

CREATE TABLE IF NOT EXISTS pm_custom_archive_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '档案结果项ID',
    archive_id BIGINT NOT NULL COMMENT '所属档案ID',
    item_code VARCHAR(64) NOT NULL COMMENT '结果项编码',
    item_name VARCHAR(100) NOT NULL COMMENT '结果项名称',
    priority INT DEFAULT 1 COMMENT '优先级，值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_archive_item_code (archive_id, item_code),
    KEY idx_archive_item_status (archive_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案结果项表';

ALTER TABLE pm_custom_archive_item
    DROP COLUMN IF EXISTS item_value,
    DROP COLUMN IF EXISTS sort_order;

CREATE TABLE IF NOT EXISTS pm_custom_archive_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自动划分规则ID',
    archive_item_id BIGINT NOT NULL COMMENT '归属结果项ID',
    group_no INT DEFAULT 1 COMMENT '规则组号，同组条件为且、组间为或',
    field_key VARCHAR(64) NOT NULL COMMENT '匹配字段标识',
    operator VARCHAR(32) NOT NULL COMMENT '比较运算符:EQ/NE/IN/NOT_IN/GT/BETWEEN/CONTAINS',
    compare_value VARCHAR(500) COMMENT '比较值，按JSON序列化存储',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_archive_item_group (archive_item_id, group_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案自动划分规则表';

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

-- FIXED_ASSETS_INIT_BEGIN
USE finex_db;

SET NAMES utf8mb4;


CREATE TABLE IF NOT EXISTS fa_asset_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
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
    asset_code VARCHAR(64) NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
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
    bill_no VARCHAR(64) NOT NULL,
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
    asset_code VARCHAR(64) NOT NULL,
    asset_name VARCHAR(128) NULL,
    change_type VARCHAR(32) NOT NULL,
    category_id BIGINT NULL,
    category_code VARCHAR(64) NULL,
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
    run_no VARCHAR(64) NOT NULL,
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
    asset_code VARCHAR(64) NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
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
    bill_no VARCHAR(64) NOT NULL,
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
    asset_code VARCHAR(64) NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
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
    batch_no VARCHAR(64) NOT NULL,
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
    asset_code VARCHAR(64) NULL,
    asset_name VARCHAR(128) NULL,
    category_code VARCHAR(64) NULL,
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
-- FIXED_ASSETS_INIT_END

-- EXPENSE_VOUCHER_GENERATION_INIT_BEGIN
CREATE TABLE IF NOT EXISTS exp_voucher_template_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    credit_account_code VARCHAR(64) NOT NULL,
    credit_account_name VARCHAR(128) NULL,
    voucher_type VARCHAR(16) NOT NULL DEFAULT '记',
    summary_rule VARCHAR(255) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_template_policy (company_id, template_code),
    KEY idx_exp_voucher_template_policy_enabled (company_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher template policy';

CREATE TABLE IF NOT EXISTS exp_voucher_subject_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    expense_type_code VARCHAR(64) NOT NULL,
    expense_type_name VARCHAR(128) NULL,
    debit_account_code VARCHAR(64) NOT NULL,
    debit_account_name VARCHAR(128) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NULL,
    updated_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_subject_mapping (company_id, template_code, expense_type_code),
    KEY idx_exp_voucher_subject_mapping_enabled (company_id, template_code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher subject mapping';

CREATE TABLE IF NOT EXISTS exp_voucher_push_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    document_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_push_batch (company_id, batch_no),
    KEY idx_exp_voucher_push_batch_status (company_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push batch';

CREATE TABLE IF NOT EXISTS exp_voucher_push_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    batch_id BIGINT NULL,
    batch_no VARCHAR(64) NULL,
    document_code VARCHAR(64) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NULL,
    submitter_user_id BIGINT NULL,
    submitter_name VARCHAR(128) NULL,
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    push_status VARCHAR(16) NOT NULL,
    voucher_no VARCHAR(128) NULL,
    voucher_type VARCHAR(16) NULL,
    voucher_number INT NULL,
    fiscal_period INT NULL,
    bill_date DATE NULL,
    error_message VARCHAR(500) NULL,
    pushed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exp_voucher_push_document (company_id, document_code),
    KEY idx_exp_voucher_push_document_status (company_id, push_status),
    KEY idx_exp_voucher_push_document_voucher (company_id, voucher_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push document';

CREATE TABLE IF NOT EXISTS exp_voucher_push_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id VARCHAR(64) NOT NULL,
    push_document_id BIGINT NOT NULL,
    entry_no INT NOT NULL,
    direction VARCHAR(16) NOT NULL,
    digest VARCHAR(255) NULL,
    account_code VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NULL,
    expense_type_code VARCHAR(64) NULL,
    expense_type_name VARCHAR(128) NULL,
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exp_voucher_push_entry_document (company_id, push_document_id),
    KEY idx_exp_voucher_push_entry_account (company_id, account_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Expense voucher push entry';
-- EXPENSE_VOUCHER_GENERATION_INIT_END

CREATE TABLE IF NOT EXISTS ea_agent_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_code VARCHAR(32) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    agent_description VARCHAR(500) NULL,
    icon_key VARCHAR(64) NULL,
    theme_key VARCHAR(64) NULL,
    cover_color VARCHAR(32) NULL,
    tags_json TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    latest_version_no INT NOT NULL DEFAULT 1,
    published_version_id BIGINT NULL,
    last_run_id BIGINT NULL,
    last_run_status VARCHAR(32) NULL,
    last_run_summary VARCHAR(500) NULL,
    last_run_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_definition_code (agent_code),
    KEY idx_ea_agent_definition_owner (owner_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent definition';

CREATE TABLE IF NOT EXISTS ea_agent_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_label VARCHAR(64) NULL,
    config_json LONGTEXT NOT NULL,
    published INT NOT NULL DEFAULT 0,
    created_by_user_id BIGINT NOT NULL,
    created_by_name VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_version_agent_no (agent_id, version_no),
    KEY idx_ea_agent_version_agent (agent_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent version';

CREATE TABLE IF NOT EXISTS ea_agent_trigger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    schedule_mode VARCHAR(32) NULL,
    cron_expression VARCHAR(64) NULL,
    interval_minutes INT NULL,
    event_code VARCHAR(64) NULL,
    config_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_trigger_agent (agent_id, trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent trigger';

CREATE TABLE IF NOT EXISTS ea_agent_tool_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    tool_code VARCHAR(64) NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    credential_ref_code VARCHAR(64) NULL,
    config_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_tool_binding_agent (agent_id, tool_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent tool binding';

CREATE TABLE IF NOT EXISTS ea_agent_credential_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    credential_code VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    credential_name VARCHAR(100) NOT NULL,
    masked_key VARCHAR(128) NULL,
    secret_payload TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_credential_ref_code (owner_user_id, credential_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent credential reference';

CREATE TABLE IF NOT EXISTS ea_agent_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no VARCHAR(40) NOT NULL,
    agent_id BIGINT NOT NULL,
    agent_version_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    trigger_source VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500) NULL,
    summary VARCHAR(500) NULL,
    input_json LONGTEXT NULL,
    output_json LONGTEXT NULL,
    scheduled_fire_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_run_no (run_no),
    KEY idx_ea_agent_run_agent (agent_id, created_at),
    KEY idx_ea_agent_run_owner (owner_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent run';

CREATE TABLE IF NOT EXISTS ea_agent_run_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    node_label VARCHAR(100) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500) NULL,
    input_json LONGTEXT NULL,
    output_json LONGTEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_run_step_run (run_id, step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent run step';

CREATE TABLE IF NOT EXISTS ea_agent_run_artifact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    artifact_key VARCHAR(64) NOT NULL,
    artifact_type VARCHAR(32) NOT NULL,
    artifact_name VARCHAR(100) NULL,
    summary VARCHAR(500) NULL,
    content_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_run_artifact_run (run_id, artifact_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent run artifact';

CREATE TABLE IF NOT EXISTS ea_agent_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trigger_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    schedule_status VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    last_fire_at DATETIME NULL,
    next_fire_at DATETIME NULL,
    last_run_id BIGINT NULL,
    locked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_schedule_trigger (trigger_id),
    KEY idx_ea_agent_schedule_due (schedule_status, next_fire_at),
    KEY idx_ea_agent_schedule_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archive Agent schedule';
