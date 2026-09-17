@echo off
chcp 65001 >nul
echo ========================================
echo FinWork API 完整测试脚本
echo ========================================
echo.

echo [步骤 1/3] 登录获取 Token...
curl -s -X POST http://localhost:8081/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}" ^
  > login_result.json

echo 登录响应已保存到 login_result.json
type login_result.json
echo.
echo.

echo [步骤 2/3] 请从上面的响应中复制 token 值（不含引号）
set /p TOKEN="请粘贴 Token: "
echo.

echo [步骤 3/3] 测试创建会话...
curl -v -X POST http://localhost:8081/auth/agent/chat/conversations ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json"
echo.
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo 如果看到 "会话创建成功" 或返回 sessionId，说明后端正常
echo 如果仍然报错，请将错误信息发给我
echo.
pause
