USE finex_db;

SET NAMES utf8mb4;

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
    parent.id,
    seed.module_code,
    seed.route_path,
    seed.sort_order,
    1
FROM (
    SELECT
        'finance:general_ledger:post_voucher:run' AS permission_code,
        '开始记账' AS permission_name,
        'BUTTON' AS permission_type,
        'finance:general_ledger:post_voucher:view' AS parent_code,
        'finance' AS module_code,
        NULL AS route_path,
        401351 AS sort_order
    UNION ALL
    SELECT
        'finance:general_ledger:post_voucher:task:view' AS permission_code,
        '查看记账任务' AS permission_name,
        'BUTTON' AS permission_type,
        'finance:general_ledger:post_voucher:view' AS parent_code,
        'finance' AS module_code,
        NULL AS route_path,
        401352 AS sort_order
) seed
JOIN sys_permission parent
    ON parent.permission_code = seed.parent_code
LEFT JOIN sys_permission permission
    ON permission.permission_code = seed.permission_code
WHERE permission.id IS NULL;

UPDATE sys_permission permission
JOIN (
    SELECT
        'finance:general_ledger:post_voucher:run' AS permission_code,
        '开始记账' AS permission_name,
        'BUTTON' AS permission_type,
        'finance' AS module_code,
        401351 AS sort_order
    UNION ALL
    SELECT
        'finance:general_ledger:post_voucher:task:view' AS permission_code,
        '查看记账任务' AS permission_name,
        'BUTTON' AS permission_type,
        'finance' AS module_code,
        401352 AS sort_order
) seed
    ON seed.permission_code = permission.permission_code
SET permission.permission_name = seed.permission_name,
    permission.permission_type = seed.permission_type,
    permission.module_code = seed.module_code,
    permission.sort_order = seed.sort_order,
    permission.status = 1;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
    ON permission.permission_code IN (
        'finance:general_ledger:post_voucher:run',
        'finance:general_ledger:post_voucher:task:view'
    )
WHERE role.role_code = 'SUPER_ADMIN';
