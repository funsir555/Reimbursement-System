#!/usr/bin/env python3
"""
Generic Excel importer for finex_db.

Recommended setup on Windows:
    python -m venv .venv
    .\\.venv\\Scripts\\python.exe -m pip install --upgrade pip
    .\\.venv\\Scripts\\python.exe -m pip install openpyxl pymysql

Examples:
    .\\.venv\\Scripts\\python.exe import_excel_to_mysql.py ^
        --file C:\\data\\customer.xlsx ^
        --table gl_Customer ^
        --key-columns cCusCode ^
        --dry-run

    .\\.venv\\Scripts\\python.exe import_excel_to_mysql.py ^
        --file C:\\data\\customer_template.xlsx ^
        --table gl_Customer ^
        --key-columns cCusCode ^
        --header-row 1 ^
        --start-row 4 ^
        --dry-run

Rules:
    - The first header row must use database column names.
    - Templates exported by export_table_template.py keep row 1 as headers,
      row 2 as Chinese comments, row 3 as column constraints, and row 4+ as data.
    - Existing keys cause the import to fail immediately.
    - DATE / DATETIME / TIMESTAMP columns are validated and normalized.
    - DECIMAL(18,2) columns are treated as amount columns by default.
    - Suspected garbled text values are rejected.
    - Use --dry-run before a real import.
"""

from __future__ import annotations

import argparse
import datetime as dt
import sys
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Iterable, Sequence

from excel_mysql_common import (
    DEFAULT_APP_YML,
    ColumnMeta,
    ExcelToolError,
    fetch_columns,
    open_connection,
    split_csv_arg,
    quote_identifier,
)

DATE_INPUT_FORMATS = (
    "%Y-%m-%d",
    "%Y/%m/%d",
    "%Y-%m-%d %H:%M:%S",
    "%Y/%m/%d %H:%M:%S",
)
TEXT_DATA_TYPES = {"char", "varchar", "text", "mediumtext", "longtext"}
DATE_DATA_TYPES = {"date", "datetime", "timestamp"}
AMOUNT_QUANT = Decimal("0.01")
SUSPICIOUS_TEXT_PATTERNS = (
    "????",
    "???",
    "??",
    "\ufffd",
    "锟",
    "�",
)
INSERT_BATCH_SIZE = 200


@dataclass
class ValidationError(ExcelToolError):
    row_number: int
    column_name: str
    raw_value: Any
    reason: str

    def __str__(self) -> str:
        return (
            f"Row {self.row_number}, column '{self.column_name}', "
            f"value={self.raw_value!r}: {self.reason}"
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Import .xlsx rows into a MySQL table. For templates exported by "
        "export_table_template.py, use --header-row 1 --start-row 4."
    )
    parser.add_argument("--file", required=True, help="Path to the .xlsx file.")
    parser.add_argument("--table", required=True, help="Target table name.")
    parser.add_argument("--sheet", help="Worksheet name. Defaults to the first worksheet.")
    parser.add_argument(
        "--key-columns",
        required=True,
        help="Comma-separated unique key columns used for duplicate checks.",
    )
    parser.add_argument("--header-row", type=int, default=1, help="Header row number. Default: 1.")
    parser.add_argument("--start-row", type=int, default=2, help="Data start row number. Default: 2.")
    parser.add_argument("--dry-run", action="store_true", help="Validate only, do not write to DB.")
    parser.add_argument(
        "--date-columns",
        default="",
        help="Extra comma-separated columns forced to use date validation.",
    )
    parser.add_argument(
        "--amount-columns",
        default="",
        help="Extra comma-separated columns forced to use amount validation.",
    )
    parser.add_argument(
        "--text-columns",
        default="",
        help="Extra comma-separated columns forced to use strict text/garbled validation.",
    )
    parser.add_argument(
        "--app-yml",
        default=str(DEFAULT_APP_YML),
        help="Path to application.yml. Default: backend auth-service datasource config.",
    )
    args = parser.parse_args()
    if args.header_row < 1 or args.start_row < 1:
        parser.error("--header-row and --start-row must be positive integers.")
    if args.start_row <= args.header_row:
        parser.error("--start-row must be greater than --header-row.")
    return args


