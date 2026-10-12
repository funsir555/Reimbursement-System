-- Run against the selected FinEx database with ledger writes stopped and a verified backup.
-- This migration changes gl_accsum/gl_accass mb and me to debit-positive, credit-negative.
-- It never changes voucher amounts or quantity balances. A migration marker prevents replay.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fin_gl_balance_migration (
    migration_key VARCHAR(80) NOT NULL PRIMARY KEY,
    applied_at DATETIME NOT NULL,
    accsum_rows BIGINT NOT NULL,
    accass_rows BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_gl_debit_signed_balance_20261002 $$
CREATE PROCEDURE migrate_gl_debit_signed_balance_20261002()
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
    IF EXISTS (SELECT 1 FROM fin_gl_balance_migration
               WHERE migration_key = 'debit_signed_balance_20261002') THEN
        COMMIT;
        LEAVE main;
    END IF;

    SELECT COUNT(*) INTO sum_count FROM gl_accsum;
    SELECT COUNT(*) INTO ass_count FROM gl_accass;

    -- A missing subject, unknown usual direction, or invalid period makes the old sign ambiguous.
    SELECT COUNT(*) INTO issue_count
    FROM (
        SELECT s.i_id FROM gl_accsum s
        LEFT JOIN fin_account_subject subj ON subj.company_id = s.company_id AND subj.subject_code = s.ccode
        WHERE subj.id IS NULL OR UPPER(TRIM(subj.balance_direction)) NOT IN ('DEBIT', 'CREDIT')
           OR s.iyear IS NULL OR s.iperiod NOT BETWEEN 1 AND 12 OR s.currency_code IS NULL
        UNION ALL
        SELECT a.i_id FROM gl_accass a
        LEFT JOIN fin_account_subject subj ON subj.company_id = a.company_id AND subj.subject_code = a.ccode
        WHERE subj.id IS NULL OR UPPER(TRIM(subj.balance_direction)) NOT IN ('DEBIT', 'CREDIT')
           OR a.iyear IS NULL OR a.iperiod NOT BETWEEN 1 AND 12 OR a.currency_code IS NULL
    ) invalid_rows;
    IF issue_count > 0 THEN
        SELECT 'gl_accsum' AS source_table, s.company_id, s.iyear, s.iperiod, s.ccode, s.currency_code
        FROM gl_accsum s LEFT JOIN fin_account_subject subj
          ON subj.company_id = s.company_id AND subj.subject_code = s.ccode
        WHERE subj.id IS NULL OR UPPER(TRIM(subj.balance_direction)) NOT IN ('DEBIT', 'CREDIT')
           OR s.iyear IS NULL OR s.iperiod NOT BETWEEN 1 AND 12 OR s.currency_code IS NULL
        LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: missing subject, direction, period or currency';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT company_id, iyear, iperiod, ccode, currency_code
        FROM gl_accsum GROUP BY company_id, iyear, iperiod, ccode, currency_code HAVING COUNT(*) > 1
    ) duplicates;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: duplicate subject snapshot key';
    END IF;
    SELECT COUNT(*) INTO issue_count FROM (
        SELECT company_id, iyear, iperiod, ccode, currency_code,
               cdept_id, cperson_id, ccus_id, csup_id, citem_class, citem_id
        FROM gl_accass
        GROUP BY company_id, iyear, iperiod, ccode, currency_code,
                 cdept_id, cperson_id, ccus_id, csup_id, citem_class, citem_id
        HAVING COUNT(*) > 1
    ) duplicates;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: duplicate auxiliary snapshot key';
    END IF;

    -- Old rows must still satisfy their stored arithmetic; otherwise the opening cannot be replayed safely.
    SELECT COUNT(*) INTO issue_count FROM gl_accsum
    WHERE ABS(COALESCE(me, 0) - COALESCE(mb, 0) - COALESCE(md, 0) + COALESCE(mc, 0)) > 0.005
       OR ABS(COALESCE(me_f, 0) - COALESCE(mb_f, 0) - COALESCE(md_f, 0) + COALESCE(mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SELECT company_id, iyear, iperiod, ccode, currency_code, mb, md, mc, me,
               COALESCE(mb, 0) + COALESCE(md, 0) - COALESCE(mc, 0) AS expected_me
        FROM gl_accsum
        WHERE ABS(COALESCE(me, 0) - COALESCE(mb, 0) - COALESCE(md, 0) + COALESCE(mc, 0)) > 0.005
           OR ABS(COALESCE(me_f, 0) - COALESCE(mb_f, 0) - COALESCE(md_f, 0) + COALESCE(mc_f, 0)) > 0.005
        LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: gl_accsum arithmetic mismatch';
    END IF;
    SELECT COUNT(*) INTO issue_count FROM gl_accass
    WHERE ABS(COALESCE(me, 0) - COALESCE(mb, 0) - COALESCE(md, 0) + COALESCE(mc, 0)) > 0.005
       OR ABS(COALESCE(me_f, 0) - COALESCE(mb_f, 0) - COALESCE(md_f, 0) + COALESCE(mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SELECT company_id, iyear, iperiod, ccode, currency_code, cdept_id, cperson_id,
               ccus_id, csup_id, citem_class, citem_id, mb, md, mc, me,
               COALESCE(mb, 0) + COALESCE(md, 0) - COALESCE(mc, 0) AS expected_me
        FROM gl_accass
        WHERE ABS(COALESCE(me, 0) - COALESCE(mb, 0) - COALESCE(md, 0) + COALESCE(mc, 0)) > 0.005
           OR ABS(COALESCE(me_f, 0) - COALESCE(mb_f, 0) - COALESCE(md_f, 0) + COALESCE(mc_f, 0)) > 0.005
        LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: gl_accass arithmetic mismatch';
    END IF;

    -- Parent snapshots must exist for every child snapshot so a bottom-up rebuild is complete.
    SELECT COUNT(*) INTO issue_count
    FROM gl_accsum child
    JOIN fin_account_subject subj ON subj.company_id = child.company_id AND subj.subject_code = child.ccode
    LEFT JOIN gl_accsum parent ON parent.company_id = child.company_id
         AND parent.iyear = child.iyear AND parent.iperiod = child.iperiod
         AND parent.ccode = subj.parent_subject_code AND parent.currency_code = child.currency_code
    WHERE subj.parent_subject_code IS NOT NULL AND parent.i_id IS NULL;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: child snapshot has no parent snapshot';
    END IF;

    CREATE TEMPORARY TABLE tmp_gl_sum_history AS
    SELECT s.i_id,
           subj.leaf_flag,
           subj.subject_level,
           UPPER(TRIM(subj.balance_direction)) AS usual_direction,
           s.company_id, s.iyear, s.iperiod, s.ccode, s.currency_code,
           s.mb, s.mb_f, s.md, s.mc, s.md_f, s.mc_f, s.me, s.me_f,
           ROW_NUMBER() OVER w AS period_number,
           LAG(s.me) OVER w AS previous_end,
           LAG(s.me_f) OVER w AS previous_end_f,
           FIRST_VALUE(COALESCE(s.mb, 0)) OVER w AS original_begin,
           FIRST_VALUE(COALESCE(s.mb_f, 0)) OVER w AS original_begin_f,
           COALESCE(SUM(COALESCE(s.md, 0) - COALESCE(s.mc, 0))
               OVER (PARTITION BY s.company_id, s.iyear, s.ccode, s.currency_code
                     ORDER BY s.iperiod, s.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS movement_before,
           SUM(COALESCE(s.md, 0) - COALESCE(s.mc, 0)) OVER w AS movement_through,
           COALESCE(SUM(COALESCE(s.md_f, 0) - COALESCE(s.mc_f, 0))
               OVER (PARTITION BY s.company_id, s.iyear, s.ccode, s.currency_code
                     ORDER BY s.iperiod, s.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS movement_before_f,
           SUM(COALESCE(s.md_f, 0) - COALESCE(s.mc_f, 0)) OVER w AS movement_through_f
    FROM gl_accsum s
    JOIN fin_account_subject subj ON subj.company_id = s.company_id AND subj.subject_code = s.ccode
    WINDOW w AS (PARTITION BY s.company_id, s.iyear, s.ccode, s.currency_code
                 ORDER BY s.iperiod, s.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW);

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_sum_history
    WHERE leaf_flag = 1 AND period_number > 1 AND (ABS(COALESCE(mb, 0) - COALESCE(previous_end, 0)) > 0.005
                               OR ABS(COALESCE(mb_f, 0) - COALESCE(previous_end_f, 0)) > 0.005);
    IF issue_count > 0 THEN
        SELECT company_id, iyear, iperiod, ccode, currency_code, mb, previous_end,
               mb_f, previous_end_f
        FROM tmp_gl_sum_history
        WHERE leaf_flag = 1 AND period_number > 1
          AND (ABS(COALESCE(mb, 0) - COALESCE(previous_end, 0)) > 0.005
               OR ABS(COALESCE(mb_f, 0) - COALESCE(previous_end_f, 0)) > 0.005)
        LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: subject period continuity mismatch';
    END IF;

    CREATE TEMPORARY TABLE tmp_gl_ass_history AS
    SELECT a.i_id, UPPER(TRIM(subj.balance_direction)) AS usual_direction,
           a.company_id, a.iyear, a.iperiod, a.ccode, a.currency_code,
           a.cdept_id, a.cperson_id, a.ccus_id, a.csup_id, a.citem_class, a.citem_id,
           a.mb, a.mb_f, a.md, a.mc, a.md_f, a.mc_f, a.me, a.me_f,
           ROW_NUMBER() OVER w AS period_number,
           LAG(a.me) OVER w AS previous_end,
           LAG(a.me_f) OVER w AS previous_end_f,
           FIRST_VALUE(COALESCE(a.mb, 0)) OVER w AS original_begin,
           FIRST_VALUE(COALESCE(a.mb_f, 0)) OVER w AS original_begin_f,
           COALESCE(SUM(COALESCE(a.md, 0) - COALESCE(a.mc, 0))
               OVER (PARTITION BY a.company_id, a.iyear, a.ccode, a.currency_code,
                     a.cdept_id, a.cperson_id, a.ccus_id, a.csup_id, a.citem_class, a.citem_id
                     ORDER BY a.iperiod, a.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS movement_before,
           SUM(COALESCE(a.md, 0) - COALESCE(a.mc, 0)) OVER w AS movement_through,
           COALESCE(SUM(COALESCE(a.md_f, 0) - COALESCE(a.mc_f, 0))
               OVER (PARTITION BY a.company_id, a.iyear, a.ccode, a.currency_code,
                     a.cdept_id, a.cperson_id, a.ccus_id, a.csup_id, a.citem_class, a.citem_id
                     ORDER BY a.iperiod, a.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS movement_before_f,
           SUM(COALESCE(a.md_f, 0) - COALESCE(a.mc_f, 0)) OVER w AS movement_through_f
    FROM gl_accass a
    JOIN fin_account_subject subj ON subj.company_id = a.company_id AND subj.subject_code = a.ccode
    WINDOW w AS (PARTITION BY a.company_id, a.iyear, a.ccode, a.currency_code,
                 a.cdept_id, a.cperson_id, a.ccus_id, a.csup_id, a.citem_class, a.citem_id
                 ORDER BY a.iperiod, a.i_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW);

    SELECT COUNT(*) INTO issue_count FROM tmp_gl_ass_history
    WHERE period_number > 1 AND (ABS(COALESCE(mb, 0) - COALESCE(previous_end, 0)) > 0.005
                               OR ABS(COALESCE(mb_f, 0) - COALESCE(previous_end_f, 0)) > 0.005);
    IF issue_count > 0 THEN
        SELECT company_id, iyear, iperiod, ccode, currency_code, cdept_id, cperson_id,
               ccus_id, csup_id, citem_class, citem_id, mb, previous_end
        FROM tmp_gl_ass_history
        WHERE period_number > 1 AND (ABS(COALESCE(mb, 0) - COALESCE(previous_end, 0)) > 0.005
                                  OR ABS(COALESCE(mb_f, 0) - COALESCE(previous_end_f, 0)) > 0.005)
        LIMIT 20;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: assist period continuity mismatch';
    END IF;

    -- The posted movement and auxiliary totals are independent checks before changing any amounts.
    SELECT COUNT(*) INTO issue_count FROM (
        SELECT v.company_id, v.iyear, v.iperiod, v.ccode, v.currency_code,
               SUM(GREATEST(COALESCE(v.md, 0), 0) + GREATEST(-COALESCE(v.mc, 0), 0)) AS debit,
               SUM(GREATEST(COALESCE(v.mc, 0), 0) + GREATEST(-COALESCE(v.md, 0), 0)) AS credit,
               SUM(GREATEST(COALESCE(v.md_f, 0), 0) + GREATEST(-COALESCE(v.mc_f, 0), 0)) AS debit_f,
               SUM(GREATEST(COALESCE(v.mc_f, 0), 0) + GREATEST(-COALESCE(v.md_f, 0), 0)) AS credit_f
        FROM gl_accvouch v WHERE v.ibook = 1
        GROUP BY v.company_id, v.iyear, v.iperiod, v.ccode, v.currency_code
    ) posted
    LEFT JOIN gl_accsum s ON s.company_id = posted.company_id AND s.iyear = posted.iyear
         AND s.iperiod = posted.iperiod AND s.ccode = posted.ccode AND s.currency_code = posted.currency_code
    WHERE s.i_id IS NULL OR ABS(COALESCE(s.md, 0) - posted.debit) > 0.005
         OR ABS(COALESCE(s.mc, 0) - posted.credit) > 0.005
         OR ABS(COALESCE(s.md_f, 0) - posted.debit_f) > 0.005
         OR ABS(COALESCE(s.mc_f, 0) - posted.credit_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: posted voucher movement mismatch';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT company_id, iyear, iperiod, ccode, currency_code,
               SUM(COALESCE(mb, 0)) AS mb, SUM(COALESCE(mb_f, 0)) AS mb_f,
               SUM(COALESCE(md, 0)) AS md, SUM(COALESCE(mc, 0)) AS mc,
               SUM(COALESCE(md_f, 0)) AS md_f, SUM(COALESCE(mc_f, 0)) AS mc_f
        FROM gl_accass GROUP BY company_id, iyear, iperiod, ccode, currency_code
    ) aux
    LEFT JOIN gl_accsum s ON s.company_id = aux.company_id AND s.iyear = aux.iyear
         AND s.iperiod = aux.iperiod AND s.ccode = aux.ccode AND s.currency_code = aux.currency_code
    WHERE s.i_id IS NULL OR ABS(COALESCE(s.mb, 0) - aux.mb) > 0.005
         OR ABS(COALESCE(s.mb_f, 0) - aux.mb_f) > 0.005
         OR ABS(COALESCE(s.md, 0) - aux.md) > 0.005 OR ABS(COALESCE(s.mc, 0) - aux.mc) > 0.005
         OR ABS(COALESCE(s.md_f, 0) - aux.md_f) > 0.005 OR ABS(COALESCE(s.mc_f, 0) - aux.mc_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: auxiliary and subject totals differ';
    END IF;

    UPDATE gl_accass a JOIN tmp_gl_ass_history h ON h.i_id = a.i_id
    SET a.mb = IF(h.usual_direction = 'CREDIT', -h.original_begin, h.original_begin) + h.movement_before,
        a.me = IF(h.usual_direction = 'CREDIT', -h.original_begin, h.original_begin) + h.movement_through,
        a.mb_f = IF(h.usual_direction = 'CREDIT', -h.original_begin_f, h.original_begin_f) + h.movement_before_f,
        a.me_f = IF(h.usual_direction = 'CREDIT', -h.original_begin_f, h.original_begin_f) + h.movement_through_f;

    UPDATE gl_accsum s JOIN tmp_gl_sum_history h ON h.i_id = s.i_id
    SET s.mb = IF(h.usual_direction = 'CREDIT', -h.original_begin, h.original_begin) + h.movement_before,
        s.me = IF(h.usual_direction = 'CREDIT', -h.original_begin, h.original_begin) + h.movement_through,
        s.mb_f = IF(h.usual_direction = 'CREDIT', -h.original_begin_f, h.original_begin_f) + h.movement_before_f,
        s.me_f = IF(h.usual_direction = 'CREDIT', -h.original_begin_f, h.original_begin_f) + h.movement_through_f
    WHERE h.leaf_flag = 1;

    SELECT COALESCE(MAX(subject_level), 1) - 1 INTO current_level FROM fin_account_subject;
    WHILE current_level >= 1 DO
        CREATE TEMPORARY TABLE tmp_gl_parent_totals AS
        SELECT child.company_id, child.iyear, child.iperiod, child.currency_code,
               child_subject.parent_subject_code AS parent_code,
               SUM(COALESCE(child.mb, 0)) AS mb, SUM(COALESCE(child.mb_f, 0)) AS mb_f,
               SUM(COALESCE(child.md, 0)) AS md, SUM(COALESCE(child.mc, 0)) AS mc,
               SUM(COALESCE(child.md_f, 0)) AS md_f, SUM(COALESCE(child.mc_f, 0)) AS mc_f
        FROM gl_accsum child
        JOIN fin_account_subject child_subject ON child_subject.company_id = child.company_id
             AND child_subject.subject_code = child.ccode
        WHERE child_subject.subject_level = current_level + 1
        GROUP BY child.company_id, child.iyear, child.iperiod, child.currency_code,
                 child_subject.parent_subject_code;
        UPDATE gl_accsum parent
        JOIN fin_account_subject parent_subject ON parent_subject.company_id = parent.company_id
             AND parent_subject.subject_code = parent.ccode AND parent_subject.subject_level = current_level
        JOIN tmp_gl_parent_totals totals ON totals.company_id = parent.company_id AND totals.iyear = parent.iyear
             AND totals.iperiod = parent.iperiod AND totals.currency_code = parent.currency_code
             AND totals.parent_code = parent.ccode
        SET parent.mb = totals.mb, parent.mb_f = totals.mb_f,
            parent.md = totals.md, parent.mc = totals.mc,
            parent.md_f = totals.md_f, parent.mc_f = totals.mc_f,
            parent.me = totals.mb + totals.md - totals.mc,
            parent.me_f = totals.mb_f + totals.md_f - totals.mc_f;
        DROP TEMPORARY TABLE tmp_gl_parent_totals;
        SET current_level = current_level - 1;
    END WHILE;

    UPDATE gl_accsum SET
        cbegind_c = IF(COALESCE(mb, 0) < 0, '贷', '借'),
        cbegind_c_engl = IF(COALESCE(mb, 0) < 0, 'CREDIT', 'DEBIT'),
        cendd_c = IF(COALESCE(me, 0) < 0, '贷', '借'),
        cendd_c_engl = IF(COALESCE(me, 0) < 0, 'CREDIT', 'DEBIT');
    UPDATE gl_accass SET
        cbegind_c = IF(COALESCE(mb, 0) < 0, '贷', '借'),
        cbegind_c_engl = IF(COALESCE(mb, 0) < 0, 'CREDIT', 'DEBIT'),
        cendd_c = IF(COALESCE(me, 0) < 0, '贷', '借'),
        cendd_c_engl = IF(COALESCE(me, 0) < 0, 'CREDIT', 'DEBIT');

    SELECT COUNT(*) INTO issue_count FROM gl_accsum
    WHERE ABS(COALESCE(me, 0) - COALESCE(mb, 0) - COALESCE(md, 0) + COALESCE(mc, 0)) > 0.005
       OR ABS(COALESCE(me_f, 0) - COALESCE(mb_f, 0) - COALESCE(md_f, 0) + COALESCE(mc_f, 0)) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: post-update arithmetic mismatch';
    END IF;

    SELECT COUNT(*) INTO issue_count FROM (
        SELECT company_id, iyear, iperiod, ccode, currency_code,
               SUM(COALESCE(mb, 0)) AS mb, SUM(COALESCE(me, 0)) AS me,
               SUM(COALESCE(mb_f, 0)) AS mb_f, SUM(COALESCE(me_f, 0)) AS me_f
        FROM gl_accass GROUP BY company_id, iyear, iperiod, ccode, currency_code
    ) aux
    JOIN gl_accsum s ON s.company_id = aux.company_id AND s.iyear = aux.iyear
         AND s.iperiod = aux.iperiod AND s.ccode = aux.ccode AND s.currency_code = aux.currency_code
    WHERE ABS(COALESCE(s.mb, 0) - aux.mb) > 0.005 OR ABS(COALESCE(s.me, 0) - aux.me) > 0.005
       OR ABS(COALESCE(s.mb_f, 0) - aux.mb_f) > 0.005 OR ABS(COALESCE(s.me_f, 0) - aux.me_f) > 0.005;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL balance migration: post-update auxiliary mismatch';
    END IF;

    INSERT INTO fin_gl_balance_migration (migration_key, applied_at, accsum_rows, accass_rows)
    VALUES ('debit_signed_balance_20261002', NOW(), sum_count, ass_count);
    COMMIT;
END $$
CALL migrate_gl_debit_signed_balance_20261002() $$
DROP PROCEDURE IF EXISTS migrate_gl_debit_signed_balance_20261002 $$
DELIMITER ;

SELECT migration_key, applied_at, accsum_rows, accass_rows
FROM fin_gl_balance_migration WHERE migration_key = 'debit_signed_balance_20261002';
