#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run this script as root."
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
DB_NAME="${DB_NAME:-finex_db}"
DB_USER="${DB_USER:-finex_user}"
DB_PASSWORD="${DB_PASSWORD:-change-me}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
RUN_PERMISSION_REFRESH="${RUN_PERMISSION_REFRESH:-0}"

escape_sql_literal() {
  printf "%s" "$1" | sed "s/'/''/g"
}

if [[ -n "$MYSQL_ROOT_PASSWORD" ]]; then
  MYSQL_ROOT_CMD=(mysql -uroot "-p$MYSQL_ROOT_PASSWORD")
else
  MYSQL_ROOT_CMD=(mysql -uroot)
fi

DB_NAME_ESCAPED="$(escape_sql_literal "$DB_NAME")"
DB_USER_ESCAPED="$(escape_sql_literal "$DB_USER")"
DB_PASSWORD_ESCAPED="$(escape_sql_literal "$DB_PASSWORD")"

SQL_FILES=(
  "$ROOT_DIR/backend/sql/init.sql"
  "$ROOT_DIR/backend/sql/init_custom_archive.sql"
  "$ROOT_DIR/backend/sql/init_expense_type_tree.sql"
  "$ROOT_DIR/backend/sql/init_async_task.sql"
  "$ROOT_DIR/backend/sql/init_finance_gl.sql"
  "$ROOT_DIR/backend/sql/init_process_flow_design.sql"
  "$ROOT_DIR/backend/sql/migrate_system_settings.sql"
)

OPTIONAL_SQL_FILES=(
  "$ROOT_DIR/backend/sql/refresh_system_settings_permissions.sql"
)

for sql_file in "${SQL_FILES[@]}"; do
  if [[ ! -f "$sql_file" ]]; then
    echo "Missing SQL file: $sql_file" >&2
    exit 1
  fi
done

"${MYSQL_ROOT_CMD[@]}" <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NAME_ESCAPED\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER_ESCAPED'@'localhost' IDENTIFIED BY '$DB_PASSWORD_ESCAPED';
ALTER USER '$DB_USER_ESCAPED'@'localhost' IDENTIFIED BY '$DB_PASSWORD_ESCAPED';
GRANT ALL PRIVILEGES ON \`$DB_NAME_ESCAPED\`.* TO '$DB_USER_ESCAPED'@'localhost';
FLUSH PRIVILEGES;
SQL

for sql_file in "${SQL_FILES[@]}"; do
  echo "Applying $(basename "$sql_file") ..."
  "${MYSQL_ROOT_CMD[@]}" "$DB_NAME" < "$sql_file"
done

if [[ "$RUN_PERMISSION_REFRESH" == "1" ]]; then
  for sql_file in "${OPTIONAL_SQL_FILES[@]}"; do
    if [[ -f "$sql_file" ]]; then
      echo "Applying optional $(basename "$sql_file") ..."
      "${MYSQL_ROOT_CMD[@]}" "$DB_NAME" < "$sql_file"
    fi
  done
fi

cat <<EOF
MySQL initialization complete.
- Database: $DB_NAME
- User:     $DB_USER@localhost

If menus or permissions look incomplete later, rerun with:
RUN_PERMISSION_REFRESH=1 DB_PASSWORD='<same-password>' bash deploy/linux/scripts/init_mysql.sh
EOF