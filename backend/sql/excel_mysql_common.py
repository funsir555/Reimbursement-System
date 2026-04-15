#!/usr/bin/env python3
"""
Shared helpers for Excel/MySQL import-export scripts.
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

SYSTEM_MANAGED_COLUMN_NAMES = {
    "id",
    "created_at",
    "updated_at",
    "created_by",
    "updated_by",
}


DEFAULT_APP_YML = (
    Path(__file__).resolve().parents[1]
    / "auth-service"
    / "src"
    / "main"
    / "resources"
    / "application.yml"
)


class ExcelToolError(Exception):
    """Base exception for Excel/MySQL helper scripts."""


@dataclass(frozen=True)
class ColumnMeta:
    name: str
    data_type: str
    column_type: str
    is_nullable: bool
    column_default: Any
    extra: str
    column_key: str
    comment: str


@dataclass(frozen=True)
class RegexCheckConstraint:
    name: str
    column_name: str
    pattern: str


def split_csv_arg(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def quote_identifier(identifier: str) -> str:
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", identifier):
        raise ExcelToolError(f"非法标识符：{identifier}")
    return f"`{identifier}`"


def resolve_placeholder(raw: str) -> str:
    raw = raw.strip().strip("'").strip('"')
    match = re.fullmatch(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?\}", raw)
    if not match:
        return raw
    env_name, default_value = match.groups()
    env_value = os.getenv(env_name)
    if env_value not in (None, ""):
        return env_value
    if default_value is None:
        raise ExcelToolError(f"环境变量 {env_name} 未设置，且 application.yml 中没有默认值。")
    return default_value


def load_datasource_settings(path: Path) -> tuple[str, str, str]:
    if not path.exists():
        raise ExcelToolError(f"未找到 application.yml：{path}")
    text = path.read_text(encoding="utf-8")
    url_match = re.search(r"(?m)^\s*url:\s*(?P<value>.+?)\s*$", text)
    user_match = re.search(r"(?m)^\s*username:\s*(?P<value>.+?)\s*$", text)
    password_match = re.search(r"(?m)^\s*password:\s*(?P<value>.+?)\s*$", text)
    if not url_match or not user_match or not password_match:
        raise ExcelToolError("无法从 application.yml 解析数据库连接配置。")
    url = resolve_placeholder(url_match.group("value"))
    username = resolve_placeholder(user_match.group("value"))
    password = resolve_placeholder(password_match.group("value"))
    return url, username, password


def parse_jdbc_url(jdbc_url: str) -> dict[str, Any]:
    match = re.fullmatch(
        r"jdbc:mysql://(?P<host>[^:/?#]+)(?::(?P<port>\d+))?/(?P<db>[^?]+)(?:\?(?P<query>.*))?",
        jdbc_url.strip(),
    )
    if not match:
        raise ExcelToolError(f"不支持的 JDBC URL：{jdbc_url}")
    return {
        "host": match.group("host"),
        "port": int(match.group("port") or "3306"),
        "database": match.group("db"),
    }


def open_connection(pymysql: Any, app_yml: Path) -> Any:
    url, username, password = load_datasource_settings(app_yml)
    parsed = parse_jdbc_url(url)
    return pymysql.connect(
        host=parsed["host"],
        port=parsed["port"],
        user=username,
        password=password,
        database=parsed["database"],
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


def fetch_columns(cursor: Any, table_name: str) -> dict[str, ColumnMeta]:
    sql = """
        SELECT
            COLUMN_NAME,
            DATA_TYPE,
            COLUMN_TYPE,
            IS_NULLABLE,
            COLUMN_DEFAULT,
            EXTRA,
            COLUMN_KEY,
            COLUMN_COMMENT
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = %s
        ORDER BY ORDINAL_POSITION
    """
    cursor.execute(sql, (table_name,))
    rows = cursor.fetchall()
    if not rows:
        raise ExcelToolError(f"目标表不存在：{table_name}")
    columns: dict[str, ColumnMeta] = {}
    for row in rows:
        columns[row["COLUMN_NAME"]] = ColumnMeta(
            name=row["COLUMN_NAME"],
            data_type=row["DATA_TYPE"].lower(),
            column_type=row["COLUMN_TYPE"].lower(),
            is_nullable=row["IS_NULLABLE"] == "YES",
            column_default=row["COLUMN_DEFAULT"],
            extra=(row["EXTRA"] or "").lower(),
            column_key=(row["COLUMN_KEY"] or "").upper(),
            comment=row["COLUMN_COMMENT"] or "",
        )
    return columns


def fetch_regex_check_constraints(cursor: Any, table_name: str) -> list[RegexCheckConstraint]:
    sql = """
        SELECT
            tc.CONSTRAINT_NAME,
            cc.CHECK_CLAUSE
        FROM information_schema.TABLE_CONSTRAINTS tc
        JOIN information_schema.CHECK_CONSTRAINTS cc
          ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
         AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.TABLE_SCHEMA = DATABASE()
          AND tc.TABLE_NAME = %s
          AND tc.CONSTRAINT_TYPE = 'CHECK'
        ORDER BY tc.CONSTRAINT_NAME
    """
    cursor.execute(sql, (table_name,))
    rows = cursor.fetchall()
    constraints: list[RegexCheckConstraint] = []
    regexp_like_pattern = re.compile(
        r"regexp_like\(\s*`?(?P<column>[A-Za-z_][A-Za-z0-9_]*)`?\s*,\s*(?:_[A-Za-z0-9]+)?'(?P<pattern>(?:\\'|[^'])*)'\s*\)",
        re.IGNORECASE,
    )
    regexp_operator_pattern = re.compile(
        r"`?(?P<column>[A-Za-z_][A-Za-z0-9_]*)`?\s+regexp\s+(?:_[A-Za-z0-9]+)?'(?P<pattern>(?:\\'|[^'])*)'",
        re.IGNORECASE,
    )
    for row in rows:
        clause = (row["CHECK_CLAUSE"] or "").strip()
        normalized_clause = clause.replace("\\'", "'")
        match = regexp_like_pattern.fullmatch(normalized_clause) or regexp_operator_pattern.fullmatch(normalized_clause)
        if not match:
            continue
        constraints.append(
            RegexCheckConstraint(
                name=row["CONSTRAINT_NAME"],
                column_name=match.group("column"),
                pattern=match.group("pattern").replace("\\'", "'"),
            )
        )
    return constraints


def is_current_timestamp_default(value: Any) -> bool:
    if value is None:
        return False
    normalized = str(value).strip().lower()
    return normalized in {"current_timestamp", "current_timestamp()", "now()", "localtimestamp", "localtimestamp()"}


def is_system_managed_column(meta: ColumnMeta) -> bool:
    if meta.name.lower() in SYSTEM_MANAGED_COLUMN_NAMES:
        return True
    if "auto_increment" in meta.extra:
        return True
    if is_current_timestamp_default(meta.column_default):
        return True
    if "on update current_timestamp" in meta.extra:
        return True
    return False


def filter_user_editable_columns(columns: dict[str, ColumnMeta]) -> dict[str, ColumnMeta]:
    return {
        name: meta
        for name, meta in columns.items()
        if not is_system_managed_column(meta)
    }


def format_default_value(value: Any) -> str:
    if value is None:
        return "-"
    return str(value)


def format_column_constraint(meta: ColumnMeta) -> str:
    parts = [meta.column_type.upper(), "NULL" if meta.is_nullable else "NOT NULL"]
    if meta.column_default is not None:
        parts.append(f"DEFAULT {format_default_value(meta.column_default)}")
    if meta.extra:
        parts.append(meta.extra.upper())
    if meta.column_key == "PRI":
        parts.append("PRIMARY KEY")
    elif meta.column_key == "UNI":
        parts.append("UNIQUE")
    return " ".join(parts)
