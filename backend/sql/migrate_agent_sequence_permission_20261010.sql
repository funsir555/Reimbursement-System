SET NAMES utf8mb4;

-- Run after migrate_finwork_phase1_query_20261002.sql. The sequence tool reuses
-- the ledger view permission, but some existing databases lack its permission
-- directory rows. This repair is safe to run more than once.
-- Before applying, confirm that finance:general_ledger:menu exists and is active.

INSERT IGNORE INTO sys_permission
  (permission_code, permission_name, permission_type, parent_id,
   module_code, route_path, sort_order, status)
SELECT 'finance:general_ledger:sequence_ledger:view', '序时账', 'MENU', parent.id,
       'finance', '/finance/general-ledger/sequence-ledger', 4022, 1
FROM sys_permission parent
WHERE parent.permission_code='finance:general_ledger:menu' AND parent.status=1;

INSERT IGNORE INTO sys_permission
  (permission_code, permission_name, permission_type, parent_id,
   module_code, route_path, sort_order, status)
SELECT 'finance:general_ledger:sequence_ledger:export', '导出序时账', 'BUTTON', parent.id,
       'finance', NULL, 40221, 1
FROM sys_permission parent
WHERE parent.permission_code='finance:general_ledger:sequence_ledger:view' AND parent.status=1;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission ON permission.status=1
WHERE UPPER(role.role_code)='SUPER_ADMIN' AND role.status=1
  AND permission.permission_code IN (
    'finance:general_ledger:sequence_ledger:view',
    'finance:general_ledger:sequence_ledger:export');

-- Expected: two active permissions with a SUPER_ADMIN grant. Existing disabled
-- permissions are deliberately not re-enabled; investigate them separately.
SELECT permission.permission_code, permission.status,
       parent.permission_code AS parent_code,
       COUNT(DISTINCT CASE WHEN role.role_code='SUPER_ADMIN' THEN role.id END)
         AS super_admin_grants
FROM sys_permission permission
LEFT JOIN sys_permission parent ON parent.id=permission.parent_id
LEFT JOIN sys_role_permission grant_row ON grant_row.permission_id=permission.id
LEFT JOIN sys_role role ON role.id=grant_row.role_id AND role.status=1
WHERE permission.permission_code IN (
  'finance:general_ledger:sequence_ledger:view',
  'finance:general_ledger:sequence_ledger:export')
GROUP BY permission.permission_code, permission.status, parent.permission_code
ORDER BY permission.permission_code;

-- Rollback: disable the sequence tool/page before changing grants. Do not delete
-- permission or role records that may already be referenced by role settings.