def ensure_dependencies() -> tuple[Any, Any]:
    try:
        import openpyxl  # type: ignore
    except ModuleNotFoundError as exc:
        raise SystemExit("Missing dependency 'openpyxl'. Run: pip install openpyxl pymysql") from exc
    try:
        import pymysql  # type: ignore
    except ModuleNotFoundError as exc:
        raise SystemExit("Missing dependency 'pymysql'. Run: pip install openpyxl pymysql") from exc
    return openpyxl, pymysql


def load_workbook(openpyxl: Any, file_path: Path, sheet_name: str | None) -> Any:
    if not file_path.exists():
        raise ExcelToolError(f"Excel file not found: {file_path}")
    workbook = openpyxl.load_workbook(filename=file_path, read_only=True, data_only=True)
    if sheet_name:
        if sheet_name not in workbook.sheetnames:
            raise ExcelToolError(f"Worksheet not found: {sheet_name}")
        return workbook[sheet_name]
    return workbook[workbook.sheetnames[0]]


def read_headers(sheet: Any, header_row: int) -> list[str]:
    headers = []
    seen = set()
    header_cells = next(sheet.iter_rows(min_row=header_row, max_row=header_row))
    for cell in header_cells:
        value = cell.value
        if value is None or str(value).strip() == "":
            continue
        header = str(value).strip()
        if header in seen:
            raise ExcelToolError(f"Duplicate header in Excel: {header}")
        seen.add(header)
        headers.append(header)
    if not headers:
        raise ExcelToolError("Header row is empty.")
    return headers


def validate_headers(headers: Sequence[str], columns: dict[str, ColumnMeta], key_columns: Sequence[str]) -> None:
    db_columns = set(columns)
    extra_headers = [header for header in headers if header not in db_columns]
    if extra_headers:
        raise ExcelToolError(f"Excel has columns not found in DB: {', '.join(extra_headers)}")
    missing_required = []
    for name, meta in columns.items():
        if name in headers:
            continue
        if meta.is_nullable or meta.column_default is not None or "auto_increment" in meta.extra:
            continue
        missing_required.append(name)
    if missing_required:
        raise ExcelToolError(
            "Excel is missing required DB columns: " + ", ".join(missing_required)
        )
    missing_keys = [column for column in key_columns if column not in headers]
    if missing_keys:
        raise ExcelToolError("Key columns are missing from Excel header: " + ", ".join(missing_keys))


def is_blank_row(row_values: Sequence[Any]) -> bool:
    return all(value is None or str(value).strip() == "" for value in row_values)


def parse_date_value(value: Any, meta: ColumnMeta, row_number: int, column_name: str) -> Any:
    if value is None or str(value).strip() == "":
        return None
    if isinstance(value, dt.datetime):
        if meta.data_type == "date":
            return value.date()
        return value.replace(microsecond=0)
    if isinstance(value, dt.date):
        if meta.data_type == "date":
            return value
        return dt.datetime.combine(value, dt.time.min)
    if isinstance(value, str):
        text = value.strip()
        for fmt in DATE_INPUT_FORMATS:
            try:
                parsed = dt.datetime.strptime(text, fmt)
                if meta.data_type == "date":
                    return parsed.date()
                return parsed
            except ValueError:
                continue
    raise ValidationError(row_number, column_name, value, "invalid date format")


def parse_amount_value(value: Any, row_number: int, column_name: str) -> Decimal | None:
    if value is None or str(value).strip() == "":
        return None
    if isinstance(value, bool):
        raise ValidationError(row_number, column_name, value, "boolean is not a valid amount")
    if isinstance(value, str):
        text = value.strip()
        if "," in text:
            raise ValidationError(row_number, column_name, value, "comma-separated amount is not supported")
        candidate = text
    else:
        candidate = str(value)
    try:
        decimal_value = Decimal(candidate)
    except InvalidOperation as exc:
        raise ValidationError(row_number, column_name, value, "invalid numeric amount") from exc
    if decimal_value.as_tuple().exponent < -2:
        raise ValidationError(row_number, column_name, value, "amount has more than 2 decimal places")
    return decimal_value.quantize(AMOUNT_QUANT)


