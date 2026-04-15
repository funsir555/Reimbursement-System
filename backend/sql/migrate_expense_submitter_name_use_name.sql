-- 将报销链提单人显示口径纠正回 sys_user.name。
-- 该脚本用于修正此前已经按 username 回填过的 submitter_name 历史数据。
UPDATE pm_document_instance AS instance_record
INNER JOIN sys_user AS submitter
        ON submitter.id = instance_record.submitter_user_id
SET instance_record.submitter_name = submitter.name
WHERE instance_record.submitter_user_id IS NOT NULL
  AND TRIM(COALESCE(submitter.name, '')) <> ''
  AND (
        instance_record.submitter_name IS NULL
        OR instance_record.submitter_name <> submitter.name
      );
