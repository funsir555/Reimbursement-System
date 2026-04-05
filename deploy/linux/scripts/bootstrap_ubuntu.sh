#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "请使用 root 运行此脚本"
  exit 1
fi

apt-get update
apt-get install -y curl git nginx mysql-client openjdk-17-jre-headless maven rsync

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y nodejs
fi

if ! id -u finex >/dev/null 2>&1; then
  useradd -r -m -d /opt/finex -s /bin/bash finex
fi

mkdir -p /opt/finex/backend /opt/finex/www/admin /opt/finex/logs /opt/finex/repo
chown -R finex:finex /opt/finex

echo "基础环境已完成：Java 17 / Maven / Node.js / Nginx / finex 用户"
