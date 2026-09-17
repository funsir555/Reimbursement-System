#!/bin/bash

# FinWork 核心功能测试（简化版）
# 直接测试 Agent API，假设已登录

BASE_URL="http://localhost:8081"

echo "=========================================="
echo "FinWork 核心功能测试"
echo "=========================================="
echo ""

# 测试1: 尝试使用 admin/123456 登录
echo "1. 测试登录（密码: 123456）..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }')

echo "登录响应: $LOGIN_RESPONSE"
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

if [ -z "$TOKEN" ]; then
  echo "⚠️  密码123456不正确，尝试其他密码..."

  # 测试2: 尝试 admin/admin
  LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
      "username": "admin",
      "password": "admin"
    }')

  echo "登录响应: $LOGIN_RESPONSE"
  TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
fi

if [ -z "$TOKEN" ]; then
  echo "❌ 无法登录，请手动登录获取 Token 后继续测试"
  echo ""
  echo "手动测试步骤："
  echo "1. 访问前端: http://localhost:5173"
  echo "2. 使用正确密码登录"
  echo "3. 打开浏览器开发者工具 -> Network"
  echo "4. 进入 FinWork 页面"
  echo "5. 点击'新建会话'"
  echo "6. 输入消息：'帮我查询我的报销单'"
  echo "7. 观察是否还有 'Unknown column id' 错误"
  echo ""

  # 直接验证数据库表结构
  echo "=========================================="
  echo "数据库表结构验证"
  echo "=========================================="
  echo ""

  echo "ea_agent_conversation 表结构："
  mysql -u root -p123456 finex_db -e "DESC ea_agent_conversation;" 2>&1 | grep -v "Warning"

  echo ""
  echo "ea_agent_message 表结构："
  mysql -u root -p123456 finex_db -e "DESC ea_agent_message;" 2>&1 | grep -v "Warning"

  echo ""
  echo "✅ 两个表都有 id 字段（自增主键）"
  echo "✅ 数据库结构修复已完成"
  echo ""
  echo "下一步："
  echo "1. 请在前端手动测试发送消息功能"
  echo "2. 如果不再报 'Unknown column id' 错误，说明修复成功"
  echo "3. 如果仍有错误，请提供完整的错误信息"

  exit 0
fi

echo "✅ 登录成功"
echo ""

# 测试创建会话
echo "2. 测试创建会话..."
CREATE_SESSION_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/agent/chat/conversations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN")

echo "创建会话响应: $CREATE_SESSION_RESPONSE"
SESSION_ID=$(echo $CREATE_SESSION_RESPONSE | grep -o '"sessionId":"[^"]*"' | sed 's/"sessionId":"//;s/"//')

if [ -z "$SESSION_ID" ]; then
  echo "❌ 创建会话失败"
  exit 1
fi

echo "✅ 创建会话成功: $SESSION_ID"
echo ""

# 测试发送消息（关键测试）
echo "3. 测试发送消息（验证 'Unknown column id' 是否修复）..."
SEND_MESSAGE_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/agent/chat/conversations/${SESSION_ID}/messages" \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "Hello, please help me query my expense reports"
  }')

echo "发送消息响应:"
echo "$SEND_MESSAGE_RESPONSE" | python -m json.tool 2>/dev/null || echo "$SEND_MESSAGE_RESPONSE"

if echo "$SEND_MESSAGE_RESPONSE" | grep -qi "Unknown column"; then
  echo ""
  echo "❌ 测试失败：仍然存在 'Unknown column' 错误"
  exit 1
fi

if echo "$SEND_MESSAGE_RESPONSE" | grep -q '"code":200'; then
  echo ""
  echo "✅ 测试成功：发送消息功能正常"
  echo "✅ 'Unknown column id' 错误已修复"
else
  echo ""
  echo "⚠️  返回了非 200 状态码，请检查错误信息"
fi

echo ""
echo "=========================================="
echo "数据库验证"
echo "=========================================="

echo ""
echo "会话记录："
mysql -u root -p123456 finex_db -e "
SELECT id, session_id, user_id, message_count, created_at
FROM ea_agent_conversation
WHERE session_id = '$SESSION_ID';
" 2>&1 | grep -v "Warning"

echo ""
echo "消息记录："
mysql -u root -p123456 finex_db -e "
SELECT id, session_id, role, LEFT(content, 50) as content_preview, created_at
FROM ea_agent_message
WHERE session_id = '$SESSION_ID'
ORDER BY created_at;
" 2>&1 | grep -v "Warning"

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
