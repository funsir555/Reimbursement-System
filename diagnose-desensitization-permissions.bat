@echo off
chcp 65001 >nul
echo ========================================
echo 脱敏功能权限诊断脚本
echo ========================================
echo.

echo [检查1] 检查权限是否存在...
mysql -u root -p123456 finex_db -e "SELECT permission_code, permission_type, sort_order FROM sys_permission WHERE permission_code LIKE 'agent:desensitization:%%' ORDER BY sort_order;"
echo.

echo [检查2] 检查超级管理员是否拥有权限...
mysql -u root -p123456 finex_db -e "SELECT p.permission_code FROM sys_role r JOIN sys_role_permission rp ON r.id = rp.role_id JOIN sys_permission p ON rp.permission_id = p.id WHERE r.role_code = 'super_admin' AND p.permission_code LIKE 'agent:desensitization:%%';"
echo.

echo [检查3] 检查你的用户角色...
set /p username="请输入你的登录用户名: "
mysql -u root -p123456 finex_db -e "SELECT u.username, r.role_code, r.role_name FROM sys_user u JOIN sys_user_role ur ON u.id = ur.user_id JOIN sys_role r ON ur.role_id = r.id WHERE u.username = '%username%';"
echo.

echo [检查4] 检查你的用户是否有脱敏权限...
mysql -u root -p123456 finex_db -e "SELECT DISTINCT p.permission_code FROM sys_user u JOIN sys_user_role ur ON u.id = ur.user_id JOIN sys_role_permission rp ON ur.role_id = rp.role_id JOIN sys_permission p ON rp.permission_id = p.id WHERE u.username = '%username%' AND p.permission_code LIKE 'agent:desensitization:%%';"
echo.

echo [检查5] 检查Agent菜单权限...
mysql -u root -p123456 finex_db -e "SELECT permission_code, permission_name FROM sys_permission WHERE permission_code = 'agent:menu';"
echo.

echo ========================================
echo 诊断完成
echo ========================================
echo.
echo 如果上面显示：
echo - 检查1: 应该有4个权限
echo - 检查2: 应该显示4个权限码
echo - 检查3: 应该显示你的角色
echo - 检查4: 应该显示脱敏相关权限
echo - 检查5: 应该显示 agent:menu
echo.
pause
