-- 废弃报销单 APPROVED 状态，将历史单据迁移到新的支付/完成状态链路。
-- 执行前请先在目标库做备份。

-- 1) 已拿到回单的历史单据，直接迁移为已完成。
UPDATE pm_document_instance d
JOIN pm_bank_payment_record r ON r.document_code = d.document_code
SET d.status = 'PAYMENT_FINISHED',
    d.current_node_key = NULL,
    d.current_node_name = NULL,
    d.current_task_type = NULL,
    d.finished_at = COALESCE(d.finished_at, r.paid_at, d.updated_at, d.created_at),
    d.updated_at = NOW()
WHERE d.status = 'APPROVED'
  AND (
    (r.receipt_attachment_id IS NOT NULL AND r.receipt_attachment_id <> '')
    OR r.receipt_status = 'RECEIVED'
  );

-- 2) 已确认付款但尚未拿到回单的历史单据，迁移为已支付。
UPDATE pm_document_instance d
JOIN pm_bank_payment_record r ON r.document_code = d.document_code
SET d.status = 'PAYMENT_COMPLETED',
    d.current_task_type = 'PAYMENT',
    d.updated_at = NOW()
WHERE d.status = 'APPROVED'
  AND r.paid_at IS NOT NULL;

-- 3) 审批流包含支付节点、但还没有支付结果的历史单据，迁移为待支付。
UPDATE pm_document_instance
SET status = 'PENDING_PAYMENT',
    current_task_type = CASE
        WHEN current_task_type IS NULL OR current_task_type = '' THEN 'PAYMENT'
        ELSE current_task_type
    END,
    updated_at = NOW()
WHERE status = 'APPROVED'
  AND flow_snapshot_json LIKE '%"nodeType":"PAYMENT"%';

-- 4) 其余历史 APPROVED 单据视为无支付节点审批结束，统一迁移为已完成。
UPDATE pm_document_instance
SET status = 'COMPLETED',
    current_node_key = NULL,
    current_node_name = NULL,
    current_task_type = NULL,
    finished_at = COALESCE(finished_at, updated_at, created_at),
    updated_at = NOW()
WHERE status = 'APPROVED';
