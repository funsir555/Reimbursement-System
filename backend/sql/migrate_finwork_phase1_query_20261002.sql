-- FinWork phase 1: direct query scopes and protected conversation provenance.
-- Run after migrate_digital_employee_query_20261001.sql. No ordinary role receives
-- an implicit scope; administrators must configure every required capability.

INSERT INTO sys_query_capability
  (permission_code, capability_name, domain_code, supported_scopes, status)
VALUES
  ('expense:list:view', '我的报销单', 'EXPENSE', 'SELF', 1),
  ('expense:approval:view', '分配给我的审批单', 'EXPENSE', 'SELF', 1),
  ('expense:documents:view', '单据查询', 'EXPENSE', 'SELF,DEPARTMENT_TREE,SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('expense:payment:payment_order:view', '付款单据查看', 'EXPENSE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('dashboard:view', '个人看板单据', 'EXPENSE', 'SELF', 1),
  ('finance:general_ledger:balance_sheet:view', '余额表查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:detail_ledger:view', '明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:general_ledger:view', '总分类账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:project_detail_ledger:view', '项目明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:supplier_detail_ledger:view', '供应商明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:customer_detail_ledger:view', '客户明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:personal_detail_ledger:view', '个人明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:quantity_amount_detail_ledger:view', '数量金额明细账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1),
  ('finance:general_ledger:sequence_ledger:view', '序时账查询', 'FINANCE', 'SELECTED_COMPANIES,ALL_COMPANIES', 1)
ON DUPLICATE KEY UPDATE
  capability_name = VALUES(capability_name),
  supported_scopes = VALUES(supported_scopes),
  status = VALUES(status);

CREATE TABLE IF NOT EXISTS ea_agent_chat_pending_plan (
  session_id VARCHAR(64) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  pending_json TEXT NOT NULL,
  expires_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_chat_pending_user (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FinWork 待澄清的受控任务计划';

CREATE TABLE IF NOT EXISTS ea_agent_message_query_origin_item (
  message_id BIGINT NOT NULL,
  item_no TINYINT NOT NULL,
  origin_count TINYINT NOT NULL,
  employee_id BIGINT NOT NULL,
  binding_id BIGINT NOT NULL,
  responsible_user_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  auth_digest CHAR(64) NOT NULL,
  query_json TEXT NOT NULL,
  source_digest CHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, item_no),
  KEY idx_message_query_employee (employee_id, agent_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复合受控回答的逐工作流来源';

CREATE TABLE IF NOT EXISTS sys_download_query_origin (
  download_record_id BIGINT PRIMARY KEY,
  permission_code VARCHAR(128) NOT NULL,
  auth_digest CHAR(64) NOT NULL,
  query_json TEXT NOT NULL,
  source_digest CHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感导出的再次授权依据';
