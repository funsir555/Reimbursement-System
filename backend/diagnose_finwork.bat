@echo off
echo ========================================
echo FinWork 系统异常诊断脚本
echo ========================================
echo.

echo [1/5] 检查后端服务是否运行...
netstat -ano | findstr ":8081" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ 后端服务运行中 ^(端口 8081^)
) else (
    echo ✗ 后端服务未运行
    echo 请先启动后端服务: cd backend/auth-service ^&^& mvn spring-boot:run
    goto :end
)
echo.

echo [2/5] 检查 Ollama 服务...
curl -s http://localhost:11434/api/tags >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Ollama 服务运行中
) else (
    echo ✗ Ollama 服务未运行
    echo 请启动 Ollama 或设置环境变量使用 DeepSeek
)
echo.

echo [3/5] 检查数据库表...
mysql -u root -p"" finex_db -e "SHOW TABLES LIKE 'ea_agent_%%';" 2>nul
if %errorlevel% equ 0 (
    echo ✓ 数据库表检查完成
) else (
    echo ✗ 数据库连接失败或表不存在
)
echo.

echo [4/5] 测试 API 连接...
echo 请提供 admin 的 JWT token 或跳过此步骤
echo.

echo [5/5] 检查环境变量...
if defined DEEPSEEK_API_KEY (
    echo ✓ DEEPSEEK_API_KEY 已设置
) else (
    echo ✗ DEEPSEEK_API_KEY 未设置
)

if defined LOCAL_MODEL_NAME (
    echo ✓ LOCAL_MODEL_NAME = %LOCAL_MODEL_NAME%
) else (
    echo ! LOCAL_MODEL_NAME 未设置 ^(将使用默认值 deepseek-r1:7b^)
)
echo.

echo ========================================
echo 诊断完成
echo ========================================
echo.
echo 下一步请做：
echo 1. 打开浏览器访问 FinWork
echo 2. 按 F12 打开开发者工具
echo 3. 切换到 Network 标签
echo 4. 点击"新建会话"
echo 5. 找到失败的请求，查看响应内容
echo 6. 将响应内容发给我
echo.

:end
pause
