USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_expense_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '费用类型ID',
    parent_id BIGINT NULL COMMENT '上级费用类型ID',
    expense_code VARCHAR(8) NOT NULL COMMENT '完整费用类型编码',
    expense_name VARCHAR(100) NOT NULL COMMENT '费用类型名称',
    expense_description VARCHAR(255) NULL COMMENT '费用类型说明',
    code_level TINYINT NOT NULL COMMENT '编码层级:1一级 2二级',
    code_prefix VARCHAR(4) NOT NULL COMMENT '编码前四位归组标识',
    scope_dept_ids JSON NULL COMMENT '限定部门ID数组',
    scope_user_ids JSON NULL COMMENT '限定人员ID数组',
    invoice_free_mode VARCHAR(32) NOT NULL DEFAULT 'NOT_FREE' COMMENT '是否免票配置',
    tax_deduction_mode VARCHAR(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '税额抵扣与转出配置',
    tax_separation_mode VARCHAR(32) NOT NULL DEFAULT 'SEPARATE' COMMENT '价税分离规则',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_pm_expense_type_code UNIQUE (expense_code),
    KEY idx_pm_expense_type_parent_id (parent_id),
    KEY idx_pm_expense_type_status (status),
    KEY idx_pm_expense_type_prefix (code_prefix),
    CONSTRAINT fk_pm_expense_type_parent_id FOREIGN KEY (parent_id) REFERENCES pm_expense_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程管理费用类型树';

INSERT INTO pm_expense_type (
    parent_id,
    expense_code,
    expense_name,
    expense_description,
    code_level,
    code_prefix,
    scope_dept_ids,
    scope_user_ids,
    invoice_free_mode,
    tax_deduction_mode,
    tax_separation_mode,
    status
)
VALUES
    (
        NULL,
        '660100',
        '差旅费',
        '一级费用类型示例',
        1,
        '6601',
        JSON_ARRAY(),
        JSON_ARRAY(),
        'NOT_FREE',
        'DEFAULT',
        'SEPARATE',
        1
    ),
    (
        NULL,
        '660200',
        '福利费',
        '一级费用类型示例',
        1,
        '6602',
        JSON_ARRAY(),
        JSON_ARRAY(),
        'FREE',
        'SPECIAL_NO_DEDUCT_NEED_OUT',
        'NOT_SEPARATE',
        1
    )
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    expense_name = VALUES(expense_name),
    expense_description = VALUES(expense_description),
    code_level = VALUES(code_level),
    code_prefix = VALUES(code_prefix),
    scope_dept_ids = VALUES(scope_dept_ids),
    scope_user_ids = VALUES(scope_user_ids),
    invoice_free_mode = VALUES(invoice_free_mode),
    tax_deduction_mode = VALUES(tax_deduction_mode),
    tax_separation_mode = VALUES(tax_separation_mode),
    status = VALUES(status);

INSERT INTO pm_expense_type (
    parent_id,
    expense_code,
    expense_name,
    expense_description,
    code_level,
    code_prefix,
    scope_dept_ids,
    scope_user_ids,
    invoice_free_mode,
    tax_deduction_mode,
    tax_separation_mode,
    status
)
SELECT
    parent.id,
    '66010001',
    '国内机票',
    '二级费用类型示例，自动挂接到 660100',
    2,
    '6601',
    JSON_ARRAY(),
    JSON_ARRAY(),
    'NOT_FREE',
    'HAS_DEDUCT_NO_DEDUCT_NEED_OUT',
    'SEPARATE',
    1
FROM pm_expense_type parent
WHERE parent.expense_code = '660100'
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    expense_name = VALUES(expense_name),
    expense_description = VALUES(expense_description),
    code_level = VALUES(code_level),
    code_prefix = VALUES(code_prefix),
    scope_dept_ids = VALUES(scope_dept_ids),
    scope_user_ids = VALUES(scope_user_ids),
    invoice_free_mode = VALUES(invoice_free_mode),
    tax_deduction_mode = VALUES(tax_deduction_mode),
    tax_separation_mode = VALUES(tax_separation_mode),
    status = VALUES(status);