def check_text_value(value: Any, row_number: int, column_name: str, strict: bool) -> str | None:
    if value is None:
        return None
    text = str(value)
    if text.strip() == "":
        return None
    for marker in SUSPICIOUS_TEXT_PATTERNS:
        if marker in text:
            raise ValidationError(row_number, column_name, value, "suspected garbled text")
    if strict and text.strip() in {"?", "??", "???", "????", "NULL", "null"}:
        raise ValidationError(row_number, column_name, value, "placeholder text is not allowed")
    return text


def normalize_row(
    row_number: int,
    row_map: dict[str, Any],
    columns: dict[str, ColumnMeta],
    date_columns: set[str],
    amount_columns: set[str],
    strict_text_columns: set[str],
) -> dict[str, Any]:
    normalized: dict[str, Any] = {}
    for column_name, value in row_map.items():
        meta = columns[column_name]
        if value is None or (isinstance(value, str) and value.strip() == ""):
            normalized[column_name] = None
            continue
        if column_name in date_columns:
            normalized[column_name] = parse_date_value(value, meta, row_number, column_name)
            continue
        if column_name in amount_columns:
            normalized[column_name] = parse_amount_value(value, row_number, column_name)
            continue
        if meta.data_type in TEXT_DATA_TYPES or column_name in strict_text_columns:
            normalized[column_name] = check_text_value(
                value, row_number, column_name, column_name in strict_text_columns
            )
            continue
        normalized[column_name] = value
    return normalized


def build_key_tuple(row_map: dict[str, Any], key_columns: Sequence[str], row_number: int) -> tuple[Any, ...]:
    values = []
    for column in key_columns:
        value = row_map.get(column)
        if value is None or str(value).strip() == "":
            raise ValidationError(row_number, column, value, "key column cannot be empty")
        values.append(value)
    return tuple(values)


def detect_duplicate_keys_in_excel(key_rows: Iterable[tuple[int, tuple[Any, ...]]]) -> None:
    seen: dict[tuple[Any, ...], int] = {}
    for row_number, key_tuple in key_rows:
        if key_tuple in seen:
            raise ExcelToolError(
                f"Duplicate key found in Excel. First row={seen[key_tuple]}, duplicate row={row_number}, key={key_tuple!r}"
            )
        seen[key_tuple] = row_number


def detect_existing_keys(cursor: Any, table_name: str, key_columns: Sequence[str], key_values: Sequence[tuple[Any, ...]]) -> None:
    if not key_values:
        return
    key_expr = ", ".join(quote_identifier(column) for column in key_columns)
    chunk_size = 500
    for start in range(0, len(key_values), chunk_size):
        chunk = key_values[start : start + chunk_size]
        placeholders = ", ".join(["(" + ", ".join(["%s"] * len(key_columns)) + ")"] * len(chunk))
        sql = (
            f"SELECT {key_expr} FROM {quote_identifier(table_name)} "
            f"WHERE ({key_expr}) IN ({placeholders}) LIMIT 1"
        )
        params: list[Any] = []
        for key_tuple in chunk:
            params.extend(key_tuple)
        cursor.execute(sql, params)
        row = cursor.fetchone()
        if row:
            existing_key = tuple(row[column] for column in key_columns)
            raise ExcelToolError(f"Existing key found in database: {existing_key!r}")


def insert_rows(cursor: Any, table_name: str, headers: Sequence[str], rows: Sequence[dict[str, Any]]) -> int:
    if not rows:
        return 0
    sql = (
        f"INSERT INTO {quote_identifier(table_name)} "
        f"({', '.join(quote_identifier(column) for column in headers)}) "
        f"VALUES ({', '.join(['%s'] * len(headers))})"
    )
    inserted = 0
    for start in range(0, len(rows), INSERT_BATCH_SIZE):
        batch = rows[start : start + INSERT_BATCH_SIZE]
        params = [tuple(row.get(column) for column in headers) for row in batch]
        cursor.executemany(sql, params)
        inserted += len(batch)
        print(f"[INFO] Inserted {inserted}/{len(rows)} rows")
    return inserted


