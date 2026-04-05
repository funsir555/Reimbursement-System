USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_code_sequence (
    biz_key VARCHAR(64) NOT NULL COMMENT 'column comment',
    biz_date VARCHAR(8) NOT NULL COMMENT 'column comment',
    current_value BIGINT NOT NULL DEFAULT 0 COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    PRIMARY KEY (biz_key, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

INSERT INTO pm_code_sequence (
    biz_key,
    biz_date,
    current_value,
    created_at,
    updated_at
)
SELECT
    'DOCUMENT_TEMPLATE' AS biz_key,
    SUBSTRING(template_code, 3, 8) AS biz_date,
    MAX(CAST(RIGHT(template_code, 4) AS UNSIGNED)) AS current_value,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM pm_document_template
WHERE template_code REGEXP '^FX[0-9]{12}$'
GROUP BY SUBSTRING(template_code, 3, 8)
ON DUPLICATE KEY UPDATE
    current_value = GREATEST(current_value, VALUES(current_value)),
    updated_at = CURRENT_TIMESTAMP;

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

CREATE TABLE IF NOT EXISTS pm_expense_detail_design (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    detail_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    detail_name VARCHAR(100) NOT NULL COMMENT 'column comment',
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
    detail_title VARCHAR(200) NULL COMMENT 'column comment',
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
        'ALTER TABLE pm_document_expense_detail ADD COLUMN expense_type_code VARCHAR(64) NULL COMMENT ''column comment'' AFTER enterprise_mode',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'expense_type_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN business_scene_mode VARCHAR(32) NULL COMMENT ''column comment'' AFTER expense_type_code',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'business_scene_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN invoice_amount DECIMAL(18,2) NULL COMMENT ''column comment'' AFTER sort_order',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'invoice_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN actual_payment_amount DECIMAL(18,2) NULL COMMENT ''column comment'' AFTER invoice_amount',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'actual_payment_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD COLUMN pending_write_off_amount DECIMAL(18,2) NULL COMMENT ''column comment'' AFTER actual_payment_amount',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND COLUMN_NAME = 'pending_write_off_amount'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
        'ALTER TABLE pm_document_expense_detail ADD KEY idx_pm_document_expense_detail_document (document_code, sort_order)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'idx_pm_document_expense_detail_document'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_expense_detail ADD KEY idx_pm_document_expense_detail_design (detail_design_code)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_expense_detail'
      AND INDEX_NAME = 'idx_pm_document_expense_detail_design'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS pm_document_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    source_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    source_field_key VARCHAR(128) NOT NULL COMMENT 'column comment',
    target_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    target_template_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_relation_source_target (source_document_code, source_field_key, target_document_code),
    KEY idx_pm_document_relation_source (source_document_code, source_field_key, status),
    KEY idx_pm_document_relation_target (target_document_code, target_template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS pm_document_write_off (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'column comment',
    source_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    source_field_key VARCHAR(128) NOT NULL COMMENT 'column comment',
    target_document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    target_template_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    writeoff_source_kind VARCHAR(32) NOT NULL COMMENT 'column comment',
    requested_amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'column comment',
    effective_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    available_snapshot_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    remaining_snapshot_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    sort_order INT NOT NULL DEFAULT 1 COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_EFFECTIVE' COMMENT 'column comment',
    effective_at DATETIME NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_write_off_source_target (source_document_code, source_field_key, target_document_code),
    KEY idx_pm_document_write_off_source (source_document_code, source_field_key, status),
    KEY idx_pm_document_write_off_target (target_document_code, target_template_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS gl_Vender (
    cVenCode VARCHAR(255) NOT NULL COMMENT 'column comment',
    cVenName VARCHAR(255) NOT NULL COMMENT 'column comment',
    cVenAbbName VARCHAR(255) NULL COMMENT 'column comment',
    cVCCode VARCHAR(255) NULL COMMENT 'column comment',
    cTrade VARCHAR(255) NULL COMMENT 'column comment',
    cVenAddress VARCHAR(255) NULL COMMENT 'column comment',
    cVenRegCode VARCHAR(255) NULL COMMENT 'column comment',
    cVenBank VARCHAR(255) NULL COMMENT 'column comment',
    cVenAccount VARCHAR(255) NULL COMMENT 'column comment',
    cVenBankNub VARCHAR(255) NULL COMMENT 'column comment',
    cVenPerson VARCHAR(255) NULL COMMENT 'column comment',
    cVenPhone VARCHAR(255) NULL COMMENT 'column comment',
    cVenHand VARCHAR(255) NULL COMMENT 'column comment',
    cVenEmail VARCHAR(255) NULL COMMENT 'column comment',
    company_id VARCHAR(255) NULL COMMENT 'column comment',
    cMemo VARCHAR(255) NULL COMMENT 'column comment',
    dEndDate DATETIME NULL COMMENT 'column comment',
    bBusinessDate TINYINT DEFAULT 0 COMMENT 'column comment',
    bLicenceDate TINYINT DEFAULT 0 COMMENT 'column comment',
    bPassGMP TINYINT DEFAULT 0 COMMENT 'column comment',
    bProxyDate TINYINT DEFAULT 0 COMMENT 'column comment',
    bProxyForeign TINYINT DEFAULT 0 COMMENT 'column comment',
    bVenCargo TINYINT DEFAULT 0 COMMENT 'column comment',
    bVenService TINYINT DEFAULT 0 COMMENT 'column comment',
    bVenTax TINYINT DEFAULT 0 COMMENT 'column comment',
    cBarCode VARCHAR(255) NULL COMMENT 'column comment',
    cCreatePerson VARCHAR(255) NULL COMMENT 'column comment',
    cDCCode VARCHAR(255) NULL COMMENT 'column comment',
    cModifyPerson VARCHAR(255) NULL COMMENT 'column comment',
    cRelCustomer VARCHAR(255) NULL COMMENT 'column comment',
    cVenBankCode VARCHAR(255) NULL COMMENT 'column comment',
    cVenBP VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine10 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine11 INT NULL COMMENT 'column comment',
    cVenDefine12 INT NULL COMMENT 'column comment',
    cVenDefine13 DECIMAL(18,2) NULL COMMENT 'column comment',
    cVenDefine14 DECIMAL(18,2) NULL COMMENT 'column comment',
    cVenDefine15 DATETIME NULL COMMENT 'column comment',
    cVenDefine16 DATETIME NULL COMMENT 'column comment',
    cVenDefine3 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine4 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine5 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine6 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine7 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine8 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDefine9 VARCHAR(255) NULL COMMENT 'column comment',
    cVenDepart VARCHAR(255) NULL COMMENT 'column comment',
    cVenFax VARCHAR(255) NULL COMMENT 'column comment',
    cVenHeadCode VARCHAR(255) NULL COMMENT 'column comment',
    cVenIAddress VARCHAR(255) NULL COMMENT 'column comment',
    cVenIType VARCHAR(255) NULL COMMENT 'column comment',
    cVenLPerson VARCHAR(255) NULL COMMENT 'column comment',
    cVenPayCond VARCHAR(255) NULL COMMENT 'column comment',
    cVenPostCode VARCHAR(255) NULL COMMENT 'column comment',
    cVenPPerson VARCHAR(255) NULL COMMENT 'column comment',
    cVenTradeCCode VARCHAR(255) NULL COMMENT 'column comment',
    cVenWhCode VARCHAR(255) NULL COMMENT 'column comment',
    dBusinessEDate DATETIME NULL COMMENT 'column comment',
    dBusinessSDate DATETIME NULL COMMENT 'column comment',
    dLastDate DATETIME NULL COMMENT 'column comment',
    dLicenceEDate DATETIME NULL COMMENT 'column comment',
    dLicenceSDate DATETIME NULL COMMENT 'column comment',
    dLRDate DATETIME NULL COMMENT 'column comment',
    dModifyDate DATETIME NULL COMMENT 'column comment',
    dProxyEDate DATETIME NULL COMMENT 'column comment',
    dProxySDate DATETIME NULL COMMENT 'column comment',
    dVenDevDate DATETIME NULL COMMENT 'column comment',
    fRegistFund DECIMAL(18,2) NULL COMMENT 'column comment',
    iAPMoney DECIMAL(18,2) NULL COMMENT 'column comment',
    iBusinessADays INT NULL COMMENT 'column comment',
    iEmployeeNum INT NULL COMMENT 'column comment',
    iFrequency INT NULL COMMENT 'column comment',
    iGradeABC SMALLINT NULL COMMENT 'column comment',
    iId INT NULL COMMENT 'column comment',
    iLastMoney DECIMAL(18,2) NULL COMMENT 'column comment',
    iLicenceADays INT NULL COMMENT 'column comment',
    iLRMoney DECIMAL(18,2) NULL COMMENT 'column comment',
    iProxyADays INT NULL COMMENT 'column comment',
    iVenCreDate INT NULL COMMENT 'column comment',
    iVenCreGrade VARCHAR(255) NULL COMMENT 'column comment',
    iVenCreLine DECIMAL(18,2) NULL COMMENT 'column comment',
    iVenDisRate DECIMAL(18,2) NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    PRIMARY KEY (cVenCode),
    KEY idx_gl_Vender_name (cVenName),
    KEY idx_gl_Vender_company (company_id),
    KEY idx_gl_Vender_end_date (dEndDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';


CREATE TABLE IF NOT EXISTS gl_Customer (
    cCusCode VARCHAR(255) NOT NULL COMMENT '客户编码',
    cCusName VARCHAR(255) NOT NULL COMMENT '客户名称',
    cCusAbbName VARCHAR(255) NULL COMMENT '客户简称',
    cCCCode VARCHAR(255) NULL COMMENT '客户分类编码',
    cDCCode VARCHAR(255) NULL COMMENT '地区编码',
    cCusTradeCCode VARCHAR(255) NULL COMMENT '行业编码',
    cTrade VARCHAR(255) NULL COMMENT '所属行业',
    cCusAddress VARCHAR(255) NULL COMMENT '地址',
    cCusPostCode VARCHAR(255) NULL COMMENT '邮政编码',
    cCusRegCode VARCHAR(255) NULL COMMENT '纳税人登记号',
    cCusBank VARCHAR(255) NULL COMMENT '开户银行',
    cCusAccount VARCHAR(255) NULL COMMENT '银行账号',
    cCusLPerson VARCHAR(255) NULL COMMENT '法人',
    cCusPerson VARCHAR(255) NULL COMMENT '联系人',
    cCusHand VARCHAR(255) NULL COMMENT '手机',
    cCusCreGrade VARCHAR(255) NULL COMMENT '信用等级',
    iCusCreLine DECIMAL(18,2) NULL COMMENT '信用额度',
    iCusCreDate INT NULL COMMENT '信用期限',
    cCusOAddress VARCHAR(255) NULL COMMENT '发货地址',
    cCusOType VARCHAR(255) NULL COMMENT '发运方式',
    cCusHeadCode VARCHAR(255) NULL COMMENT '客户总公司编码',
    cCusWhCode VARCHAR(255) NULL COMMENT '发货仓库',
    cCusDepart VARCHAR(255) NULL COMMENT '分管部门',
    iARMoney DECIMAL(18,2) NULL COMMENT '应收余额',
    dLastDate DATETIME NULL COMMENT '最后交易日期',
    iLastMoney DECIMAL(18,2) NULL COMMENT '最后交易金额',
    dLRDate DATETIME NULL COMMENT '最后收款日期',
    iLRMoney DECIMAL(18,2) NULL COMMENT '最后收款金额',
    dEndDate DATETIME NULL COMMENT '停用日期',
    cCusBankCode VARCHAR(255) NULL COMMENT '所属银行编码',
    cCusDefine1 VARCHAR(255) NULL COMMENT '客户自定义项1',
    cCusDefine2 VARCHAR(255) NULL COMMENT '客户自定义项2',
    cCusDefine3 VARCHAR(255) NULL COMMENT '客户自定义项3',
    cCusDefine4 VARCHAR(255) NULL COMMENT '客户自定义项4',
    cCusDefine5 VARCHAR(255) NULL COMMENT '客户自定义项5',
    cCusDefine6 VARCHAR(255) NULL COMMENT '客户自定义项6',
    cCusDefine7 VARCHAR(255) NULL COMMENT '客户自定义项7',
    cCusDefine8 VARCHAR(255) NULL COMMENT '客户自定义项8',
    cCusDefine9 VARCHAR(255) NULL COMMENT '客户自定义项9',
    cCusDefine10 VARCHAR(255) NULL COMMENT '客户自定义项10',
    cCusDefine11 INT NULL COMMENT '客户自定义项11',
    cCusDefine12 INT NULL COMMENT '客户自定义项12',
    cCusDefine13 DECIMAL(18,2) NULL COMMENT '客户自定义项13',
    cCusDefine14 DECIMAL(18,2) NULL COMMENT '客户自定义项14',
    cCusDefine15 DATETIME NULL COMMENT '客户自定义项15',
    cCusDefine16 DATETIME NULL COMMENT '客户自定义项16',
    cInvoiceCompany VARCHAR(255) NULL COMMENT '开票单位',
    bCredit TINYINT DEFAULT 0 COMMENT '是否控制信用',
    bCreditDate TINYINT DEFAULT 0 COMMENT '是否控制信用期限',
    bCreditByHead TINYINT DEFAULT 0 COMMENT '是否按总公司控制信用',
    cMemo VARCHAR(255) NULL COMMENT '备注',
    fCommisionRate DECIMAL(18,2) NULL COMMENT '佣金比率(%)',
    fInsueRate DECIMAL(18,2) NULL COMMENT '保险费率(%)',
    CustomerKCode VARCHAR(255) NULL COMMENT '客户级别编码',
    bCusState TINYINT DEFAULT 0 COMMENT '是否成交',
    company_id VARCHAR(64) NULL COMMENT '公司主体编码',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (cCusCode),
    KEY idx_gl_Customer_name (cCusName),
    KEY idx_gl_Customer_company (company_id),
    KEY idx_gl_Customer_end_date (dEndDate),
    CONSTRAINT fk_gl_Customer_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户档案表';
CREATE TABLE IF NOT EXISTS pm_document_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'column comment',
    document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    template_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    template_name VARCHAR(100) NOT NULL COMMENT 'column comment',
    template_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    form_design_code VARCHAR(64) NULL COMMENT 'column comment',
    approval_flow_code VARCHAR(64) NULL COMMENT 'column comment',
    flow_name VARCHAR(100) NULL COMMENT 'column comment',
    submitter_user_id BIGINT NOT NULL COMMENT 'column comment',
    submitter_name VARCHAR(100) NOT NULL COMMENT 'column comment',
    document_title VARCHAR(200) NULL COMMENT 'column comment',
    document_reason VARCHAR(500) NULL COMMENT 'column comment',
    total_amount DECIMAL(18,2) NULL COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL' COMMENT 'column comment',
    current_node_key VARCHAR(64) NULL COMMENT 'column comment',
    current_node_name VARCHAR(100) NULL COMMENT 'column comment',
    current_task_type VARCHAR(32) NULL COMMENT 'column comment',
    form_data_json LONGTEXT NOT NULL COMMENT 'column comment',
    template_snapshot_json LONGTEXT NOT NULL COMMENT 'column comment',
    form_schema_snapshot_json LONGTEXT NOT NULL COMMENT 'column comment',
    flow_snapshot_json LONGTEXT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'column comment',
    finished_at DATETIME NULL COMMENT 'column comment',
    UNIQUE KEY uk_pm_document_instance_code (document_code),
    KEY idx_pm_document_instance_submitter (submitter_user_id, created_at),
    KEY idx_pm_document_instance_template (template_code),
    KEY idx_pm_document_instance_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_node_key VARCHAR(64) NULL COMMENT ''column comment'' AFTER status',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_node_key'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_node_name VARCHAR(100) NULL COMMENT ''column comment'' AFTER current_node_key',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_node_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_task_type VARCHAR(32) NULL COMMENT ''column comment'' AFTER current_node_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_task_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN finished_at DATETIME NULL COMMENT ''column comment'' AFTER updated_at',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'finished_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS pm_document_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'column comment',
    document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    node_key VARCHAR(64) NOT NULL COMMENT 'column comment',
    node_name VARCHAR(100) NULL COMMENT 'column comment',
    node_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    assignee_user_id BIGINT NOT NULL COMMENT 'column comment',
    assignee_name VARCHAR(100) NULL COMMENT 'column comment',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'column comment',
    task_batch_no VARCHAR(64) NOT NULL COMMENT 'column comment',
    approval_mode VARCHAR(32) NULL COMMENT 'column comment',
    action_comment VARCHAR(500) NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    handled_at DATETIME NULL COMMENT 'column comment',
    KEY idx_pm_document_task_assignee (assignee_user_id, status, created_at),
    KEY idx_pm_document_task_document (document_code, created_at),
    KEY idx_pm_document_task_node_batch (document_code, node_key, task_batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

CREATE TABLE IF NOT EXISTS pm_document_action_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'column comment',
    document_code VARCHAR(64) NOT NULL COMMENT 'column comment',
    node_key VARCHAR(64) NULL COMMENT 'column comment',
    node_name VARCHAR(100) NULL COMMENT 'column comment',
    action_type VARCHAR(32) NOT NULL COMMENT 'column comment',
    actor_user_id BIGINT NULL COMMENT 'column comment',
    actor_name VARCHAR(100) NULL COMMENT 'column comment',
    action_comment VARCHAR(500) NULL COMMENT 'column comment',
    payload_json LONGTEXT NULL COMMENT 'column comment',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'column comment',
    KEY idx_pm_document_action_log_document (document_code, created_at),
    KEY idx_pm_document_action_log_node (document_code, node_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='table comment';

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN node_type VARCHAR(32) NOT NULL DEFAULT ''APPROVAL'' COMMENT ''column comment'' AFTER node_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'node_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN assignee_user_id BIGINT NOT NULL DEFAULT 0 COMMENT ''column comment'' AFTER node_type',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'assignee_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN assignee_name VARCHAR(100) NULL COMMENT ''column comment'' AFTER assignee_user_id',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'assignee_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN task_batch_no VARCHAR(64) NOT NULL DEFAULT ''LEGACY'' COMMENT ''column comment'' AFTER status',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'task_batch_no'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN action_comment VARCHAR(500) NULL COMMENT ''column comment'' AFTER source_task_id',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'action_comment'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD KEY idx_pm_document_task_node_batch (document_code, node_key, task_batch_no)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND INDEX_NAME = 'idx_pm_document_task_node_batch'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_action_log ADD COLUMN action_comment VARCHAR(500) NULL COMMENT ''column comment'' AFTER actor_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_action_log'
      AND COLUMN_NAME = 'action_comment'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_action_log ADD COLUMN payload_json LONGTEXT NULL COMMENT ''column comment'' AFTER action_comment',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_action_log'
      AND COLUMN_NAME = 'payload_json'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_action_log ADD KEY idx_pm_document_action_log_node (document_code, node_key, created_at)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_action_log'
      AND INDEX_NAME = 'idx_pm_document_action_log_node'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN task_kind VARCHAR(32) NULL COMMENT ''column comment'' AFTER approval_mode',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'task_kind'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD COLUMN source_task_id BIGINT NULL COMMENT ''column comment'' AFTER task_kind',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND COLUMN_NAME = 'source_task_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_task ADD KEY idx_pm_document_task_source (source_task_id)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_task'
      AND INDEX_NAME = 'idx_pm_document_task_source'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
