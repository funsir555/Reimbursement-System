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
    ADD COLUMN IF NOT EXISTS receipt_account_name VARCHAR(128) NULL COMMENT '收款开户名' AFTER cVenBankNub;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_bank_province VARCHAR(64) NULL COMMENT '收款开户省' AFTER receipt_account_name;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_bank_city VARCHAR(64) NULL COMMENT '收款开户市' AFTER receipt_bank_province;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_branch_code VARCHAR(64) NULL COMMENT '收款分支行编码' AFTER receipt_bank_city;

ALTER TABLE gl_Vender
    ADD COLUMN IF NOT EXISTS receipt_branch_name VARCHAR(128) NULL COMMENT '收款分支行名称' AFTER receipt_branch_code;

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

-- comment standardization begin
ALTER TABLE gl_vender
    MODIFY COLUMN cVenCode varchar(64) NOT NULL COMMENT '供应商编码',
    MODIFY COLUMN cVenName varchar(128) NOT NULL COMMENT '供应商名称',
    MODIFY COLUMN cVenAbbName varchar(64) NULL COMMENT '供应商简称',
    MODIFY COLUMN cVCCode varchar(64) NULL COMMENT '供应商分类编码',
    MODIFY COLUMN cTrade varchar(255) NULL COMMENT '所属行业',
    MODIFY COLUMN cVenAddress varchar(255) NULL COMMENT '地址',
    MODIFY COLUMN cVenRegCode varchar(255) NULL COMMENT '纳税人登记号',
    MODIFY COLUMN cVenBank varchar(128) NULL COMMENT '开户银行',
    MODIFY COLUMN cVenAccount varchar(64) NULL COMMENT '银行账号',
    MODIFY COLUMN cVenBankNub varchar(64) NULL COMMENT '银行行号',
    MODIFY COLUMN receipt_account_name varchar(128) NULL COMMENT '收款开户名',
    MODIFY COLUMN receipt_bank_province varchar(64) NULL COMMENT '收款开户省',
    MODIFY COLUMN receipt_bank_city varchar(64) NULL COMMENT '收款开户市',
    MODIFY COLUMN receipt_branch_code varchar(64) NULL COMMENT '收款分支行编码',
    MODIFY COLUMN receipt_branch_name varchar(128) NULL COMMENT '收款分支行名称',
    MODIFY COLUMN cVenPerson varchar(64) NULL COMMENT '联系人',
    MODIFY COLUMN cVenPhone varchar(32) NULL COMMENT '电话',
    MODIFY COLUMN cVenHand varchar(32) NULL COMMENT '手机',
    MODIFY COLUMN cVenEmail varchar(255) NULL COMMENT 'Email地址',
    MODIFY COLUMN company_id varchar(64) NULL COMMENT '公司主体编码',
    MODIFY COLUMN cMemo varchar(255) NULL COMMENT '备注',
    MODIFY COLUMN dEndDate datetime NULL COMMENT '停用日期',
    MODIFY COLUMN bBusinessDate tinyint NULL DEFAULT 0 COMMENT '经营许可证是否期限管理',
    MODIFY COLUMN bLicenceDate tinyint NULL DEFAULT 0 COMMENT '营业执照是否期限管理',
    MODIFY COLUMN bPassGMP tinyint NULL DEFAULT 0 COMMENT '是否通过GMP认证',
    MODIFY COLUMN bProxyDate tinyint NULL DEFAULT 0 COMMENT '法人委托书是否期限管理',
    MODIFY COLUMN bProxyForeign tinyint NULL DEFAULT 0 COMMENT '是否委外',
    MODIFY COLUMN bVenCargo tinyint NULL DEFAULT 0 COMMENT '是否货物',
    MODIFY COLUMN bVenService tinyint NULL DEFAULT 0 COMMENT '是否服务',
    MODIFY COLUMN bVenTax tinyint NULL DEFAULT 0 COMMENT '单价是否含税',
    MODIFY COLUMN cBarCode varchar(255) NULL COMMENT '对应条形码',
    MODIFY COLUMN cCreatePerson varchar(64) NULL COMMENT '建档人',
    MODIFY COLUMN cDCCode varchar(64) NULL COMMENT '地区编码',
    MODIFY COLUMN cModifyPerson varchar(64) NULL COMMENT '变更人',
    MODIFY COLUMN cRelCustomer varchar(64) NULL COMMENT '对应客户',
    MODIFY COLUMN cVenBankCode varchar(64) NULL COMMENT '所属银行编码',
    MODIFY COLUMN cVenBP varchar(32) NULL COMMENT '呼机',
    MODIFY COLUMN cVenDefine10 varchar(255) NULL COMMENT '供应商自定义项10',
    MODIFY COLUMN cVenDefine11 int NULL COMMENT '供应商自定义项11',
    MODIFY COLUMN cVenDefine12 int NULL COMMENT '供应商自定义项12',
    MODIFY COLUMN cVenDefine13 decimal(18,2) NULL COMMENT '供应商自定义项13',
    MODIFY COLUMN cVenDefine14 decimal(18,2) NULL COMMENT '供应商自定义项14',
    MODIFY COLUMN cVenDefine15 datetime NULL COMMENT '供应商自定义项15',
    MODIFY COLUMN cVenDefine16 datetime NULL COMMENT '供应商自定义项16',
    MODIFY COLUMN cVenDefine3 varchar(255) NULL COMMENT '供应商自定义项3',
    MODIFY COLUMN cVenDefine4 varchar(255) NULL COMMENT '供应商自定义项4',
    MODIFY COLUMN cVenDefine5 varchar(255) NULL COMMENT '供应商自定义项5',
    MODIFY COLUMN cVenDefine6 varchar(255) NULL COMMENT '供应商自定义项6',
    MODIFY COLUMN cVenDefine7 varchar(255) NULL COMMENT '供应商自定义项7',
    MODIFY COLUMN cVenDefine8 varchar(255) NULL COMMENT '供应商自定义项8',
    MODIFY COLUMN cVenDefine9 varchar(255) NULL COMMENT '供应商自定义项9',
    MODIFY COLUMN cVenDepart varchar(255) NULL COMMENT '分管部门',
    MODIFY COLUMN cVenFax varchar(32) NULL COMMENT '传真',
    MODIFY COLUMN cVenHeadCode varchar(64) NULL COMMENT '供应商总公司编码',
    MODIFY COLUMN cVenIAddress varchar(255) NULL COMMENT '到货地址',
    MODIFY COLUMN cVenIType varchar(255) NULL COMMENT '到货方式',
    MODIFY COLUMN cVenLPerson varchar(64) NULL COMMENT '法人',
    MODIFY COLUMN cVenPayCond varchar(64) NULL COMMENT '付款条件编码',
    MODIFY COLUMN cVenPostCode varchar(16) NULL COMMENT '邮政编码',
    MODIFY COLUMN cVenPPerson varchar(64) NULL COMMENT '专营业务员',
    MODIFY COLUMN cVenTradeCCode varchar(64) NULL COMMENT '行业编码',
    MODIFY COLUMN cVenWhCode varchar(64) NULL COMMENT '到货仓库',
    MODIFY COLUMN dBusinessEDate datetime NULL COMMENT '经营许可证到期日期',
    MODIFY COLUMN dBusinessSDate datetime NULL COMMENT '经营许可证生效日期',
    MODIFY COLUMN dLastDate datetime NULL COMMENT '最后交易日期',
    MODIFY COLUMN dLicenceEDate datetime NULL COMMENT '营业执照到期日期',
    MODIFY COLUMN dLicenceSDate datetime NULL COMMENT '营业执照生效日期',
    MODIFY COLUMN dLRDate datetime NULL COMMENT '最后付款日期',
    MODIFY COLUMN dModifyDate datetime NULL COMMENT '变更日期',
    MODIFY COLUMN dProxyEDate datetime NULL COMMENT '法人委托书到期日期',
    MODIFY COLUMN dProxySDate datetime NULL COMMENT '法人委托书生效日期',
    MODIFY COLUMN dVenDevDate datetime NULL COMMENT '发展日期',
    MODIFY COLUMN fRegistFund decimal(18,2) NULL COMMENT '注册资金',
    MODIFY COLUMN iAPMoney decimal(18,2) NULL COMMENT '应付余额',
    MODIFY COLUMN iBusinessADays int NULL COMMENT '经营许可证预警天数',
    MODIFY COLUMN iEmployeeNum int NULL COMMENT '员工人数',
    MODIFY COLUMN iFrequency int NULL COMMENT '使用频度',
    MODIFY COLUMN iGradeABC smallint NULL COMMENT 'ABC等级',
    MODIFY COLUMN iId int NULL COMMENT '所属权限组',
    MODIFY COLUMN iLastMoney decimal(18,2) NULL COMMENT '最后交易金额',
    MODIFY COLUMN iLicenceADays int NULL COMMENT '营业执照预警天数',
    MODIFY COLUMN iLRMoney decimal(18,2) NULL COMMENT '最后付款金额',
    MODIFY COLUMN iProxyADays int NULL COMMENT '法人委托书预警天数',
    MODIFY COLUMN iVenCreDate int NULL COMMENT '信用期限',
    MODIFY COLUMN iVenCreGrade varchar(255) NULL COMMENT '信用等级',
    MODIFY COLUMN iVenCreLine decimal(18,2) NULL COMMENT '信用额度',
    MODIFY COLUMN iVenDisRate decimal(18,2) NULL COMMENT '扣率',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '供应商档案表';

ALTER TABLE sys_bank_catalog
    MODIFY COLUMN bank_code varchar(64) NOT NULL COMMENT '银行编码',
    MODIFY COLUMN bank_name varchar(200) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '国内银行目录表';

ALTER TABLE sys_bank_branch_catalog
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN bank_code varchar(64) NOT NULL COMMENT '银行编码',
    MODIFY COLUMN bank_name varchar(200) NOT NULL COMMENT '银行名称',
    MODIFY COLUMN province varchar(64) NOT NULL COMMENT '开户省',
    MODIFY COLUMN city varchar(64) NOT NULL COMMENT '开户市',
    MODIFY COLUMN branch_code varchar(64) NOT NULL COMMENT '分支行编码',
    MODIFY COLUMN branch_name varchar(200) NOT NULL COMMENT '分支行名称',
    MODIFY COLUMN cnaps_code varchar(64) NULL COMMENT '联行号',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '国内银行联行目录表';

-- comment standardization end
