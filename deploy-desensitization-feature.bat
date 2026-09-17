@echo off
chcp 65001 >nul
echo ========================================
echo 脱敏功能部署脚本
echo ========================================
echo.

echo [1/4] 检查数据库迁移状态...
mysql -u root -p -e "USE finex; SHOW TABLES LIKE 'ea_desensitization%%';" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [警告] 无法连接数据库，请手动检查
    goto SKIP_DB_CHECK
)

:SKIP_DB_CHECK

echo.
echo [2/4] 执行数据库迁移（如果尚未执行）...
set /p execute_migration="是否执行数据库迁移？(y/n): "
if /i "%execute_migration%"=="y" (
    echo 执行 migrate_desensitization.sql...
    mysql -u root -p finex < backend\sql\migrate_desensitization.sql
    if %ERRORLEVEL% EQU 0 (
        echo [成功] 数据库迁移完成
    ) else (
        echo [失败] 数据库迁移失败
    )
)

echo.
echo [3/4] 初始化权限配置...
set /p execute_permissions="是否执行权限初始化？(y/n): "
if /i "%execute_permissions%"=="y" (
    echo 执行 init_desensitization_permissions.sql...
    mysql -u root -p finex < backend\sql\init_desensitization_permissions.sql
    if %ERRORLEVEL% EQU 0 (
        echo [成功] 权限初始化完成
    ) else (
        echo [失败] 权限初始化失败
    )
)

echo.
echo [4/4] 前端配置已完成
echo   ✓ 路由配置: frontend\admin-web\src\router\route-catalog.ts
echo   ✓ 路由元信息: frontend\admin-web\src\router\route-meta.ts
echo   ✓ 导航菜单: frontend\admin-web\src\router\navigation-config.ts

echo.
echo ========================================
echo 部署完成！
echo ========================================
echo.
echo 下一步操作：
echo 1. 重启后端服务（确保权限配置生效）
echo 2. 重启前端服务（确保路由配置生效）
echo 3. 使用超级管理员账号登录系统
echo 4. 在 Agent 菜单中查看"脱敏规则配置"
echo.
echo 验证命令：
echo   mysql -u root -p finex -e "SELECT permission_code, permission_name FROM sys_permission WHERE permission_code LIKE 'agent:desensitization:%%';"
echo.
pause
