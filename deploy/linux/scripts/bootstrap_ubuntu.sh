#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run this script as root."
  exit 1
fi

APP_HOME="${APP_HOME:-/opt/finex}"
REPO_DIR="${REPO_DIR:-$APP_HOME/repo}"

apt-get update
apt-get install -y \
  ca-certificates \
  certbot \
  curl \
  git \
  gnupg \
  lsb-release \
  maven \
  mysql-client \
  mysql-server \
  nginx \
  openjdk-17-jre-headless \
  python3-certbot-nginx \
  rsync

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

if ! id -u finex >/dev/null 2>&1; then
  useradd -r -m -d /opt/finex -s /bin/bash finex
fi

mkdir -p \
  "$APP_HOME/backend" \
  "$APP_HOME/logs" \
  "$APP_HOME/repo" \
  "$APP_HOME/sql-backups" \
  "$APP_HOME/www/admin"
chown -R finex:finex "$APP_HOME"

systemctl enable mysql nginx
systemctl restart mysql
systemctl restart nginx

cat <<EOF
Bootstrap complete.
- Runtime: Java 17 / Maven / Node.js / Nginx / MySQL 8
- App home: $APP_HOME
- Repo dir:  $REPO_DIR

Next steps:
1. Copy the repository to $REPO_DIR
2. Run deploy/linux/scripts/init_mysql.sh
3. Create backend/.env.production and frontend/admin-web/.env.production
4. Run deploy/linux/scripts/deploy_finex.sh
5. Run FINEX_DOMAIN=admin.finexgz.xyz bash deploy/linux/scripts/install_runtime.sh
EOF