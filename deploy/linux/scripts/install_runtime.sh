#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run this script as root."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
APP_HOME="${APP_HOME:-/opt/finex}"
FINEX_DOMAIN="${FINEX_DOMAIN:-admin.finexgz.xyz}"
NGINX_SITE="${NGINX_SITE:-/etc/nginx/conf.d/finex.conf}"
NGINX_TEMPLATE="$ROOT_DIR/deploy/linux/nginx/finex.conf"

render_nginx_site() {
  sed \
    -e "s|__APP_HOME__|$APP_HOME|g" \
    -e "s|__SERVER_NAME__|$FINEX_DOMAIN|g" \
    "$NGINX_TEMPLATE" > "$NGINX_SITE"
}

assert_exists() {
  local file_path="$1"
  if [[ ! -f "$file_path" ]]; then
    echo "Required file not found: $file_path" >&2
    exit 1
  fi
}

if ! id -u finex >/dev/null 2>&1; then
  echo "User 'finex' does not exist. Run bootstrap_ubuntu.sh first." >&2
  exit 1
fi

assert_exists "$APP_HOME/backend/.env.production"
assert_exists "$APP_HOME/backend/auth-service.jar"
assert_exists "$APP_HOME/backend/gateway.jar"
assert_exists "$NGINX_TEMPLATE"

install -m 0644 "$ROOT_DIR/deploy/linux/systemd/finex-auth.service" /etc/systemd/system/finex-auth.service
install -m 0644 "$ROOT_DIR/deploy/linux/systemd/finex-gateway.service" /etc/systemd/system/finex-gateway.service
render_nginx_site

mkdir -p "$APP_HOME/logs"
chown -R finex:finex "$APP_HOME"

nginx -t
systemctl daemon-reload
systemctl enable finex-auth.service finex-gateway.service nginx
systemctl restart finex-auth.service
systemctl restart finex-gateway.service
systemctl restart nginx

cat <<EOF
Runtime install complete.
- Domain:   $FINEX_DOMAIN
- Nginx:    $NGINX_SITE
- App home: $APP_HOME

Next steps:
1. Point DNS A record: $FINEX_DOMAIN -> your Alibaba Cloud public IP
2. After filing is approved, run:
   CERTBOT_EMAIL=you@example.com FINEX_DOMAIN=$FINEX_DOMAIN bash deploy/linux/scripts/enable_https.sh
3. In WeCom, trust this domain and the server public IP
EOF