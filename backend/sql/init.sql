CREATE DATABASE IF NOT EXISTS finex_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(64) NOT NULL COMMENT '密码(MD5)',
    name VARCHAR(50) COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    dept_id BIGINT COMMENT '部门ID',
    position VARCHAR(50) COMMENT '岗位',
    labor_relation_belong VARCHAR(100) COMMENT '劳动关系所属',
    status TINYINT DEFAULT 1 COMMENT '状态:1正常 0禁用',
    wecom_user_id VARCHAR(100) COMMENT '企微用户ID',
    dingtalk_user_id VARCHAR(100) COMMENT '钉钉用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS labor_relation_belong VARCHAR(100) COMMENT '劳动关系所属' AFTER position;

CREATE TABLE IF NOT EXISTS sys_user_bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '银行账户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    bank_name VARCHAR(100) NOT NULL COMMENT '银行名称',
    branch_name VARCHAR(100) COMMENT '支行名称',
    account_name VARCHAR(100) NOT NULL COMMENT '账户名',
    account_no VARCHAR(50) NOT NULL COMMENT '银行卡号',
    account_type VARCHAR(50) DEFAULT '对私账户' COMMENT '账户类型',
    default_account TINYINT DEFAULT 0 COMMENT '是否默认账户',
    status TINYINT DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收款账户表';

CREATE TABLE IF NOT EXISTS sys_download_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '下载记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    business_type VARCHAR(100) NOT NULL COMMENT '业务类型',
    status VARCHAR(20) NOT NULL COMMENT '状态:DOWNLOADING/COMPLETED/FAILED',
    progress INT DEFAULT 0 COMMENT '下载进度',
    file_size VARCHAR(30) COMMENT '文件大小',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at DATETIME NULL COMMENT '完成时间',
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载记录表';

CREATE TABLE IF NOT EXISTS pm_template_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    category_description VARCHAR(255) COMMENT '分类说明',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板分类表';

CREATE TABLE IF NOT EXISTS pm_document_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(32) NOT NULL COMMENT '模板类型:report/application/loan',
    template_type_label VARCHAR(32) NOT NULL COMMENT '模板类型中文名',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    template_description VARCHAR(500) COMMENT '模板说明',
    numbering_rule VARCHAR(64) COMMENT '编号规则',
    icon_color VARCHAR(32) DEFAULT 'blue' COMMENT '主题色',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    publish_status VARCHAR(16) DEFAULT 'ENABLED' COMMENT '发布状态',
    print_mode VARCHAR(64) COMMENT '打印方式',
    approval_flow VARCHAR(64) COMMENT '审批流程编码',
    flow_name VARCHAR(100) COMMENT '审批流程名称',
    payment_mode VARCHAR(64) COMMENT '付款联动模式',
    split_payment TINYINT DEFAULT 0 COMMENT '是否支持分期付款',
    travel_form VARCHAR(64) COMMENT '行程表单',
    allocation_form VARCHAR(64) COMMENT '分摊表单',
    ai_audit_mode VARCHAR(64) DEFAULT 'disabled' COMMENT 'AI审核模式',
    relation_remark VARCHAR(500) COMMENT '关联规则说明',
    validation_remark VARCHAR(500) COMMENT '未税校验说明',
    installment_remark VARCHAR(500) COMMENT '分期说明',
    highlights VARCHAR(500) COMMENT '卡片亮点，使用|分隔',
    owner_name VARCHAR(64) COMMENT '维护人',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_code (template_code),
    KEY idx_category_code (category_code),
    KEY idx_template_type (template_type),
    KEY idx_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单据流程模板表';

