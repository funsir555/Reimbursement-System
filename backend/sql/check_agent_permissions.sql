SELECT permission_code, permission_name, permission_type, sort_order, status
FROM sys_permission 
WHERE permission_code LIKE 'agent%' OR permission_code = 'agents:menu' OR permission_code = 'agents:view'
ORDER BY permission_code;
