#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 运行此脚本"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
APP_HOME="${APP_HOME:-/opt/finex}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/conf.d/finex.conf}"

install -m 0644 "$ROOT_DIR/deploy/linux/systemd/finex-auth.service" /etc/systemd/system/finex-auth.service
install -m 0644 "$ROOT_DIR/deploy/linux/systemd/finex-gateway.service" /etc/systemd/system/finex-gateway.service
install -m 0644 "$ROOT_DIR/deploy/linux/nginx/finex.conf" "$NGINX_SITE"

mkdir -p "$APP_HOME/logs"
chown -R finex:finex "$APP_HOME"

nginx -t
systemctl daemon-reload
systemctl enable finex-auth.service finex-gateway.service nginx
systemctl restart finex-auth.service
systemctl restart finex-gateway.service
systemctl restart nginx

echo "运行环境已安装完成，请继续："
echo "1. 修改 $NGINX_SITE 中的域名"
echo "2. 为域名签发 HTTPS 证书"
echo "3. 在企微后台填写可信域名和企业可信 IP"