CREATE TABLE IF NOT EXISTS pm_template_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '范围明细ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    option_type VARCHAR(32) NOT NULL COMMENT '明细类型:EXPENSE_TYPE/SCOPE_OPTION/TAG_OPTION',
    option_code VARCHAR(64) NOT NULL COMMENT '选项编码',
    option_label VARCHAR(64) NOT NULL COMMENT '选项名称',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_template_id (template_id),
    KEY idx_option_type (option_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板范围标签明细表';

INSERT IGNORE INTO sys_user (
    username, password, name, phone, email, position, labor_relation_belong, status
) VALUES
    ('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '13800138000', 'admin@finex.com', '系统管理员', '财务共享中心', 1),
    ('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13800138001', 'zhangsan@finex.com', '财务经理', '华东运营中心', 1),
    ('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13800138002', 'lisi@finex.com', '报销专员', '总部职能中心', 1);

UPDATE sys_user
SET labor_relation_belong = COALESCE(NULLIF(labor_relation_belong, ''), '总部')
WHERE labor_relation_belong IS NULL OR labor_relation_belong = '';

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '招商银行', '上海陆家嘴支行', u.name, '6225888888881001', '工资卡', 1, 1
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6225888888881001'
  );

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '建设银行', '上海张江支行', u.name, '6217000012345678', '报销卡', 1, 1
FROM sys_user u
WHERE u.username = 'zhangsan'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6217000012345678'
  );

INSERT INTO sys_user_bank_account (
    user_id, bank_name, branch_name, account_name, account_no, account_type, default_account, status
)
SELECT u.id, '工商银行', '上海徐汇支行', u.name, '6222000098765432', '报销卡', 1, 1
FROM sys_user u
WHERE u.username = 'lisi'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_bank_account a
      WHERE a.user_id = u.id AND a.account_no = '6222000098765432'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '3月报销单导出.xlsx', '报销明细导出', 'DOWNLOADING', 68, '4.6 MB', DATE_SUB(NOW(), INTERVAL 3 MINUTE), NULL
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '3月报销单导出.xlsx'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '待审批单据清单.xlsx', '审批清单导出', 'COMPLETED', 100, '2.1 MB', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 2 MINUTE
FROM sys_user u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '待审批单据清单.xlsx'
  );

INSERT INTO sys_download_record (
    user_id, file_name, business_type, status, progress, file_size, created_at, finished_at
)
SELECT u.id, '发票验真结果.csv', '发票管理导出', 'COMPLETED', 100, '860 KB', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 1 MINUTE
FROM sys_user u
WHERE u.username = 'zhangsan'
  AND NOT EXISTS (
      SELECT 1 FROM sys_download_record d
      WHERE d.user_id = u.id AND d.file_name = '发票验真结果.csv'
  );

