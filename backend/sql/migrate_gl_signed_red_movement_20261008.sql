-- Correct posted red-letter movement without changing vouchers or signed balances.
-- Stop ledger writes, back up the target database, and verify on an isolated copy first.
-- Requires migrate_gl_debit_signed_balance_20261002.sql to have completed.
SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_gl_signed_red_movement_20261008 $$
CREATE PROCEDURE migrate_gl_signed_red_movement_20261008()
main: BEGIN
    DECLARE issue_count BIGINT DEFAULT 0;
    DECLARE sum_count BIGINT DEFAULT 0;
    DECLARE ass_count BIGINT DEFAULT 0;
    DECLARE current_level INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_gl_balance_migration'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: prior balance migration marker table is missing';
    END IF;
    IF EXISTS (SELECT 1 FROM fin_gl_balance_migration
               WHERE migration_key = 'signed_red_movement_20261008') THEN
        COMMIT;
        LEAVE main;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM fin_gl_balance_migration
                   WHERE migration_key = 'debit_signed_balance_20261002') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: debit-signed balance migration is required';
    END IF;

    SELECT COUNT(*) INTO sum_count FROM gl_accsum;
    SELECT COUNT(*) INTO ass_count FROM gl_accass;

    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_sum;
    CREATE TEMPORARY TABLE tmp_gl_red_sum AS
    SELECT v.company_id, v.iyear, v.iperiod, v.ccode, v.currency_code,
           SUM(GREATEST(COALESCE(v.md, 0), 0) + GREATEST(-COALESCE(v.mc, 0), 0)) AS old_md,
           SUM(GREATEST(COALESCE(v.mc, 0), 0) + GREATEST(-COALESCE(v.md, 0), 0)) AS old_mc,
           SUM(GREATEST(COALESCE(v.md_f, 0), 0) + GREATEST(-COALESCE(v.mc_f, 0), 0)) AS old_md_f,
           SUM(GREATEST(COALESCE(v.mc_f, 0), 0) + GREATEST(-COALESCE(v.md_f, 0), 0)) AS old_mc_f,
           SUM(COALESCE(v.md, 0)) AS signed_md,
           SUM(COALESCE(v.mc, 0)) AS signed_mc,
           SUM(COALESCE(v.md_f, 0)) AS signed_md_f,
           SUM(COALESCE(v.mc_f, 0)) AS signed_mc_f
    FROM gl_accvouch v
    WHERE v.ibook = 1
    GROUP BY v.company_id, v.iyear, v.iperiod, v.ccode, v.currency_code
    HAVING SUM(CASE WHEN COALESCE(v.md, 0) < 0 OR COALESCE(v.mc, 0) < 0
                         OR COALESCE(v.md_f, 0) < 0 OR COALESCE(v.mc_f, 0) < 0
                    THEN 1 ELSE 0 END) > 0;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_sum r
    LEFT JOIN fin_account_subject subject ON subject.company_id = r.company_id
         AND subject.subject_code = r.ccode
    WHERE r.company_id IS NULL OR r.iyear IS NULL OR r.iperiod NOT BETWEEN 1 AND 12
       OR r.ccode IS NULL OR r.currency_code IS NULL OR subject.id IS NULL
       OR EXISTS (SELECT 1 FROM fin_account_subject child
                  WHERE child.company_id = r.company_id AND child.parent_subject_code = r.ccode);
    IF issue_count > 0 THEN
        SELECT company_id, iyear, iperiod, ccode, currency_code FROM tmp_gl_red_sum LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: invalid voucher key or non-leaf subject';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT s.company_id, s.iyear, s.iperiod, s.ccode, s.currency_code
        FROM gl_accsum s JOIN tmp_gl_red_sum r
          ON r.company_id = s.company_id AND r.iyear = s.iyear
         AND r.iperiod = s.iperiod AND r.ccode = s.ccode AND r.currency_code = s.currency_code
        GROUP BY s.company_id, s.iyear, s.iperiod, s.ccode, s.currency_code
        HAVING COUNT(*) <> 1
    ) duplicates;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: duplicate subject snapshot key';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_sum r
    LEFT JOIN gl_accsum s ON s.company_id = r.company_id AND s.iyear = r.iyear
         AND s.iperiod = r.iperiod AND s.ccode = r.ccode AND s.currency_code = r.currency_code
    WHERE s.i_id IS NULL OR ABS(COALESCE(s.md, 0) - r.old_md) > 0.005
       OR ABS(COALESCE(s.mc, 0) - r.old_mc) > 0.005
       OR ABS(COALESCE(s.md_f, 0) - r.old_md_f) > 0.005
       OR ABS(COALESCE(s.mc_f, 0) - r.old_mc_f) > 0.005
       OR ABS(COALESCE(s.me, 0) - COALESCE(s.mb, 0) - COALESCE(s.md, 0) + COALESCE(s.mc, 0)) > 0.005
       OR ABS(COALESCE(s.me_f, 0) - COALESCE(s.mb_f, 0) - COALESCE(s.md_f, 0) + COALESCE(s.mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SELECT r.company_id, r.iyear, r.iperiod, r.ccode, r.currency_code,
               s.md, s.mc, s.md_f, s.mc_f,
               r.old_md, r.old_mc, r.old_md_f, r.old_mc_f
        FROM tmp_gl_red_sum r LEFT JOIN gl_accsum s ON s.company_id = r.company_id
             AND s.iyear = r.iyear AND s.iperiod = r.iperiod
             AND s.ccode = r.ccode AND s.currency_code = r.currency_code
        WHERE s.i_id IS NULL OR ABS(COALESCE(s.md, 0) - r.old_md) > 0.005
           OR ABS(COALESCE(s.mc, 0) - r.old_mc) > 0.005
           OR ABS(COALESCE(s.md_f, 0) - r.old_md_f) > 0.005
           OR ABS(COALESCE(s.mc_f, 0) - r.old_mc_f) > 0.005 LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: subject snapshot differs from old posted movement';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_ass_keys;
    CREATE TEMPORARY TABLE tmp_gl_red_ass_keys AS
    SELECT DISTINCT v.company_id, v.iyear, v.iperiod, v.ccode, v.currency_code,
           COALESCE(NULLIF(TRIM(v.cdept_id), ''), '') AS cdept_id,
           COALESCE(NULLIF(TRIM(v.cperson_id), ''), '') AS cperson_id,
           COALESCE(NULLIF(TRIM(v.ccus_id), ''), '') AS ccus_id,
           COALESCE(NULLIF(TRIM(v.csup_id), ''), '') AS csup_id,
           COALESCE(NULLIF(TRIM(v.citem_class), ''), '') AS citem_class,
           COALESCE(NULLIF(TRIM(v.citem_id), ''), '') AS citem_id
    FROM gl_accvouch v
    WHERE v.ibook = 1
      AND (COALESCE(v.md, 0) < 0 OR COALESCE(v.mc, 0) < 0
           OR COALESCE(v.md_f, 0) < 0 OR COALESCE(v.mc_f, 0) < 0)
      AND (NULLIF(TRIM(v.cdept_id), '') IS NOT NULL
           OR NULLIF(TRIM(v.cperson_id), '') IS NOT NULL
           OR NULLIF(TRIM(v.ccus_id), '') IS NOT NULL
           OR NULLIF(TRIM(v.csup_id), '') IS NOT NULL
           OR NULLIF(TRIM(v.citem_class), '') IS NOT NULL
           OR NULLIF(TRIM(v.citem_id), '') IS NOT NULL);

    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_ass;
    CREATE TEMPORARY TABLE tmp_gl_red_ass AS
    SELECT k.company_id, k.iyear, k.iperiod, k.ccode, k.currency_code,
           k.cdept_id, k.cperson_id, k.ccus_id, k.csup_id, k.citem_class, k.citem_id,
           SUM(GREATEST(COALESCE(v.md, 0), 0) + GREATEST(-COALESCE(v.mc, 0), 0)) AS old_md,
           SUM(GREATEST(COALESCE(v.mc, 0), 0) + GREATEST(-COALESCE(v.md, 0), 0)) AS old_mc,
           SUM(GREATEST(COALESCE(v.md_f, 0), 0) + GREATEST(-COALESCE(v.mc_f, 0), 0)) AS old_md_f,
           SUM(GREATEST(COALESCE(v.mc_f, 0), 0) + GREATEST(-COALESCE(v.md_f, 0), 0)) AS old_mc_f,
           SUM(COALESCE(v.md, 0)) AS signed_md, SUM(COALESCE(v.mc, 0)) AS signed_mc,
           SUM(COALESCE(v.md_f, 0)) AS signed_md_f, SUM(COALESCE(v.mc_f, 0)) AS signed_mc_f
    FROM tmp_gl_red_ass_keys k JOIN gl_accvouch v
      ON v.company_id = k.company_id AND v.iyear = k.iyear AND v.iperiod = k.iperiod
     AND v.ccode = k.ccode AND v.currency_code = k.currency_code AND v.ibook = 1
     AND COALESCE(NULLIF(TRIM(v.cdept_id), ''), '') = k.cdept_id
     AND COALESCE(NULLIF(TRIM(v.cperson_id), ''), '') = k.cperson_id
     AND COALESCE(NULLIF(TRIM(v.ccus_id), ''), '') = k.ccus_id
     AND COALESCE(NULLIF(TRIM(v.csup_id), ''), '') = k.csup_id
     AND COALESCE(NULLIF(TRIM(v.citem_class), ''), '') = k.citem_class
     AND COALESCE(NULLIF(TRIM(v.citem_id), ''), '') = k.citem_id
    GROUP BY k.company_id, k.iyear, k.iperiod, k.ccode, k.currency_code,
             k.cdept_id, k.cperson_id, k.ccus_id, k.csup_id, k.citem_class, k.citem_id;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT a.company_id, a.iyear, a.iperiod, a.ccode, a.currency_code,
               COALESCE(NULLIF(TRIM(a.cdept_id), ''), ''),
               COALESCE(NULLIF(TRIM(a.cperson_id), ''), ''),
               COALESCE(NULLIF(TRIM(a.ccus_id), ''), ''),
               COALESCE(NULLIF(TRIM(a.csup_id), ''), ''),
               COALESCE(NULLIF(TRIM(a.citem_class), ''), ''),
               COALESCE(NULLIF(TRIM(a.citem_id), ''), '')
        FROM gl_accass a JOIN tmp_gl_red_sum r ON r.company_id = a.company_id
          AND r.iyear = a.iyear AND r.iperiod = a.iperiod
          AND r.ccode = a.ccode AND r.currency_code = a.currency_code
        GROUP BY a.company_id, a.iyear, a.iperiod, a.ccode, a.currency_code,
                 COALESCE(NULLIF(TRIM(a.cdept_id), ''), ''),
                 COALESCE(NULLIF(TRIM(a.cperson_id), ''), ''),
                 COALESCE(NULLIF(TRIM(a.ccus_id), ''), ''),
                 COALESCE(NULLIF(TRIM(a.csup_id), ''), ''),
                 COALESCE(NULLIF(TRIM(a.citem_class), ''), ''),
                 COALESCE(NULLIF(TRIM(a.citem_id), ''), '')
        HAVING COUNT(*) > 1
    ) duplicates;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: duplicate auxiliary snapshot key';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_ass r
    LEFT JOIN gl_accass a ON a.company_id = r.company_id AND a.iyear = r.iyear
         AND a.iperiod = r.iperiod AND a.ccode = r.ccode AND a.currency_code = r.currency_code
         AND COALESCE(NULLIF(TRIM(a.cdept_id), ''), '') = r.cdept_id
         AND COALESCE(NULLIF(TRIM(a.cperson_id), ''), '') = r.cperson_id
         AND COALESCE(NULLIF(TRIM(a.ccus_id), ''), '') = r.ccus_id
         AND COALESCE(NULLIF(TRIM(a.csup_id), ''), '') = r.csup_id
         AND COALESCE(NULLIF(TRIM(a.citem_class), ''), '') = r.citem_class
         AND COALESCE(NULLIF(TRIM(a.citem_id), ''), '') = r.citem_id
    WHERE a.i_id IS NULL OR ABS(COALESCE(a.md, 0) - r.old_md) > 0.005
       OR ABS(COALESCE(a.mc, 0) - r.old_mc) > 0.005
       OR ABS(COALESCE(a.md_f, 0) - r.old_md_f) > 0.005
       OR ABS(COALESCE(a.mc_f, 0) - r.old_mc_f) > 0.005
       OR ABS(COALESCE(a.me, 0) - COALESCE(a.mb, 0) - COALESCE(a.md, 0) + COALESCE(a.mc, 0)) > 0.005
       OR ABS(COALESCE(a.me_f, 0) - COALESCE(a.mb_f, 0) - COALESCE(a.md_f, 0) + COALESCE(a.mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SELECT r.company_id, r.iyear, r.iperiod, r.ccode, r.currency_code,
               r.cdept_id, r.cperson_id, r.ccus_id, r.csup_id, r.citem_class, r.citem_id,
               a.md, a.mc, a.md_f, a.mc_f,
               r.old_md, r.old_mc, r.old_md_f, r.old_mc_f
        FROM tmp_gl_red_ass r LEFT JOIN gl_accass a ON a.company_id = r.company_id
             AND a.iyear = r.iyear AND a.iperiod = r.iperiod
             AND a.ccode = r.ccode AND a.currency_code = r.currency_code
             AND COALESCE(NULLIF(TRIM(a.cdept_id), ''), '') = r.cdept_id
             AND COALESCE(NULLIF(TRIM(a.cperson_id), ''), '') = r.cperson_id
             AND COALESCE(NULLIF(TRIM(a.ccus_id), ''), '') = r.ccus_id
             AND COALESCE(NULLIF(TRIM(a.csup_id), ''), '') = r.csup_id
             AND COALESCE(NULLIF(TRIM(a.citem_class), ''), '') = r.citem_class
             AND COALESCE(NULLIF(TRIM(a.citem_id), ''), '') = r.citem_id
        WHERE a.i_id IS NULL OR ABS(COALESCE(a.md, 0) - r.old_md) > 0.005
           OR ABS(COALESCE(a.mc, 0) - r.old_mc) > 0.005
           OR ABS(COALESCE(a.md_f, 0) - r.old_md_f) > 0.005
           OR ABS(COALESCE(a.mc_f, 0) - r.old_mc_f) > 0.005 LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: auxiliary snapshot differs from old posted movement';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_period;
    CREATE TEMPORARY TABLE tmp_gl_red_period AS
    SELECT DISTINCT company_id, iyear, iperiod, currency_code FROM tmp_gl_red_sum;

    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_aux_totals;
    CREATE TEMPORARY TABLE tmp_gl_red_aux_totals AS
    SELECT a.company_id, a.iyear, a.iperiod, a.ccode, a.currency_code,
           SUM(COALESCE(a.md, 0)) AS md, SUM(COALESCE(a.mc, 0)) AS mc,
           SUM(COALESCE(a.md_f, 0)) AS md_f, SUM(COALESCE(a.mc_f, 0)) AS mc_f
    FROM gl_accass a JOIN tmp_gl_red_sum r ON r.company_id = a.company_id
      AND r.iyear = a.iyear AND r.iperiod = a.iperiod
      AND r.ccode = a.ccode AND r.currency_code = a.currency_code
    GROUP BY a.company_id, a.iyear, a.iperiod, a.ccode, a.currency_code;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_aux_totals totals
    JOIN gl_accsum s ON s.company_id = totals.company_id
      AND s.iyear = totals.iyear AND s.iperiod = totals.iperiod
      AND s.ccode = totals.ccode AND s.currency_code = totals.currency_code
    WHERE ABS(COALESCE(s.md, 0) - totals.md) > 0.005
       OR ABS(COALESCE(s.mc, 0) - totals.mc) > 0.005
       OR ABS(COALESCE(s.md_f, 0) - totals.md_f) > 0.005
       OR ABS(COALESCE(s.mc_f, 0) - totals.mc_f) > 0.005;
    IF issue_count > 0 THEN
        SELECT totals.company_id, totals.iyear, totals.iperiod, totals.ccode, totals.currency_code,
               s.md, s.mc, s.md_f, s.mc_f,
               totals.md AS aux_md, totals.mc AS aux_mc,
               totals.md_f AS aux_md_f, totals.mc_f AS aux_mc_f
        FROM tmp_gl_red_aux_totals totals JOIN gl_accsum s ON s.company_id = totals.company_id
          AND s.iyear = totals.iyear AND s.iperiod = totals.iperiod
          AND s.ccode = totals.ccode AND s.currency_code = totals.currency_code
        WHERE ABS(COALESCE(s.md, 0) - totals.md) > 0.005
           OR ABS(COALESCE(s.mc, 0) - totals.mc) > 0.005
           OR ABS(COALESCE(s.md_f, 0) - totals.md_f) > 0.005
           OR ABS(COALESCE(s.mc_f, 0) - totals.mc_f) > 0.005 LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: auxiliary total mismatch';
    END IF;

    -- Fail before writing if any affected period has inconsistent hierarchy.
    DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_parent_before;
    CREATE TEMPORARY TABLE tmp_gl_red_parent_before AS
    SELECT child.company_id, child.iyear, child.iperiod, child.currency_code,
           subject.parent_subject_code AS parent_code,
           SUM(COALESCE(child.md, 0)) AS md, SUM(COALESCE(child.mc, 0)) AS mc,
           SUM(COALESCE(child.md_f, 0)) AS md_f, SUM(COALESCE(child.mc_f, 0)) AS mc_f
    FROM gl_accsum child JOIN fin_account_subject subject
      ON subject.company_id = child.company_id AND subject.subject_code = child.ccode
    JOIN tmp_gl_red_period period_key ON period_key.company_id = child.company_id
      AND period_key.iyear = child.iyear AND period_key.iperiod = child.iperiod
      AND period_key.currency_code = child.currency_code
    WHERE subject.parent_subject_code IS NOT NULL
    GROUP BY child.company_id, child.iyear, child.iperiod, child.currency_code,
             subject.parent_subject_code;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_parent_before totals
    LEFT JOIN gl_accsum parent ON parent.company_id = totals.company_id
         AND parent.iyear = totals.iyear AND parent.iperiod = totals.iperiod
         AND parent.currency_code = totals.currency_code AND parent.ccode = totals.parent_code
    WHERE parent.i_id IS NULL OR ABS(COALESCE(parent.md, 0) - totals.md) > 0.005
       OR ABS(COALESCE(parent.mc, 0) - totals.mc) > 0.005
       OR ABS(COALESCE(parent.md_f, 0) - totals.md_f) > 0.005
       OR ABS(COALESCE(parent.mc_f, 0) - totals.mc_f) > 0.005;
    IF issue_count > 0 THEN
        SELECT totals.company_id, totals.iyear, totals.iperiod, totals.parent_code,
               totals.currency_code, parent.md, parent.mc, parent.md_f, parent.mc_f,
               totals.md AS child_md, totals.mc AS child_mc,
               totals.md_f AS child_md_f, totals.mc_f AS child_mc_f
        FROM tmp_gl_red_parent_before totals LEFT JOIN gl_accsum parent
          ON parent.company_id = totals.company_id AND parent.iyear = totals.iyear
          AND parent.iperiod = totals.iperiod AND parent.currency_code = totals.currency_code
          AND parent.ccode = totals.parent_code
        WHERE parent.i_id IS NULL OR ABS(COALESCE(parent.md, 0) - totals.md) > 0.005
           OR ABS(COALESCE(parent.mc, 0) - totals.mc) > 0.005
           OR ABS(COALESCE(parent.md_f, 0) - totals.md_f) > 0.005
           OR ABS(COALESCE(parent.mc_f, 0) - totals.mc_f) > 0.005 LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: parent snapshot mismatch';
    END IF;

    UPDATE gl_accsum s JOIN tmp_gl_red_sum r ON r.company_id = s.company_id
         AND r.iyear = s.iyear AND r.iperiod = s.iperiod
         AND r.ccode = s.ccode AND r.currency_code = s.currency_code
    SET s.md = r.signed_md, s.mc = r.signed_mc,
        s.md_f = r.signed_md_f, s.mc_f = r.signed_mc_f;

    UPDATE gl_accass a JOIN tmp_gl_red_ass r ON r.company_id = a.company_id
         AND r.iyear = a.iyear AND r.iperiod = a.iperiod
         AND r.ccode = a.ccode AND r.currency_code = a.currency_code
         AND COALESCE(NULLIF(TRIM(a.cdept_id), ''), '') = r.cdept_id
         AND COALESCE(NULLIF(TRIM(a.cperson_id), ''), '') = r.cperson_id
         AND COALESCE(NULLIF(TRIM(a.ccus_id), ''), '') = r.ccus_id
         AND COALESCE(NULLIF(TRIM(a.csup_id), ''), '') = r.csup_id
         AND COALESCE(NULLIF(TRIM(a.citem_class), ''), '') = r.citem_class
         AND COALESCE(NULLIF(TRIM(a.citem_id), ''), '') = r.citem_id
    SET a.md = r.signed_md, a.mc = r.signed_mc,
        a.md_f = r.signed_md_f, a.mc_f = r.signed_mc_f;

    SELECT COALESCE(MAX(subject_level), 1) - 1 INTO current_level FROM fin_account_subject;
    WHILE current_level >= 1 DO
        DROP TEMPORARY TABLE IF EXISTS tmp_gl_red_parent_totals;
        CREATE TEMPORARY TABLE tmp_gl_red_parent_totals AS
        SELECT child.company_id, child.iyear, child.iperiod, child.currency_code,
               subject.parent_subject_code AS parent_code,
               SUM(COALESCE(child.md, 0)) AS md, SUM(COALESCE(child.mc, 0)) AS mc,
               SUM(COALESCE(child.md_f, 0)) AS md_f, SUM(COALESCE(child.mc_f, 0)) AS mc_f
        FROM gl_accsum child JOIN fin_account_subject subject
          ON subject.company_id = child.company_id AND subject.subject_code = child.ccode
        JOIN tmp_gl_red_period period_key ON period_key.company_id = child.company_id
          AND period_key.iyear = child.iyear AND period_key.iperiod = child.iperiod
          AND period_key.currency_code = child.currency_code
        WHERE subject.subject_level = current_level + 1
          AND subject.parent_subject_code IS NOT NULL
        GROUP BY child.company_id, child.iyear, child.iperiod, child.currency_code,
                 subject.parent_subject_code;
        UPDATE gl_accsum parent JOIN tmp_gl_red_parent_totals totals
          ON totals.company_id = parent.company_id AND totals.iyear = parent.iyear
         AND totals.iperiod = parent.iperiod AND totals.currency_code = parent.currency_code
         AND totals.parent_code = parent.ccode
        SET parent.md = totals.md, parent.mc = totals.mc,
            parent.md_f = totals.md_f, parent.mc_f = totals.mc_f;
        SET current_level = current_level - 1;
    END WHILE;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_sum r
    JOIN gl_accsum s ON s.company_id = r.company_id AND s.iyear = r.iyear
      AND s.iperiod = r.iperiod AND s.ccode = r.ccode AND s.currency_code = r.currency_code
    WHERE ABS(COALESCE(s.md, 0) - r.signed_md) > 0.005
       OR ABS(COALESCE(s.mc, 0) - r.signed_mc) > 0.005
       OR ABS(COALESCE(s.md_f, 0) - r.signed_md_f) > 0.005
       OR ABS(COALESCE(s.mc_f, 0) - r.signed_mc_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: subject post-update mismatch';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_red_ass r
    JOIN gl_accass a ON a.company_id = r.company_id AND a.iyear = r.iyear
      AND a.iperiod = r.iperiod AND a.ccode = r.ccode AND a.currency_code = r.currency_code
      AND COALESCE(NULLIF(TRIM(a.cdept_id), ''), '') = r.cdept_id
      AND COALESCE(NULLIF(TRIM(a.cperson_id), ''), '') = r.cperson_id
      AND COALESCE(NULLIF(TRIM(a.ccus_id), ''), '') = r.ccus_id
      AND COALESCE(NULLIF(TRIM(a.csup_id), ''), '') = r.csup_id
      AND COALESCE(NULLIF(TRIM(a.citem_class), ''), '') = r.citem_class
      AND COALESCE(NULLIF(TRIM(a.citem_id), ''), '') = r.citem_id
    WHERE ABS(COALESCE(a.md, 0) - r.signed_md) > 0.005
       OR ABS(COALESCE(a.mc, 0) - r.signed_mc) > 0.005
       OR ABS(COALESCE(a.md_f, 0) - r.signed_md_f) > 0.005
       OR ABS(COALESCE(a.mc_f, 0) - r.signed_mc_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: auxiliary post-update mismatch';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT child.company_id, child.iyear, child.iperiod, child.currency_code,
               subject.parent_subject_code AS parent_code,
               SUM(COALESCE(child.md, 0)) AS md, SUM(COALESCE(child.mc, 0)) AS mc,
               SUM(COALESCE(child.md_f, 0)) AS md_f, SUM(COALESCE(child.mc_f, 0)) AS mc_f
        FROM gl_accsum child JOIN fin_account_subject subject
          ON subject.company_id = child.company_id AND subject.subject_code = child.ccode
        JOIN tmp_gl_red_period period_key ON period_key.company_id = child.company_id
          AND period_key.iyear = child.iyear AND period_key.iperiod = child.iperiod
          AND period_key.currency_code = child.currency_code
        WHERE subject.parent_subject_code IS NOT NULL
        GROUP BY child.company_id, child.iyear, child.iperiod, child.currency_code,
                 subject.parent_subject_code
    ) totals
    LEFT JOIN gl_accsum parent ON parent.company_id = totals.company_id
      AND parent.iyear = totals.iyear AND parent.iperiod = totals.iperiod
      AND parent.currency_code = totals.currency_code AND parent.ccode = totals.parent_code
    WHERE parent.i_id IS NULL OR ABS(COALESCE(parent.md, 0) - totals.md) > 0.005
       OR ABS(COALESCE(parent.mc, 0) - totals.mc) > 0.005
       OR ABS(COALESCE(parent.md_f, 0) - totals.md_f) > 0.005
       OR ABS(COALESCE(parent.mc_f, 0) - totals.mc_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: parent post-update mismatch';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM gl_accsum s JOIN tmp_gl_red_period period_key
      ON period_key.company_id = s.company_id AND period_key.iyear = s.iyear
     AND period_key.iperiod = s.iperiod AND period_key.currency_code = s.currency_code
    WHERE ABS(COALESCE(s.me, 0) - COALESCE(s.mb, 0) - COALESCE(s.md, 0) + COALESCE(s.mc, 0)) > 0.005
       OR ABS(COALESCE(s.me_f, 0) - COALESCE(s.mb_f, 0) - COALESCE(s.md_f, 0) + COALESCE(s.mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: balance arithmetic changed';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM gl_accass a JOIN tmp_gl_red_period period_key
      ON period_key.company_id = a.company_id AND period_key.iyear = a.iyear
     AND period_key.iperiod = a.iperiod AND period_key.currency_code = a.currency_code
    WHERE ABS(COALESCE(a.me, 0) - COALESCE(a.mb, 0) - COALESCE(a.md, 0) + COALESCE(a.mc, 0)) > 0.005
       OR ABS(COALESCE(a.me_f, 0) - COALESCE(a.mb_f, 0) - COALESCE(a.md_f, 0) + COALESCE(a.mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: auxiliary balance arithmetic changed';
    END IF;

    IF (SELECT COUNT(*) FROM gl_accsum) <> sum_count
       OR (SELECT COUNT(*) FROM gl_accass) <> ass_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL red movement: snapshot row count changed';
    END IF;

    INSERT INTO fin_gl_balance_migration (migration_key, applied_at, accsum_rows, accass_rows)
    VALUES ('signed_red_movement_20261008', NOW(), sum_count, ass_count);
    COMMIT;
END $$
CALL migrate_gl_signed_red_movement_20261008() $$
DROP PROCEDURE IF EXISTS migrate_gl_signed_red_movement_20261008 $$
DELIMITER ;

SELECT migration_key, applied_at, accsum_rows, accass_rows
FROM fin_gl_balance_migration WHERE migration_key = 'signed_red_movement_20261008';
