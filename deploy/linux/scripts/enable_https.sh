#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run this script as root."
  exit 1
fi

FINEX_DOMAIN="${FINEX_DOMAIN:-admin.finexgz.xyz}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"

if ! command -v certbot >/dev/null 2>&1; then
  apt-get update
  apt-get install -y certbot python3-certbot-nginx
fi

if [[ -n "$CERTBOT_EMAIL" ]]; then
  certbot --nginx --redirect -d "$FINEX_DOMAIN" --agree-tos --email "$CERTBOT_EMAIL" --non-interactive
else
  certbot --nginx --redirect -d "$FINEX_DOMAIN" --agree-tos --register-unsafely-without-email --non-interactive
fi

nginx -t
systemctl reload nginx

echo "HTTPS is enabled for $FINEX_DOMAIN."