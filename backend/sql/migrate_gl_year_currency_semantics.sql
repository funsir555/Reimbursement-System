USE finex_db;

SET NAMES utf8mb4;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_migrate_gl_year_currency_semantics $$
CREATE PROCEDURE sp_migrate_gl_year_currency_semantics()
BEGIN
    DECLARE missing_gl_accvouch_year_rows BIGINT DEFAULT 0;
    DECLARE missing_gl_accsum_year_rows BIGINT DEFAULT 0;
    DECLARE missing_gl_accass_year_rows BIGINT DEFAULT 0;
    DECLARE unresolved_gl_accvouch_currency_rows BIGINT DEFAULT 0;
    DECLARE unresolved_gl_accsum_currency_rows BIGINT DEFAULT 0;
    DECLARE unresolved_gl_accass_currency_rows BIGINT DEFAULT 0;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'iyear'
    ) THEN
        ALTER TABLE gl_accvouch
            ADD COLUMN iyear INT NULL COMMENT '会计年度' AFTER i_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'iyperiod'
    ) THEN
        ALTER TABLE gl_accvouch
            ADD COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)' AFTER iyear;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'checked_at'
    ) THEN
        ALTER TABLE gl_accvouch
            ADD COLUMN checked_at DATETIME NULL COMMENT '审核时间' AFTER ccheck;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'posted_at'
    ) THEN
        ALTER TABLE gl_accvouch
            ADD COLUMN posted_at DATETIME NULL COMMENT '记账时间' AFTER ibook;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND COLUMN_NAME = 'currency_code'
    ) THEN
        ALTER TABLE gl_accvouch
            ADD COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码' AFTER cexch_name;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND COLUMN_NAME = 'iyear'
    ) THEN
        ALTER TABLE gl_accsum
            ADD COLUMN iyear INT NULL COMMENT '会计年度' AFTER i_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND COLUMN_NAME = 'iyperiod'
    ) THEN
        ALTER TABLE gl_accsum
            ADD COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)' AFTER iyear;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND COLUMN_NAME = 'currency_code'
    ) THEN
        ALTER TABLE gl_accsum
            ADD COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码' AFTER cexch_name;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND COLUMN_NAME = 'iyear'
    ) THEN
        ALTER TABLE gl_accass
            ADD COLUMN iyear INT NULL COMMENT '会计年度' AFTER i_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND COLUMN_NAME = 'iyperiod'
    ) THEN
        ALTER TABLE gl_accass
            ADD COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)' AFTER iyear;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND COLUMN_NAME = 'currency_code'
    ) THEN
        ALTER TABLE gl_accass
            ADD COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码' AFTER cexch_name;
    END IF;

    ALTER TABLE gl_accvouch
        MODIFY COLUMN iyear INT NULL COMMENT '会计年度',
        MODIFY COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)',
        MODIFY COLUMN checked_at DATETIME NULL COMMENT '审核时间',
        MODIFY COLUMN posted_at DATETIME NULL COMMENT '记账时间',
        MODIFY COLUMN cexch_name VARCHAR(32) NULL COMMENT '币种名称',
        MODIFY COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码';

    ALTER TABLE gl_accsum
        MODIFY COLUMN iyear INT NULL COMMENT '会计年度',
        MODIFY COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)',
        MODIFY COLUMN cexch_name VARCHAR(32) NULL COMMENT '币种名称',
        MODIFY COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码';

    ALTER TABLE gl_accass
        MODIFY COLUMN iyear INT NULL COMMENT '会计年度',
        MODIFY COLUMN iyperiod INT NULL COMMENT '会计年月(YYYYMM)',
        MODIFY COLUMN cexch_name VARCHAR(32) NULL COMMENT '币种名称',
        MODIFY COLUMN currency_code VARCHAR(32) NULL COMMENT '币种编码';

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND INDEX_NAME = 'idx_gl_accvouch_company_year_period'
    ) THEN
        ALTER TABLE gl_accvouch
            DROP INDEX idx_gl_accvouch_company_year_period;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accvouch'
          AND INDEX_NAME = 'idx_gl_accvouch_company_year_period_no'
    ) THEN
        ALTER TABLE gl_accvouch
            DROP INDEX idx_gl_accvouch_company_year_period_no;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accsum'
          AND INDEX_NAME = 'idx_gl_accsum_company_year_period'
    ) THEN
        ALTER TABLE gl_accsum
            DROP INDEX idx_gl_accsum_company_year_period;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'gl_accass'
          AND INDEX_NAME = 'idx_gl_accass_company_year_period'
    ) THEN
        ALTER TABLE gl_accass
            DROP INDEX idx_gl_accass_company_year_period;
    END IF;

    ALTER TABLE gl_accvouch
        ADD INDEX idx_gl_accvouch_company_year_period (company_id, iyear, iyperiod),
        ADD INDEX idx_gl_accvouch_company_year_period_no (company_id, iyear, iyperiod, csign, ino_id);

    ALTER TABLE gl_accsum
        ADD INDEX idx_gl_accsum_company_year_period (company_id, iyear, iyperiod);

    ALTER TABLE gl_accass
        ADD INDEX idx_gl_accass_company_year_period (company_id, iyear, iyperiod);

    SELECT COUNT(*)
      INTO missing_gl_accvouch_year_rows
    FROM gl_accvouch
    WHERE (iyear IS NULL OR iyperiod IS NULL)
      AND (dbill_date IS NULL OR iperiod IS NULL);

    IF missing_gl_accvouch_year_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accvouch 存在无法按 dbill_date + iperiod 回填年度字段的记录，请先专项处理';
    END IF;

    UPDATE gl_accvouch
    SET iyear = COALESCE(iyear, YEAR(dbill_date)),
        iyperiod = COALESCE(iyperiod, YEAR(dbill_date) * 100 + iperiod)
    WHERE (iyear IS NULL OR iyperiod IS NULL)
      AND dbill_date IS NOT NULL
      AND iperiod IS NOT NULL;

    SELECT COUNT(*)
      INTO missing_gl_accsum_year_rows
    FROM gl_accsum
    WHERE iyear IS NULL OR iyperiod IS NULL;

    IF missing_gl_accsum_year_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accsum 存在非空记录但缺少可判定年度来源，请改走专项回填方案';
    END IF;

    SELECT COUNT(*)
      INTO missing_gl_accass_year_rows
    FROM gl_accass
    WHERE iyear IS NULL OR iyperiod IS NULL;

    IF missing_gl_accass_year_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accass 存在非空记录但缺少可判定年度来源，请改走专项回填方案';
    END IF;

    UPDATE gl_accvouch
    SET currency_code = 'CNY',
        cexch_name = '人民币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) IN ('CNY', 'RMB')
       OR TRIM(cexch_name) = '人民币';

    UPDATE gl_accvouch
    SET currency_code = 'USD',
        cexch_name = '美元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'USD'
       OR TRIM(cexch_name) = '美元';

    UPDATE gl_accvouch
    SET currency_code = 'EUR',
        cexch_name = '欧元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'EUR'
       OR TRIM(cexch_name) = '欧元';

    UPDATE gl_accvouch
    SET currency_code = 'HKD',
        cexch_name = '港币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'HKD'
       OR TRIM(cexch_name) IN ('港币', '港元');

    UPDATE gl_accvouch
    SET currency_code = 'JPY',
        cexch_name = '日元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'JPY'
       OR TRIM(cexch_name) IN ('日元', '日圆');

    UPDATE gl_accvouch
    SET currency_code = 'GBP',
        cexch_name = '英镑'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'GBP'
       OR TRIM(cexch_name) = '英镑';

    UPDATE gl_accsum
    SET currency_code = 'CNY',
        cexch_name = '人民币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) IN ('CNY', 'RMB')
       OR TRIM(cexch_name) = '人民币';

    UPDATE gl_accsum
    SET currency_code = 'USD',
        cexch_name = '美元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'USD'
       OR TRIM(cexch_name) = '美元';

    UPDATE gl_accsum
    SET currency_code = 'EUR',
        cexch_name = '欧元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'EUR'
       OR TRIM(cexch_name) = '欧元';

    UPDATE gl_accsum
    SET currency_code = 'HKD',
        cexch_name = '港币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'HKD'
       OR TRIM(cexch_name) IN ('港币', '港元');

    UPDATE gl_accsum
    SET currency_code = 'JPY',
        cexch_name = '日元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'JPY'
       OR TRIM(cexch_name) IN ('日元', '日圆');

    UPDATE gl_accsum
    SET currency_code = 'GBP',
        cexch_name = '英镑'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'GBP'
       OR TRIM(cexch_name) = '英镑';

    UPDATE gl_accass
    SET currency_code = 'CNY',
        cexch_name = '人民币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) IN ('CNY', 'RMB')
       OR TRIM(cexch_name) = '人民币';

    UPDATE gl_accass
    SET currency_code = 'USD',
        cexch_name = '美元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'USD'
       OR TRIM(cexch_name) = '美元';

    UPDATE gl_accass
    SET currency_code = 'EUR',
        cexch_name = '欧元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'EUR'
       OR TRIM(cexch_name) = '欧元';

    UPDATE gl_accass
    SET currency_code = 'HKD',
        cexch_name = '港币'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'HKD'
       OR TRIM(cexch_name) IN ('港币', '港元');

    UPDATE gl_accass
    SET currency_code = 'JPY',
        cexch_name = '日元'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'JPY'
       OR TRIM(cexch_name) IN ('日元', '日圆');

    UPDATE gl_accass
    SET currency_code = 'GBP',
        cexch_name = '英镑'
    WHERE UPPER(TRIM(COALESCE(currency_code, cexch_name))) = 'GBP'
       OR TRIM(cexch_name) = '英镑';

    SELECT COUNT(*)
      INTO unresolved_gl_accvouch_currency_rows
    FROM gl_accvouch
    WHERE (TRIM(cexch_name) IS NOT NULL OR TRIM(currency_code) IS NOT NULL)
      AND NOT (
            (UPPER(TRIM(currency_code)) = 'CNY' AND TRIM(cexch_name) = '人民币')
         OR (UPPER(TRIM(currency_code)) = 'USD' AND TRIM(cexch_name) = '美元')
         OR (UPPER(TRIM(currency_code)) = 'EUR' AND TRIM(cexch_name) = '欧元')
         OR (UPPER(TRIM(currency_code)) = 'HKD' AND TRIM(cexch_name) = '港币')
         OR (UPPER(TRIM(currency_code)) = 'JPY' AND TRIM(cexch_name) = '日元')
         OR (UPPER(TRIM(currency_code)) = 'GBP' AND TRIM(cexch_name) = '英镑')
      );

    IF unresolved_gl_accvouch_currency_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accvouch 存在无法自动映射的旧币种值，请先专项处理';
    END IF;

    SELECT COUNT(*)
      INTO unresolved_gl_accsum_currency_rows
    FROM gl_accsum
    WHERE (TRIM(cexch_name) IS NOT NULL OR TRIM(currency_code) IS NOT NULL)
      AND NOT (
            (UPPER(TRIM(currency_code)) = 'CNY' AND TRIM(cexch_name) = '人民币')
         OR (UPPER(TRIM(currency_code)) = 'USD' AND TRIM(cexch_name) = '美元')
         OR (UPPER(TRIM(currency_code)) = 'EUR' AND TRIM(cexch_name) = '欧元')
         OR (UPPER(TRIM(currency_code)) = 'HKD' AND TRIM(cexch_name) = '港币')
         OR (UPPER(TRIM(currency_code)) = 'JPY' AND TRIM(cexch_name) = '日元')
         OR (UPPER(TRIM(currency_code)) = 'GBP' AND TRIM(cexch_name) = '英镑')
      );

    IF unresolved_gl_accsum_currency_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accsum 存在无法自动映射的旧币种值，请先专项处理';
    END IF;

    SELECT COUNT(*)
      INTO unresolved_gl_accass_currency_rows
    FROM gl_accass
    WHERE (TRIM(cexch_name) IS NOT NULL OR TRIM(currency_code) IS NOT NULL)
      AND NOT (
            (UPPER(TRIM(currency_code)) = 'CNY' AND TRIM(cexch_name) = '人民币')
         OR (UPPER(TRIM(currency_code)) = 'USD' AND TRIM(cexch_name) = '美元')
         OR (UPPER(TRIM(currency_code)) = 'EUR' AND TRIM(cexch_name) = '欧元')
         OR (UPPER(TRIM(currency_code)) = 'HKD' AND TRIM(cexch_name) = '港币')
         OR (UPPER(TRIM(currency_code)) = 'JPY' AND TRIM(cexch_name) = '日元')
         OR (UPPER(TRIM(currency_code)) = 'GBP' AND TRIM(cexch_name) = '英镑')
      );

    IF unresolved_gl_accass_currency_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'gl_accass 存在无法自动映射的旧币种值，请先专项处理';
    END IF;
END $$

CALL sp_migrate_gl_year_currency_semantics() $$
DROP PROCEDURE IF EXISTS sp_migrate_gl_year_currency_semantics $$

DELIMITER ;
