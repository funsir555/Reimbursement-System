USE finex_db;

SET NAMES utf8mb4;

-- 执行顺序：
-- 1. 先部署包含“反方向余额语义”修复的后端代码
-- 2. 再执行本脚本回填历史方向字段
-- 3. 本脚本只修正方向字段，不修改金额字段，允许重复执行

UPDATE gl_accsum s
JOIN fin_account_subject subj
  ON subj.company_id = s.company_id
 AND subj.subject_code = s.ccode
SET
  s.cbegind_c = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(s.mb, 0) < 0 THEN '贷' ELSE '借' END
    ELSE CASE WHEN IFNULL(s.mb, 0) < 0 THEN '借' ELSE '贷' END
  END,
  s.cbegind_c_engl = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(s.mb, 0) < 0 THEN 'CREDIT' ELSE 'DEBIT' END
    ELSE CASE WHEN IFNULL(s.mb, 0) < 0 THEN 'DEBIT' ELSE 'CREDIT' END
  END,
  s.cendd_c = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(s.me, 0) < 0 THEN '贷' ELSE '借' END
    ELSE CASE WHEN IFNULL(s.me, 0) < 0 THEN '借' ELSE '贷' END
  END,
  s.cendd_c_engl = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(s.me, 0) < 0 THEN 'CREDIT' ELSE 'DEBIT' END
    ELSE CASE WHEN IFNULL(s.me, 0) < 0 THEN 'DEBIT' ELSE 'CREDIT' END
  END;

UPDATE gl_accass a
JOIN fin_account_subject subj
  ON subj.company_id = a.company_id
 AND subj.subject_code = a.ccode
SET
  a.cbegind_c = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(a.mb, 0) < 0 THEN '贷' ELSE '借' END
    ELSE CASE WHEN IFNULL(a.mb, 0) < 0 THEN '借' ELSE '贷' END
  END,
  a.cbegind_c_engl = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(a.mb, 0) < 0 THEN 'CREDIT' ELSE 'DEBIT' END
    ELSE CASE WHEN IFNULL(a.mb, 0) < 0 THEN 'DEBIT' ELSE 'CREDIT' END
  END,
  a.cendd_c = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(a.me, 0) < 0 THEN '贷' ELSE '借' END
    ELSE CASE WHEN IFNULL(a.me, 0) < 0 THEN '借' ELSE '贷' END
  END,
  a.cendd_c_engl = CASE
    WHEN (UPPER(IFNULL(subj.balance_direction, '')) LIKE '%DEBIT%' OR subj.balance_direction LIKE '%借%')
      THEN CASE WHEN IFNULL(a.me, 0) < 0 THEN 'CREDIT' ELSE 'DEBIT' END
    ELSE CASE WHEN IFNULL(a.me, 0) < 0 THEN 'DEBIT' ELSE 'CREDIT' END
  END;

-- 诊断：以下记录因缺少科目主数据不会被回填，请人工确认
SELECT 'gl_accsum_missing_subject' AS issue_type, s.company_id, s.iyear, s.iperiod, s.ccode, COUNT(*) AS row_count
FROM gl_accsum s
LEFT JOIN fin_account_subject subj
  ON subj.company_id = s.company_id
 AND subj.subject_code = s.ccode
WHERE subj.id IS NULL
GROUP BY s.company_id, s.iyear, s.iperiod, s.ccode;

SELECT 'gl_accass_missing_subject' AS issue_type, a.company_id, a.iyear, a.iperiod, a.ccode, COUNT(*) AS row_count
FROM gl_accass a
LEFT JOIN fin_account_subject subj
  ON subj.company_id = a.company_id
 AND subj.subject_code = a.ccode
WHERE subj.id IS NULL
GROUP BY a.company_id, a.iyear, a.iperiod, a.ccode;
