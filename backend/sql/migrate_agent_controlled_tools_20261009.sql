SET NAMES utf8mb4;

-- Apply after migrate_digital_employee_query_20261001.sql and
-- migrate_finwork_phase1_query_20261002.sql. Safe to run again.
CREATE TABLE IF NOT EXISTS ea_workflow_share_capability (
  share_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  PRIMARY KEY (share_id, permission_code),
  KEY idx_share_capability_code (permission_code, share_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定版本全部受控查询能力';

INSERT IGNORE INTO ea_workflow_share_capability (share_id, permission_code)
SELECT id, permission_code FROM ea_workflow_share WHERE permission_code IS NOT NULL;

CREATE TABLE IF NOT EXISTS ea_controlled_result_origin (
  source_type VARCHAR(16) NOT NULL,
  source_id BIGINT NOT NULL,
  item_no SMALLINT NOT NULL,
  origin_count SMALLINT NOT NULL,
  employee_id BIGINT NOT NULL,
  binding_id BIGINT NOT NULL,
  responsible_user_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  tool_code VARCHAR(128) NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  auth_digest CHAR(64) NOT NULL,
  query_json TEXT NOT NULL,
  source_digest CHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (source_type,source_id,item_no),
  KEY idx_result_origin_employee (employee_id,agent_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天与定时结果逐工具复核来源';

-- New multi-tool deliveries must not fall back to legacy single-code checks if
-- their provenance rows are missing. Keep the flag default-off for old records.
SET @origin_required_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='ea_digital_employee_delivery'
    AND column_name='origin_required'
);
SET @origin_required_sql = IF(@origin_required_exists=0,
  'ALTER TABLE ea_digital_employee_delivery ADD COLUMN origin_required TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT ''ea_digital_employee_delivery.origin_required exists''');
PREPARE origin_required_stmt FROM @origin_required_sql;
EXECUTE origin_required_stmt;
DEALLOCATE PREPARE origin_required_stmt;

INSERT IGNORE INTO ea_controlled_result_origin
  (source_type,source_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
   agent_version_id,tool_code,permission_code,auth_digest,query_json,source_digest)
SELECT 'CHAT',i.message_id,i.item_no,i.origin_count,i.employee_id,i.binding_id,
       i.responsible_user_id,i.agent_version_id,
       CASE i.permission_code WHEN 'expense:agent:query' THEN 'expense.query_my_expenses'
            WHEN 'finance:period_close:query' THEN 'finance.query_unclosed_companies'
            ELSE 'legacy.unknown' END,
       i.permission_code,i.auth_digest,i.query_json,i.source_digest
FROM ea_agent_message_query_origin_item i;

INSERT IGNORE INTO ea_controlled_result_origin
  (source_type,source_id,item_no,origin_count,employee_id,binding_id,responsible_user_id,
   agent_version_id,tool_code,permission_code,auth_digest,query_json,source_digest)
SELECT 'DELIVERY',d.id,1,1,d.employee_id,p.workflow_binding_id,e.responsible_user_id,
       d.agent_version_id,
       CASE d.permission_code WHEN 'expense:agent:query' THEN 'expense.query_my_expenses'
            WHEN 'finance:period_close:query' THEN 'finance.query_unclosed_companies'
            ELSE 'legacy.unknown' END,
       d.permission_code,d.auth_digest,'{}',d.auth_digest
FROM ea_digital_employee_delivery d
JOIN ea_digital_employee_plan p ON p.id=d.plan_id
JOIN ea_digital_employee e ON e.id=d.employee_id
WHERE d.result_text IS NOT NULL AND d.auth_digest IS NOT NULL;

INSERT INTO sys_query_capability
  (permission_code,capability_name,domain_code,supported_scopes,status) VALUES
  ('finance:agent:account_subjects:query','Agent 会计科目查询','FINANCE','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('finance:agent:project_classes:query','Agent 项目分类查询','FINANCE','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('finance:agent:projects:query','Agent 项目名称查询','FINANCE','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('finance:agent:customers:query','Agent 客户档案查询','FINANCE','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('finance:agent:vendors:query','Agent 供应商档案查询','FINANCE','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('system:agent:departments:query','Agent 部门档案查询','SYSTEM','SELECTED_COMPANIES,ALL_COMPANIES',1),
  ('system:agent:people:query','Agent 当前租户人员查询','SYSTEM','TENANT_ALL',1)
ON DUPLICATE KEY UPDATE capability_name=VALUES(capability_name),
  supported_scopes=VALUES(supported_scopes),status=VALUES(status);

INSERT INTO sys_permission
  (permission_code,permission_name,permission_type,parent_id,module_code,sort_order,status)
SELECT c.permission_code,c.permission_name,'BUTTON',p.id,'agentWorkbench',c.sort_order,1
FROM (
  SELECT 'finance:agent:account_subjects:query' permission_code,'查询会计科目' permission_name,56101 sort_order
  UNION ALL SELECT 'finance:agent:project_classes:query','查询项目分类',56102
  UNION ALL SELECT 'finance:agent:projects:query','查询项目名称',56103
  UNION ALL SELECT 'finance:agent:customers:query','查询客户档案',56104
  UNION ALL SELECT 'finance:agent:vendors:query','查询供应商档案',56105
  UNION ALL SELECT 'system:agent:departments:query','查询部门档案',56106
  UNION ALL SELECT 'system:agent:people:query','跨公司查询人员信息',56107
) c JOIN sys_permission p ON p.permission_code='agent:workbench:view'
LEFT JOIN sys_permission existing ON existing.permission_code=c.permission_code
WHERE existing.id IS NULL;

INSERT IGNORE INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
LEFT JOIN sys_role_permission existing ON existing.role_id=r.id AND existing.permission_id=p.id
WHERE UPPER(r.role_code) IN ('SUPER_ADMIN','SUPERADMIN')
  AND existing.id IS NULL
  AND p.permission_code IN (
    'finance:agent:account_subjects:query','finance:agent:project_classes:query',
    'finance:agent:projects:query','finance:agent:customers:query',
    'finance:agent:vendors:query','system:agent:departments:query',
    'system:agent:people:query');

-- Rollback: disable new query capabilities and tools first; retain origin rows for
-- historical revocation checks. Drop the two new tables only after all dependent
-- code and records have been retired. Never erase role grants as a rollback shortcut.
