SET NAMES utf8mb4;

-- Curated, product-level semantics. No company records or executable SQL belong here.
-- Legacy ea_entity_definition / ea_field_semantics / ea_enum_semantics remain unchanged;
-- their content is not automatically published because it has not passed disclosure review.
CREATE TABLE IF NOT EXISTS ea_ontology_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_key VARCHAR(128) NOT NULL,
  kind VARCHAR(24) NOT NULL,
  entity_key VARCHAR(128) NULL,
  display_name VARCHAR(120) NOT NULL,
  description VARCHAR(1200) NOT NULL,
  aliases_json JSON NULL,
  value_code VARCHAR(64) NULL,
  target_key VARCHAR(128) NULL,
  cardinality VARCHAR(24) NULL,
  semantic_type VARCHAR(32) NULL,
  unit_name VARCHAR(32) NULL,
  tool_code VARCHAR(128) NULL,
  owner_domain VARCHAR(32) NOT NULL,
  source_ref VARCHAR(255) NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  effective_from DATE NOT NULL,
  effective_until DATE NULL,
  created_by BIGINT NOT NULL,
  reviewed_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  UNIQUE KEY uk_ontology_item_version (item_key, version_no),
  KEY idx_ontology_publish (status, effective_from, effective_until),
  KEY idx_ontology_kind_entity (kind, entity_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经审核的通用业务语义版本';

-- This binding is server-internal and must never be included in model-visible DTOs.
CREATE TABLE IF NOT EXISTS ea_ontology_schema_binding (
  item_key VARCHAR(128) PRIMARY KEY,
  table_name VARCHAR(100) NOT NULL,
  column_name VARCHAR(100) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务语义到数据库结构的内部映射';

INSERT INTO ea_ontology_item
  (item_key,kind,entity_key,display_name,description,aliases_json,value_code,target_key,
   cardinality,semantic_type,unit_name,tool_code,owner_domain,source_ref,version_no,status,
   effective_from,created_by,reviewed_by,reviewed_at)
VALUES
  ('expense_document','ENTITY',NULL,'报销单','员工提交费用报销申请所形成的业务单据。',JSON_ARRAY('报销','报销申请','费用报销单'),NULL,NULL,NULL,NULL,NULL,NULL,'EXPENSE','Expense 领域单据定义',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_detail','ENTITY',NULL,'费用明细','报销单中记录具体费用内容的明细。',JSON_ARRAY('报销明细'),NULL,NULL,NULL,NULL,NULL,NULL,'EXPENSE','Expense 领域费用明细定义',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_document.status','FIELD','expense_document','单据状态','表示报销单当前所处的业务阶段。',JSON_ARRAY('报销状态'),NULL,NULL,NULL,'ENUM',NULL,NULL,'EXPENSE','Expense 单据状态定义',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_document.status.DRAFT','ENUM','expense_document','草稿','报销单尚处于草稿阶段。',JSON_ARRAY('未提交'), 'DRAFT',NULL,NULL,NULL,NULL,NULL,'EXPENSE','Expense 单据状态定义',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_document.status.PENDING_APPROVAL','ENUM','expense_document','审批中','报销单已进入审批流程。',JSON_ARRAY('待审批'), 'PENDING_APPROVAL',NULL,NULL,NULL,NULL,NULL,'EXPENSE','Expense 单据状态定义',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_document.has_detail','RELATION','expense_document','包含费用明细','一张报销单可以包含多条费用明细。',JSON_ARRAY(),NULL,'expense_detail','ONE_TO_MANY',NULL,NULL,NULL,'EXPENSE','Expense 单据与费用明细关系',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('expense_document.submit','RULE','expense_document','报销单提交','提交报销单会进入审批流程；是否允许提交及后续状态由 Expense 和 Process 服务校验。',JSON_ARRAY('提交报销申请'),NULL,NULL,NULL,NULL,NULL,NULL,'EXPENSE','Expense 单据提交流程',1,'DRAFT','2026-10-12',0,NULL,NULL),
  ('expense_document.authorized_total','METRIC','expense_document','授权报销单金额合计','对当前用户有权查看且满足筛选条件的报销单金额做完整合计；不能据此推断全公司金额。',JSON_ARRAY('我的报销总额'),NULL,NULL,NULL,NULL,'元',NULL,'EXPENSE','受控报销查询结果口径',1,'DRAFT','2026-10-12',0,NULL,NULL),
  ('expense_document.query_mine','CAPABILITY','expense_document','查询我的报销单','按当前用户有效权限查询可见报销单；具体数据范围由业务服务决定。',JSON_ARRAY('查我的报销'),NULL,NULL,NULL,NULL,NULL,'expense.query_my_expenses','EXPENSE','受控查询工具目录',1,'PUBLISHED','2026-10-12',0,0,NOW())
ON DUPLICATE KEY UPDATE item_key=item_key;

INSERT INTO sys_permission
  (permission_code,permission_name,permission_type,parent_id,module_code,sort_order,status)
SELECT p.code,p.name,'BUTTON',parent.id,'agentBusinessOntology',p.sort_order,1
FROM (
  SELECT 'agent:business_ontology:edit' code,'编辑业务本体' name,56051 sort_order
  UNION ALL SELECT 'agent:business_ontology:publish','审核发布业务本体',56052
) p JOIN sys_permission parent ON parent.permission_code='agent:business_ontology:view'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission existing WHERE existing.permission_code=p.code);

INSERT IGNORE INTO sys_role_permission (role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE UPPER(r.role_code) IN ('SUPER_ADMIN','SUPERADMIN')
  AND p.permission_code IN ('agent:business_ontology:edit','agent:business_ontology:publish');
