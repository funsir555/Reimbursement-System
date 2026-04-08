USE finex_db;

SET NAMES utf8mb4;

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

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_account_name VARCHAR(255) NULL COMMENT '收款开户名' AFTER cVenBankNub;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_bank_province VARCHAR(64) NULL COMMENT '收款开户省' AFTER receipt_account_name;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_bank_city VARCHAR(64) NULL COMMENT '收款开户市' AFTER receipt_bank_province;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_branch_code VARCHAR(64) NULL COMMENT '收款分支行编码' AFTER receipt_bank_city;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_branch_name VARCHAR(255) NULL COMMENT '收款分支行名称' AFTER receipt_branch_code;

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
