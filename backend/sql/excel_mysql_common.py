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


def split_csv_arg(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def quote_identifier(identifier: str) -> str:
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", identifier):
        raise ExcelToolError(f"Invalid identifier: {identifier}")
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
        raise ExcelToolError(f"Environment variable '{env_name}' is required but not set.")
    return default_value


def load_datasource_settings(path: Path) -> tuple[str, str, str]:
    if not path.exists():
        raise ExcelToolError(f"application.yml not found: {path}")
    text = path.read_text(encoding="utf-8")
    url_match = re.search(r"(?m)^\s*url:\s*(?P<value>.+?)\s*$", text)
    user_match = re.search(r"(?m)^\s*username:\s*(?P<value>.+?)\s*$", text)
    password_match = re.search(r"(?m)^\s*password:\s*(?P<value>.+?)\s*$", text)
    if not url_match or not user_match or not password_match:
        raise ExcelToolError("Failed to parse datasource settings from application.yml.")
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
        raise ExcelToolError(f"Unsupported JDBC URL: {jdbc_url}")
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
        raise ExcelToolError(f"Target table not found: {table_name}")
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
