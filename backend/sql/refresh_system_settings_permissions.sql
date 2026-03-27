USE finex_db;

SET NAMES utf8mb4;

/*
用途:
1. 把权限目录刷新为和前端当前展示一致的中文结构
2. 兼容流程管理重构后的菜单层级
3. 确保 SUPER_ADMIN 拥有全部启用权限
4. 确保 admin 绑定 SUPER_ADMIN

说明:
- 流程管理页虽然内部重构为“单据与流程 / 自定义档案 / 费用类型”，
  当前前后端实际鉴权仍使用 expense:process_management:* 这一组权限码。
- 本脚本为幂等脚本，可重复执行。
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
    ('expense:documents:view', '单据查询', 'MENU', 'expense:menu', 'expense', '/expense/documents', 305, 1),
    ('expense:voucher_generation:view', '凭证生成', 'MENU', 'expense:menu', 'expense', '/expense/voucher-generation', 306, 1),
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
    ('finance:general_ledger:review_voucher:view', '审核凭证', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/review-voucher', 4013, 1),
    ('finance:general_ledger:review_voucher:review', '审核通过', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40131, 1),
    ('finance:general_ledger:review_voucher:unreview', '取消审核', 'BUTTON', 'finance:general_ledger:review_voucher:view', 'finance', NULL, 40132, 1),
    ('finance:general_ledger:balance_sheet:view', '余额表', 'MENU', 'finance:general_ledger:menu', 'finance', '/finance/general-ledger/balance-sheet', 4014, 1),
    ('finance:general_ledger:balance_sheet:export', '导出余额表', 'BUTTON', 'finance:general_ledger:balance_sheet:view', 'finance', NULL, 40141, 1),
    ('finance:fixed_assets:view', '固定资产', 'MENU', 'finance:menu', 'finance', '/finance/fixed-assets', 402, 1),
    ('finance:fixed_assets:create', '新增固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4021, 1),
    ('finance:fixed_assets:edit', '编辑固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4022, 1),
    ('finance:fixed_assets:delete', '删除固定资产', 'BUTTON', 'finance:fixed_assets:view', 'finance', NULL, 4023, 1),
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

INSERT INTO sys_role (role_code, role_name, role_description, status)
SELECT 'SUPER_ADMIN', '超级管理员', '拥有系统全部启用权限', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

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

SELECT permission_code, permission_name, permission_type, sort_order
FROM sys_permission
WHERE permission_code IN (
    'dashboard:menu',
    'profile:menu',
    'expense:menu',
    'expense:payment:menu',
    'expense:workbench:menu',
    'finance:menu',
    'finance:general_ledger:menu',
    'finance:reports:menu',
    'finance:archives:menu',
    'archives:menu',
    'settings:menu'
)
ORDER BY sort_order, permission_code;

DROP TEMPORARY TABLE IF EXISTS tmp_permission_seed;
