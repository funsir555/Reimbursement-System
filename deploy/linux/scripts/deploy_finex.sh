#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
APP_HOME="${APP_HOME:-/opt/finex}"
BACKEND_ENV_SOURCE="${BACKEND_ENV_SOURCE:-$ROOT_DIR/backend/.env.production}"
FRONTEND_ENV_SOURCE="${FRONTEND_ENV_SOURCE:-$ROOT_DIR/frontend/admin-web/.env.production}"

copy_latest_jar() {
  local source_dir="$1"
  local target_file="$2"
  local jar_file
  jar_file="$(find "$source_dir" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
  if [[ -z "$jar_file" ]]; then
    echo "未找到可部署的 jar：$source_dir" >&2
    exit 1
  fi
  install -m 0644 "$jar_file" "$target_file"
}

if [[ ! -f "$BACKEND_ENV_SOURCE" ]]; then
  echo "缺少后端生产环境文件：$BACKEND_ENV_SOURCE" >&2
  echo "请先基于 backend/.env.production.example 生成 backend/.env.production" >&2
  exit 1
fi

if [[ ! -f "$FRONTEND_ENV_SOURCE" ]]; then
  echo "缺少前端生产环境文件：$FRONTEND_ENV_SOURCE" >&2
  echo "请先基于 frontend/admin-web/.env.production.example 生成 frontend/admin-web/.env.production" >&2
  exit 1
fi

mkdir -p "$APP_HOME/backend" "$APP_HOME/www/admin" "$APP_HOME/logs"

cd "$ROOT_DIR/backend"
mvn -q -DskipTests clean package

cd "$ROOT_DIR/frontend/admin-web"
npm ci
npm run build

copy_latest_jar "$ROOT_DIR/backend/auth-service/target" "$APP_HOME/backend/auth-service.jar"
copy_latest_jar "$ROOT_DIR/backend/gateway/target" "$APP_HOME/backend/gateway.jar"
install -m 0644 "$BACKEND_ENV_SOURCE" "$APP_HOME/backend/.env.production"

rm -rf "$APP_HOME/www/admin"
mkdir -p "$APP_HOME/www/admin"
cp -R "$ROOT_DIR/frontend/admin-web/dist/." "$APP_HOME/www/admin/"

echo "构建与发布文件已就位："
echo "  后端: $APP_HOME/backend/auth-service.jar"
echo "  网关: $APP_HOME/backend/gateway.jar"
echo "  前端: $APP_HOME/www/admin"