def main() -> int:
    args = parse_args()
    openpyxl, pymysql = ensure_dependencies()

    excel_path = Path(args.file).expanduser().resolve()
    app_yml_path = Path(args.app_yml).expanduser().resolve()
    key_columns = split_csv_arg(args.key_columns)
    forced_date_columns = set(split_csv_arg(args.date_columns))
    forced_amount_columns = set(split_csv_arg(args.amount_columns))
    forced_text_columns = set(split_csv_arg(args.text_columns))

    connection = None
    try:
        connection = open_connection(pymysql, app_yml_path)
        with connection.cursor() as cursor:
            columns = fetch_columns(cursor, args.table)
            sheet = load_workbook(openpyxl, excel_path, args.sheet)
            headers = read_headers(sheet, args.header_row)
            validate_headers(headers, columns, key_columns)

            unknown_forced = (forced_date_columns | forced_amount_columns | forced_text_columns) - set(headers)
            if unknown_forced:
                raise ExcelToolError(
                    "Forced validation columns are missing from Excel header: "
                    + ", ".join(sorted(unknown_forced))
                )

            date_columns = {
                name for name in headers if columns[name].data_type in DATE_DATA_TYPES
            } | forced_date_columns
            amount_columns = {
                name for name in headers if columns[name].column_type == "decimal(18,2)"
            } | forced_amount_columns

            normalized_rows: list[dict[str, Any]] = []
            excel_keys: list[tuple[int, tuple[Any, ...]]] = []
            total_rows = 0
            for row_number, row in enumerate(
                sheet.iter_rows(min_row=args.start_row, values_only=True),
                start=args.start_row,
            ):
                row_values = list(row[: len(headers)])
                if is_blank_row(row_values):
                    continue
                total_rows += 1
                row_map = {headers[idx]: row_values[idx] if idx < len(row_values) else None for idx in range(len(headers))}
                normalized = normalize_row(
                    row_number=row_number,
                    row_map=row_map,
                    columns=columns,
                    date_columns=date_columns,
                    amount_columns=amount_columns,
                    strict_text_columns=forced_text_columns,
                )
                key_tuple = build_key_tuple(normalized, key_columns, row_number)
                excel_keys.append((row_number, key_tuple))
                normalized_rows.append(normalized)

            if total_rows == 0:
                raise ExcelToolError("No data rows found in Excel.")

            detect_duplicate_keys_in_excel(excel_keys)
            detect_existing_keys(cursor, args.table, key_columns, [item[1] for item in excel_keys])

            print(f"[INFO] Excel rows validated: {len(normalized_rows)}")
            print(f"[INFO] Date columns: {sorted(date_columns)}")
            print(f"[INFO] Amount columns: {sorted(amount_columns)}")
            print(f"[INFO] Strict text columns: {sorted(forced_text_columns)}")

            if args.dry_run:
                print("[INFO] Dry run succeeded. No rows written.")
                connection.rollback()
                return 0

            inserted = insert_rows(cursor, args.table, headers, normalized_rows)
            connection.commit()
            print(f"[INFO] Import completed successfully. Inserted rows: {inserted}")
            return 0
    except ValidationError as exc:
        if connection is not None:
            connection.rollback()
        print(f"[ERROR] Validation failed: {exc}", file=sys.stderr)
        return 1
    except ExcelToolError as exc:
        if connection is not None:
            connection.rollback()
        print(f"[ERROR] {exc}", file=sys.stderr)
        return 1
    except Exception as exc:  # pragma: no cover - defensive CLI fallback
        if connection is not None:
            connection.rollback()
        print(f"[ERROR] Unexpected failure: {exc}", file=sys.stderr)
        return 1
    finally:
        if connection is not None:
            connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
