USE finex_db;

SET NAMES utf8mb4;

-- 统一空父级编码口径，避免后续判断把空串当作有效父级。
UPDATE fin_account_subject
SET parent_subject_code = NULL
WHERE parent_subject_code = '';

-- 根科目按当前账套规范固定为 4 位编码，缺失层级时回填为 1 级。
UPDATE fin_account_subject
SET parent_subject_code = NULL,
    subject_level = 1
WHERE CHAR_LENGTH(subject_code) = 4
  AND (
      parent_subject_code IS NOT NULL
      OR subject_level IS NULL
      OR subject_level <> 1
  );

-- 对可唯一推导父级的历史科目，按最长前缀父级回填父级编码和层级。
UPDATE fin_account_subject child
JOIN (
    SELECT
        candidate.id,
        parent.subject_code AS resolved_parent_subject_code,
        parent.subject_level + 1 AS resolved_subject_level
    FROM fin_account_subject candidate
    JOIN fin_account_subject parent
        ON parent.company_id = candidate.company_id
       AND parent.subject_code <> candidate.subject_code
       AND candidate.subject_code LIKE CONCAT(parent.subject_code, '%')
    LEFT JOIN fin_account_subject longer_parent
        ON longer_parent.company_id = candidate.company_id
       AND longer_parent.subject_code <> candidate.subject_code
       AND candidate.subject_code LIKE CONCAT(longer_parent.subject_code, '%')
       AND CHAR_LENGTH(longer_parent.subject_code) > CHAR_LENGTH(parent.subject_code)
    WHERE longer_parent.id IS NULL
) resolved
    ON resolved.id = child.id
LEFT JOIN fin_account_subject current_parent
    ON current_parent.company_id = child.company_id
   AND current_parent.subject_code = child.parent_subject_code
SET child.parent_subject_code = resolved.resolved_parent_subject_code,
    child.subject_level = resolved.resolved_subject_level
WHERE child.parent_subject_code IS NULL
   OR current_parent.id IS NULL
   OR child.subject_code = child.parent_subject_code
   OR child.subject_code NOT LIKE CONCAT(current_parent.subject_code, '%')
   OR child.parent_subject_code <> resolved.resolved_parent_subject_code
   OR child.subject_level IS NULL
   OR child.subject_level <> resolved.resolved_subject_level;

-- 按真实父子关系统一回算末级状态；有下级科目则非末级，否则为末级。
UPDATE fin_account_subject subject
LEFT JOIN (
    SELECT
        company_id,
        parent_subject_code,
        COUNT(*) AS child_count
    FROM fin_account_subject
    WHERE parent_subject_code IS NOT NULL
      AND parent_subject_code <> ''
    GROUP BY company_id, parent_subject_code
) child_summary
    ON child_summary.company_id = subject.company_id
   AND child_summary.parent_subject_code = subject.subject_code
SET subject.leaf_flag = CASE
    WHEN COALESCE(child_summary.child_count, 0) > 0 THEN 0
    ELSE 1
END
WHERE subject.leaf_flag IS NULL
   OR subject.leaf_flag <> CASE
       WHEN COALESCE(child_summary.child_count, 0) > 0 THEN 0
       ELSE 1
   END;
