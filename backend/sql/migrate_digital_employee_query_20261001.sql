-- Apply after migrate_system_settings.sql, init_archive_agent.sql and ea_agent_platform_init.sql.
-- Inspect target schema and take a backup before applying. Re-running this file is safe.
SET NAMES utf8mb4;

-- Fail before creating any new table when the legacy foundation is missing.
-- The current production schema must be inspected; do not treat this migration
-- as a replacement for ea_agent_platform_init.sql.
SELECT id, tenant_id, company_id, status FROM ea_digital_employee LIMIT 0;
SELECT id, permission_code, permission_type, parent_id, status FROM sys_permission LIMIT 0;
SELECT id, role_code, status FROM sys_role LIMIT 0;
SELECT id, role_id, permission_id FROM sys_role_permission LIMIT 0;
SELECT id, submitter_user_id FROM pm_document_instance LIMIT 0;

-- Business-object company is captured when a document is created. Existing
-- documents stay NULL until their historical company has been verified; Agent
-- queries fail closed for them rather than guessing from today's user profile.
SET @has_document_company = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pm_document_instance' AND COLUMN_NAME='company_id'
);
SET @document_company_ddl = IF(@has_document_company=0,
  'ALTER TABLE pm_document_instance ADD COLUMN company_id VARCHAR(64) NULL AFTER submitter_user_id',
  'SELECT 1');
PREPARE document_company_statement FROM @document_company_ddl;
EXECUTE document_company_statement;
DEALLOCATE PREPARE document_company_statement;
SET @has_document_company_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pm_document_instance'
    AND INDEX_NAME='idx_pm_document_instance_company_created_at'
);
SET @document_company_index_ddl = IF(@has_document_company_index=0,
  'CREATE INDEX idx_pm_document_instance_company_created_at ON pm_document_instance (company_id,created_at)',
  'SELECT 1');
PREPARE document_company_index_statement FROM @document_company_index_ddl;
EXECUTE document_company_index_statement;
DEALLOCATE PREPARE document_company_index_statement;

