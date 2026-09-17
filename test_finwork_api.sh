#!/bin/bash

# FinWork API 测试脚本
# 测试完整流程：登录 → 创建会话 → 发送消息

BASE_URL="http://localhost:8081"
TOKEN=""

echo "=========================================="
echo "FinWork API 功能测试"
echo "=========================================="
echo ""

# 1. 登录获取 Token
echo "1. 测试登录..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo "登录响应: $LOGIN_RESPONSE"

# 提取 token（假设响应格式为 {code: 200, data: {token: "xxx"}}）
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，无法获取 Token"
  echo "请检查用户名密码是否正确"
  exit 1
fi

echo "✅ 登录成功，Token: ${TOKEN:0:20}..."
echo ""

# 2. 创建会话
echo "2. 测试创建会话..."
CREATE_SESSION_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/agent/chat/conversations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{}')

echo "创建会话响应: $CREATE_SESSION_RESPONSE"

# 提取 sessionId
SESSION_ID=$(echo $CREATE_SESSION_RESPONSE | grep -o '"sessionId":"[^"]*"' | sed 's/"sessionId":"//;s/"//')

if [ -z "$SESSION_ID" ]; then
  echo "❌ 创建会话失败"
  exit 1
fi

echo "✅ 创建会话成功，SessionId: $SESSION_ID"
echo ""

# 3. 发送消息（关键测试点：验证是否还有 "Unknown column 'id'" 错误）
echo "3. 测试发送消息..."
SEND_MESSAGE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/agent/chat/conversations/${SESSION_ID}/messages" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "你好，帮我查询我的报销单"
  }')

echo "发送消息响应: $SEND_MESSAGE_RESPONSE"

# 检查响应中是否包含错误
if echo "$SEND_MESSAGE_RESPONSE" | grep -q "Unknown column"; then
  echo "❌ 发送消息失败：仍然存在数据库字段错误"
  exit 1
fi

if echo "$SEND_MESSAGE_RESPONSE" | grep -q '"code":200'; then
  echo "✅ 发送消息成功"
else
  echo "⚠️  发送消息返回非 200 状态码"
fi

echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "检查数据库中的数据："
mysql -u root -p123456 finex_db -e "
SELECT
  c.id as conv_id,
  c.session_id,
  c.message_count,
  COUNT(m.id) as actual_msg_count
FROM ea_agent_conversation c
LEFT JOIN ea_agent_message m ON c.session_id = m.session_id
WHERE c.session_id = '$SESSION_ID'
GROUP BY c.id, c.session_id, c.message_count;
" 2>&1 | grep -v "Warning"

echo ""
echo "最近的消息："
mysql -u root -p123456 finex_db -e "
SELECT id, session_id, role, LEFT(content, 50) as content_preview
FROM ea_agent_message
WHERE session_id = '$SESSION_ID'
ORDER BY created_at DESC;
" 2>&1 | grep -v "Warning"
