USE finex_db;

SET NAMES utf8mb4;

/*
用途:
1. 为总账账簿报表补充高频组合索引
2. 优先覆盖序时账、明细账及各辅助明细账的查询路径

说明:
- 本脚本为幂等脚本，可重复执行
- 只新增索引，不改历史数据
*/

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_bill_ino'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_bill_ino (company_id, iyear, iperiod, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_ccode_bill'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_ccode_bill (company_id, iyear, iperiod, ccode, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_item_bill'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_item_bill (company_id, iyear, iperiod, citem_class, citem_id, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_supplier_bill'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_supplier_bill (company_id, iyear, iperiod, csup_id, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_customer_bill'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_customer_bill (company_id, iyear, iperiod, ccus_id, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = @schema_name
          AND table_name = 'gl_accvouch'
          AND index_name = 'idx_gl_accvouch_company_period_person_bill'
    ),
    'SELECT 1',
    'ALTER TABLE gl_accvouch ADD INDEX idx_gl_accvouch_company_period_person_bill (company_id, iyear, iperiod, cperson_id, dbill_date, ino_id, inid)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
