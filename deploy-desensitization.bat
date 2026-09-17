@echo off
chcp 65001 >nul
echo ========================================
echo 智能脱敏服务一键部署脚本
echo ========================================
echo.

echo [步骤 1/4] 检查数据库连接...
mysql -u root -p -e "SELECT 1" 2>nul
if errorlevel 1 (
    echo [错误] 无法连接到 MySQL，请检查数据库是否启动
    echo 请手动运行: mysql -u root -p finex ^< sql\migrate_desensitization.sql
    pause
    exit /b 1
)
echo [完成] 数据库连接正常

echo.
echo [步骤 2/4] 执行数据库迁移...
cd backend
mysql -u root -p finex < sql\migrate_desensitization.sql
if errorlevel 1 (
    echo [错误] 数据库迁移失败
    pause
    exit /b 1
)
echo [完成] 数据库表创建成功

echo.
echo [步骤 3/4] 编译项目...
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo [错误] 项目编译失败
    pause
    exit /b 1
)
echo [完成] 项目编译成功

echo.
echo [步骤 4/4] 启动服务...
echo.
echo ========================================
echo 服务启动中，请稍候...
echo ========================================
echo.
echo 后端服务: http://localhost:8081
echo 前端服务: http://localhost:5173
echo 脱敏配置: http://localhost:5173/agent/desensitization
echo.
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

cd auth-service
call mvn spring-boot:run

pause
