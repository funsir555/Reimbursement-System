-- 总账余额口径 + 红字发生额一体化迁移（MySQL 8，单文件顺序执行）。
-- 步骤 1：停止凭证写入、记账、开账、结转及结账任务，确认没有运行中的异步财务任务。
-- 步骤 2：完整备份目标数据库；先把备份恢复到隔离库，用本文件试跑并核对结果。
-- 步骤 3：选中正确的目标数据库后执行整个文件；下方首先输出当前数据库名称。
-- 步骤 4：缺少旧迁移标记时，先检查贷方科目历史余额与方向字段是否仍符合旧口径。
--         若数据可能已转换但标记丢失、或方向字段无法判定，脚本会停止，不自动补标记或猜测翻转。
-- 步骤 5：按顺序执行 2026-10-02 借方正净额迁移，再执行 2026-10-08 红字发生额同侧抵减迁移。
--         两个阶段各自提交并有独立标记；中途失败时修复原因后重跑整个文件即可，不重复校正已完成阶段。
-- 步骤 6：核对末尾两个迁移标记、受影响期间 me=mb+md-mc、父子与辅助汇总，以及页面和导出。
-- 本脚本不改原凭证和历史数量；遇到无法确认的数据只报错停止。不得跳过检查或手工插入标记。
SET NAMES utf8mb4;
SELECT DATABASE() AS target_schema;

CREATE TABLE IF NOT EXISTS fin_gl_balance_migration (
    migration_key VARCHAR(80) NOT NULL PRIMARY KEY,
    applied_at DATETIME NOT NULL,
    accsum_rows BIGINT NOT NULL,
    accass_rows BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS assert_gl_legacy_balance_state_20261008 $$
CREATE PROCEDURE assert_gl_legacy_balance_state_20261008()
main: BEGIN
    DECLARE issue_count BIGINT DEFAULT 0;
    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL combined migration: select the target database first';
    END IF;
    IF EXISTS (SELECT 1 FROM fin_gl_balance_migration
               WHERE migration_key = 'debit_signed_balance_20261002') THEN
        LEAVE main;
    END IF;
    IF EXISTS (SELECT 1 FROM fin_gl_balance_migration
               WHERE migration_key = 'signed_red_movement_20261008') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL combined migration: inconsistent migration markers';
    END IF;

    -- 旧口径中贷方科目的正额标为贷、负额标为借；新口径恰好相反。
    -- 方向字段缺失、已转成新口径或新旧混杂都不能安全执行旧余额翻转。
    SELECT COUNT(*) INTO issue_count FROM (
        SELECT s.i_id FROM gl_accsum s JOIN fin_account_subject subject
          ON subject.company_id = s.company_id AND subject.subject_code = s.ccode
        WHERE UPPER(TRIM(subject.balance_direction)) = 'CREDIT'
          AND ((ABS(COALESCE(s.mb, 0)) > 0.005 AND COALESCE(UPPER(TRIM(s.cbegind_c_engl)), '')
                <> IF(s.mb > 0, 'CREDIT', 'DEBIT'))
            OR (ABS(COALESCE(s.me, 0)) > 0.005 AND COALESCE(UPPER(TRIM(s.cendd_c_engl)), '')
                <> IF(s.me > 0, 'CREDIT', 'DEBIT'))
            OR (ABS(COALESCE(s.mb_f, 0)) > 0.005 AND ABS(COALESCE(s.mb, 0)) <= 0.005)
            OR (ABS(COALESCE(s.me_f, 0)) > 0.005 AND ABS(COALESCE(s.me, 0)) <= 0.005))
        UNION ALL
        SELECT a.i_id FROM gl_accass a JOIN fin_account_subject subject
          ON subject.company_id = a.company_id AND subject.subject_code = a.ccode
        WHERE UPPER(TRIM(subject.balance_direction)) = 'CREDIT'
          AND ((ABS(COALESCE(a.mb, 0)) > 0.005 AND COALESCE(UPPER(TRIM(a.cbegind_c_engl)), '')
                <> IF(a.mb > 0, 'CREDIT', 'DEBIT'))
            OR (ABS(COALESCE(a.me, 0)) > 0.005 AND COALESCE(UPPER(TRIM(a.cendd_c_engl)), '')
                <> IF(a.me > 0, 'CREDIT', 'DEBIT'))
            OR (ABS(COALESCE(a.mb_f, 0)) > 0.005 AND ABS(COALESCE(a.mb, 0)) <= 0.005)
            OR (ABS(COALESCE(a.me_f, 0)) > 0.005 AND ABS(COALESCE(a.me, 0)) <= 0.005))
    ) ambiguous_rows;
    IF issue_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'GL combined migration: missing marker and balances are not provably legacy';
    END IF;
END $$
CALL assert_gl_legacy_balance_state_20261008() $$
DROP PROCEDURE IF EXISTS assert_gl_legacy_balance_state_20261008 $$
DELIMITER ;

-- 第一阶段：2026-10-02 余额净额口径迁移；自身标记保证重跑时跳过。
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

-- 第二阶段：2026-10-08 红字凭证按原借贷列抵减发生额；自身标记保证重跑时跳过。
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
-- 两阶段均成功后输出结果；若在任一阶段报错，后续语句不应继续执行。
SELECT migration_key, applied_at, accsum_rows, accass_rows
FROM fin_gl_balance_migration
WHERE migration_key IN ('debit_signed_balance_20261002', 'signed_red_movement_20261008')
ORDER BY migration_key;