CREATE TABLE IF NOT EXISTS sys_query_capability (
  permission_code VARCHAR(128) PRIMARY KEY,
  capability_name VARCHAR(100) NOT NULL,
  domain_code VARCHAR(32) NOT NULL,
  supported_scopes VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受控查询能力目录';

CREATE TABLE IF NOT EXISTS sys_role_query_scope (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  scope_type VARCHAR(32) NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_query_scope (role_id, permission_code),
  KEY idx_query_scope_permission (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色逐项查询数据范围';

CREATE TABLE IF NOT EXISTS sys_role_query_scope_company (
  scope_id BIGINT NOT NULL,
  company_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (scope_id, company_id),
  KEY idx_query_scope_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色查询范围指定公司';

CREATE TABLE IF NOT EXISTS sys_query_scope_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  before_json TEXT NULL,
  after_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_query_scope_audit_role (role_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色查询范围变更审计';

-- The legacy employee row is retained. company_id is management ownership only;
-- authority_level, max_amount and config are never used for query authorization.
CREATE TABLE IF NOT EXISTS ea_digital_employee_service (
  employee_id BIGINT PRIMARY KEY,
  description VARCHAR(500) NULL,
  avatar_code VARCHAR(64) NULL,
  lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  daily_run_limit INT NULL,
  concurrency_limit INT NULL,
  version_no BIGINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字员工服务配置';

CREATE TABLE IF NOT EXISTS ea_workflow_share (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  reviewer_user_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  UNIQUE KEY uk_workflow_share_version (agent_version_id),
  KEY idx_workflow_share_status (status, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定工作流版本企业共享审核';

CREATE TABLE IF NOT EXISTS ea_workflow_share_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  share_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  action_code VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_workflow_share_audit (share_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流共享审核操作记录';

CREATE TABLE IF NOT EXISTS ea_digital_employee_workflow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  share_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_employee_share (employee_id, share_id),
  KEY idx_employee_workflow_capability (permission_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字员工固定版本绑定';

CREATE TABLE IF NOT EXISTS ea_digital_employee_user (
  employee_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (employee_id, user_id),
  KEY idx_employee_user_lookup (user_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字员工指定使用者';

CREATE TABLE IF NOT EXISTS ea_digital_employee_scope_company (
  employee_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  company_id VARCHAR(64) NOT NULL,
  PRIMARY KEY (employee_id, permission_code, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字员工服务范围公司上限';

CREATE TABLE IF NOT EXISTS ea_digital_employee_plan (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  workflow_binding_id BIGINT NOT NULL,
  frequency VARCHAR(10) NOT NULL,
  run_hour TINYINT NOT NULL,
  run_minute TINYINT NOT NULL,
  week_day TINYINT NULL,
  month_day TINYINT NULL,
  parameter_json VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  next_run_at DATETIME NULL,
  version_no BIGINT NOT NULL DEFAULT 1,
  KEY idx_employee_plan_due (status, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数字员工受控定时查询计划';

CREATE TABLE IF NOT EXISTS ea_digital_employee_subscription (
  plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  subscribed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FinWork 用户主动订阅';

CREATE TABLE IF NOT EXISTS ea_digital_employee_delivery (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  scheduled_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  employee_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  result_text TEXT NULL,
  auth_digest VARCHAR(128) NULL,
  trace_id VARCHAR(64) NULL,
  failure_code VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  UNIQUE KEY uk_employee_delivery_trigger (plan_id, user_id, scheduled_at),
  KEY idx_employee_inbox (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐用户定时查询及 FinWork 收件箱';

CREATE TABLE IF NOT EXISTS ea_agent_message_query_origin (
  message_id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  responsible_user_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  auth_digest CHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受控回答历史复核元数据';

INSERT INTO sys_query_capability (permission_code, capability_name, domain_code, supported_scopes, status) VALUES
('expense:agent:query', '报销单受控查询', 'EXPENSE', 'SELF,DEPARTMENT_TREE,SELECTED_COMPANIES,ALL_COMPANIES', 1),
('finance:period_close:query', '期间未结账公司查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1)
ON DUPLICATE KEY UPDATE capability_name = VALUES(capability_name), supported_scopes = VALUES(supported_scopes);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, parent_id, module_code, sort_order, status)
SELECT codes.permission_code, codes.permission_name, 'BUTTON', parent.id, codes.module_code, codes.sort_order, 1
FROM (
 SELECT 'settings:roles:assign_data_scope' permission_code, '设置数据可见范围' permission_name, 'settings' module_code, 52051 sort_order, 'settings:roles:view' parent_code
 UNION ALL SELECT 'expense:agent:query', '报销单受控查询', 'agentChat', 56012, 'agent:chat:view'
 UNION ALL SELECT 'finance:period_close:query', '期间未结账公司查询', 'agentChat', 56013, 'agent:chat:view'
 UNION ALL SELECT 'agent:digital_employee:create', '新建数字员工', 'agentDigitalEmployee', 56041, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:digital_employee:edit', '编辑数字员工', 'agentDigitalEmployee', 56042, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:digital_employee:assign', '分配数字员工', 'agentDigitalEmployee', 56043, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:digital_employee:status', '启停数字员工', 'agentDigitalEmployee', 56044, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:digital_employee:plan', '管理定时计划', 'agentDigitalEmployee', 56045, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:digital_employee:logs', '查看数字员工审计', 'agentDigitalEmployee', 56046, 'agent:digital_employee:view'
 UNION ALL SELECT 'agent:workbench:share_submit', '提交工作流共享', 'agentWorkbench', 56027, 'agent:workbench:view'
 UNION ALL SELECT 'agent:workbench:share_approve', '审核工作流共享', 'agentWorkbench', 56028, 'agent:workbench:view'
) codes JOIN sys_permission parent ON parent.permission_code = codes.parent_code
LEFT JOIN sys_permission existing ON existing.permission_code = codes.permission_code
WHERE existing.id IS NULL;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.permission_code IN (
 'settings:roles:assign_data_scope','expense:agent:query','finance:period_close:query',
 'agent:digital_employee:create','agent:digital_employee:edit','agent:digital_employee:assign',
 'agent:digital_employee:status','agent:digital_employee:plan','agent:digital_employee:logs',
 'agent:workbench:share_submit','agent:workbench:share_approve')
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE UPPER(r.role_code) = 'SUPER_ADMIN' AND rp.id IS NULL;

-- Legacy rows have no approved binding or assigned users. They are treated as
-- suspended until an administrator reviews and activates an explicit service.
INSERT INTO ea_digital_employee_service (employee_id, lifecycle_status)
SELECT id, 'SUSPENDED' FROM ea_digital_employee
ON DUPLICATE KEY UPDATE employee_id = VALUES(employee_id);
