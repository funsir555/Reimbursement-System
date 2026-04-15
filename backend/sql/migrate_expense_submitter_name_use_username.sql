-- 将报销链提单人显示口径统一回填为 sys_user.username。
-- pm_document_instance 是报销链的单据主表，submitter_name 会被详情、列表、审批待办、付款与凭证链路复用。
UPDATE pm_document_instance AS instance_record
INNER JOIN sys_user AS submitter
        ON submitter.id = instance_record.submitter_user_id
SET instance_record.submitter_name = submitter.username
WHERE instance_record.submitter_user_id IS NOT NULL
  AND TRIM(COALESCE(submitter.username, '')) <> ''
  AND (
        instance_record.submitter_name IS NULL
        OR instance_record.submitter_name <> submitter.username
      );