INSERT INTO pm_template_category (category_code, category_name, category_description, sort_order, status)
VALUES
    ('enterprise-payment', '企业往来类', '适用于对公付款、备用金、押金和供应商结算等场景。', 10, 1),
    ('employee-expense', '员工费用类', '适用于员工报销、借支和团队费用归集。', 20, 1),
    ('business-application', '事项申请类', '适用于项目申请、付款触发和专项审批。', 30, 1)
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    category_description = VALUES(category_description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

INSERT INTO pm_document_template (
    template_code, template_name, template_type, template_type_label, category_code,
    template_description, numbering_rule, icon_color, enabled, publish_status, print_mode,
    approval_flow, flow_name, payment_mode, split_payment, travel_form, allocation_form,
    ai_audit_mode, relation_remark, validation_remark, installment_remark, highlights,
    owner_name, sort_order
)
VALUES
    (
        'PUB-EXP-01', '对公差旅付款', 'report', '报销单', 'enterprise-payment',
        '适用于供应商垫付差旅费用后的统一报销与付款流转。', 'year-sequence', 'blue', 1, 'ENABLED', 'default-print',
        'public-payment-flow', '对公付款流程', 'public-payment', 0, 'travel-standard', 'allocation-default',
        'standard', '可与付款单联动', '按发票未税金额校验', '不适用', '支持移动端提单|联动付款单|AI 审核',
        '流程中心', 10
    ),
    (
        'PUB-APP-02', '对公付款申请', 'application', '申请单', 'enterprise-payment',
        '用于采购预付款、服务付款和阶段尾款的审批。', 'department-month-sequence', 'cyan', 1, 'ENABLED', 'finance-archive',
        'public-payment-flow', '对公付款流程', 'public-payment', 0, 'travel-standard', 'allocation-project',
        'standard', '与合同付款关联', '合同金额与付款金额联校', '不适用', '支持移动端提单|联动付款单|标准审批链路',
        '财务共享', 20
    ),
    (
        'PUB-LOAN-03', '项目备用金借支', 'loan', '借款单', 'enterprise-payment',
        '适用于项目阶段性借支与后续核销归还。', 'year-sequence', 'orange', 1, 'ENABLED', 'default-print',
        'loan-return-flow', '借款与归还流程', 'private-payment', 1, 'travel-project', 'allocation-project',
        'standard', '归还时自动关联借款单', '借支金额不得超过项目预算', '支持按里程碑分期', '支持移动端提单|支持分期付款|AI 审核',
        '资金管理', 30
    ),
    (
        'PUB-EXP-04', '押金与保证金支付', 'report', '报销单', 'enterprise-payment',
        '适用于押金、保证金及合同履约类付款。', 'custom-prefix', 'blue', 0, 'DRAFT', 'finance-archive',
        'public-payment-flow', '押金审核流程', 'public-payment', 0, 'travel-standard', 'allocation-default',
        'disabled', '与合同台账联动', '按合同税率校验', '不适用', '支持移动端提单|联动付款单|标准审批链路',
        '法务协同', 40
    ),
    (
        'EMP-EXP-11', '标准员工报销', 'report', '报销单', 'employee-expense',
        '覆盖差旅、交通、住宿、办公等常见员工费用。', 'year-sequence', 'blue', 1, 'ENABLED', 'default-print',
        'normal-expense-flow', '标准报销流程', 'none', 0, 'travel-standard', 'allocation-default',
        'standard', '支持与申请单关联', '按票据未税金额校验', '不适用', '支持移动端提单|AI 审核|标准审批链路',
        '费用中心', 10
    ),
    (
        'EMP-APP-12', '差旅出差申请', 'application', '申请单', 'employee-expense',
        '用于出差前审批、预算占用和行程采集。', 'department-month-sequence', 'cyan', 1, 'ENABLED', 'landscape-summary',
        'normal-expense-flow', '出差审批流程', 'none', 0, 'travel-standard', 'allocation-default',
        'disabled', '出差申请可回写报销单', '不适用', '不适用', '支持移动端提单|标准审批链路|可关联报销',
        '人事行政', 20
    ),
    (
        'EMP-LOAN-13', '员工借款单', 'loan', '借款单', 'employee-expense',
        '适用于临时借支、差旅借款和备用金核销。', 'year-sequence', 'orange', 1, 'ENABLED', 'default-print',
        'loan-return-flow', '借款与归还流程', 'private-payment', 1, 'travel-standard', 'allocation-default',
        'standard', '归还时自动扣减借款余额', '借款余额不得为负', '支持按报销进度分期归还', '支持移动端提单|联动付款单|支持分期付款',
        '费用中心', 30
    ),
    (
        'EMP-EXP-14', '团队活动费用报销', 'report', '报销单', 'employee-expense',
        '用于团队活动、培训费用和会议支出归集。', 'custom-prefix', 'cyan', 0, 'DRAFT', 'landscape-summary',
        'normal-expense-flow', '团队报销流程', 'none', 0, 'travel-standard', 'allocation-department',
        'disabled', '可挂多个参与人员', '按人均限额进行校验', '不适用', '支持移动端提单|标准审批链路|部门分摊',
        '行政中心', 40
    ),
    (
        'BIZ-APP-21', '项目立项申请', 'application', '申请单', 'business-application',
        '用于项目立项、预算冻结和跨部门协同审批。', 'department-month-sequence', 'blue', 1, 'ENABLED', 'finance-archive',
        'normal-expense-flow', '项目立项流程', 'none', 0, 'travel-project', 'allocation-project',
        'strict', '可回写预算池', '按项目总预算校验', '不适用', '支持移动端提单|AI 审核|标准审批链路',
        '项目管理', 10
    ),
    (
        'BIZ-APP-22', '专项费用申请', 'application', '申请单', 'business-application',
        '用于营销活动、展会和专项预算申请。', 'custom-prefix', 'cyan', 1, 'ENABLED', 'landscape-summary',
        'normal-expense-flow', '专项预算流程', 'none', 0, 'travel-project', 'allocation-project',
        'standard', '与预算池关联', '按专项额度校验', '不适用', '支持移动端提单|AI 审核|可关联预算',
        '市场中心', 20
    ),
    (
        'BIZ-EXP-23', '合同付款报销', 'report', '报销单', 'business-application',
        '用于合同执行中的付款报销与台账记录。', 'year-sequence', 'blue', 1, 'ENABLED', 'finance-archive',
        'public-payment-flow', '合同付款流程', 'public-payment', 0, 'travel-standard', 'allocation-project',
        'standard', '与合同付款节点关联', '合同金额与发票金额联校', '不适用', '支持移动端提单|联动付款单|AI 审核',
        '合同管理', 30
    ),
    (
        'BIZ-APP-24', '采购付款申请', 'application', '申请单', 'business-application',
        '用于采购预付款、尾款及分阶段付款。', 'department-month-sequence', 'orange', 1, 'ENABLED', 'default-print',
        'public-payment-flow', '采购付款流程', 'public-payment', 1, 'travel-standard', 'allocation-default',
        'standard', '与采购单联动', '采购合同与发票金额联校', '支持分期付款节点配置', '支持移动端提单|联动付款单|支持分期付款',
        '采购协同', 40
    )
ON DUPLICATE KEY UPDATE
    template_name = VALUES(template_name),
    template_type = VALUES(template_type),
    template_type_label = VALUES(template_type_label),
    category_code = VALUES(category_code),
    template_description = VALUES(template_description),
    numbering_rule = VALUES(numbering_rule),
    icon_color = VALUES(icon_color),
    enabled = VALUES(enabled),
    publish_status = VALUES(publish_status),
    print_mode = VALUES(print_mode),
    approval_flow = VALUES(approval_flow),
    flow_name = VALUES(flow_name),
    payment_mode = VALUES(payment_mode),
    split_payment = VALUES(split_payment),
    travel_form = VALUES(travel_form),
    allocation_form = VALUES(allocation_form),
    ai_audit_mode = VALUES(ai_audit_mode),
    relation_remark = VALUES(relation_remark),
    validation_remark = VALUES(validation_remark),
    installment_remark = VALUES(installment_remark),
    highlights = VALUES(highlights),
    owner_name = VALUES(owner_name),
    sort_order = VALUES(sort_order);

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'EXPENSE_TYPE', 'travel', '差旅费', 10
FROM pm_document_template t
WHERE t.template_code = 'EMP-EXP-11'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'EXPENSE_TYPE' AND s.option_code = 'travel'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'TAG_OPTION', 'ai-audit', 'AI 审核', 20
FROM pm_document_template t
WHERE t.template_code = 'EMP-EXP-11'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'TAG_OPTION' AND s.option_code = 'ai-audit'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'TAG_OPTION', 'public-payment', '对公业务', 10
FROM pm_document_template t
WHERE t.template_code = 'PUB-APP-02'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'TAG_OPTION' AND s.option_code = 'public-payment'
  );

INSERT INTO pm_template_scope (template_id, option_type, option_code, option_label, sort_order)
SELECT t.id, 'SCOPE_OPTION', 'department', '限定部门使用', 10
FROM pm_document_template t
WHERE t.template_code = 'BIZ-APP-21'
  AND NOT EXISTS (
      SELECT 1 FROM pm_template_scope s
      WHERE s.template_id = t.id AND s.option_type = 'SCOPE_OPTION' AND s.option_code = 'department'
  );

-- 所有初始化账号的原始密码都是: 123456
