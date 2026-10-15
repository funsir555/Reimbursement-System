SET NAMES utf8mb4;

-- Run after migrate_agent_business_ontology_20261012.sql. These are reviewed product terms,
-- not company data. Physical schema evidence is internal and is never projected to a model.
CREATE TABLE IF NOT EXISTS ea_ontology_result_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  link_key VARCHAR(128) NOT NULL,
  version_no INT NOT NULL,
  source_tool_code VARCHAR(128) NOT NULL,
  source_field VARCHAR(80) NOT NULL,
  source_label VARCHAR(100) NOT NULL,
  target_tool_code VARCHAR(128) NOT NULL,
  target_field VARCHAR(80) NOT NULL,
  target_label VARCHAR(100) NOT NULL,
  relation_kind VARCHAR(24) NOT NULL,
  explanation VARCHAR(600) NOT NULL,
  scope_note VARCHAR(400) NOT NULL,
  schema_evidence VARCHAR(500) NOT NULL COMMENT 'Internal table/column/index or FK evidence; never sent to a model',
  owner_domain VARCHAR(32) NOT NULL,
  source_ref VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL,
  effective_from DATE NOT NULL,
  effective_until DATE NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ontology_result_link_version (link_key,version_no),
  KEY idx_ontology_result_link_active (status,effective_from,effective_until),
  KEY idx_ontology_result_link_tools (source_tool_code,target_tool_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受控查询 JSON 字段关系及内部结构证据';

INSERT INTO ea_ontology_item
  (item_key,kind,entity_key,display_name,description,aliases_json,owner_domain,source_ref,
   version_no,status,effective_from,created_by,reviewed_by,reviewed_at)
VALUES
  ('finance_management','ENTITY',NULL,'财务管理','负责账套、会计期间、科目、凭证、总账、往来及相关财务档案的业务域；账务事实与计算口径由 Finance 服务负责。',JSON_ARRAY('财务模块','Finance'),'FINANCE','系统最终目标与开发架构规范 4.3',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_account_set','ENTITY',NULL,'账套','财务数据所属的公司核算范围；跨工具比较编码前，应确认查询来自同一账套。',JSON_ARRAY('财务账套'),'FINANCE','FinanceContextService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_period','ENTITY',NULL,'会计期间','账务发生和余额归属的年度与期间；同一科目在不同期间的余额不能直接视为同一条记录。',JSON_ARRAY('期间','财务期间'),'FINANCE','FinanceContextService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_subject','ENTITY',NULL,'会计科目','按账套维护的核算项目，具有科目编码、名称、层级和余额方向；父级科目可汇总下级。',JSON_ARRAY('科目','科目档案'),'FINANCE','FinanceAgentQueryService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_voucher_entry','ENTITY',NULL,'凭证分录','记账凭证中的一条会计记录，包含科目、摘要以及借方或贷方发生额；一张凭证可有多条分录。',JSON_ARRAY('分录','凭证行'),'FINANCE','FinanceVoucherService / FinanceAgentQueryService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_sequence','ENTITY',NULL,'序时账','按日期和凭证顺序展示的凭证分录查询结果；分页结果只代表当前授权及筛选范围。',JSON_ARRAY('凭证序时账'),'FINANCE','FinanceAgentQueryService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_balance_sheet','ENTITY',NULL,'余额表','按科目和期间汇总的期初、本期发生与期末余额；一行不是一条凭证分录。',JSON_ARRAY('科目余额表','余额查询'),'FINANCE','FinanceAgentQueryService / 总账共享余额口径',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_supplier_ledger','ENTITY',NULL,'供应商明细账','按供应商和会计科目查看授权范围内的发生额与余额；供应商名称仅用于展示，不能当作唯一关联键。',JSON_ARRAY('供应商账簿','供应商往来明细'),'FINANCE','FinanceAgentSupplierLedgerReadService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_project_class','ENTITY',NULL,'项目分类','同一账套下对项目档案进行归类的编码和名称。',JSON_ARRAY('项目大类'),'FINANCE','FinanceAgentQueryService / fin_project_class',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_project','ENTITY',NULL,'项目档案','同一账套下的项目编码、名称及所属分类；项目编码用于与凭证分录的项目维度对应。',JSON_ARRAY('核算项目','项目名称'),'FINANCE','FinanceAgentQueryService / fin_project_archive',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_customer','ENTITY',NULL,'客户档案','客户往来核算所用的客户编码和展示名称；名称不能代替编码进行关联。',JSON_ARRAY('客户'),'FINANCE','FinanceAgentQueryService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_vendor','ENTITY',NULL,'供应商档案','供应商往来核算所用的供应商编码和展示名称；名称不能代替编码进行关联。',JSON_ARRAY('供应商'),'FINANCE','FinanceAgentQueryService',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_balance_rule','RULE','finance_balance_sheet','借贷余额口径','期初和期末净额以借方为正、贷方为负；期末净额等于期初净额加本期借方发生额再减本期贷方发生额。展示时拆为非负的借方或贷方余额。',JSON_ARRAY('期末余额','借贷方向'),'FINANCE','系统最终目标与开发架构规范 4.3',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_sequence.query','CAPABILITY','finance_sequence','查询序时账','通过受控工具查询有权查看的凭证分录；工具权限、公司和期间由服务端校验。',JSON_ARRAY('查询凭证'), 'FINANCE','ControlledQueryCatalog',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_balance_sheet.query','CAPABILITY','finance_balance_sheet','查询余额表','通过受控工具查询授权账套及期间的科目汇总余额。',JSON_ARRAY('查询科目余额'),'FINANCE','ControlledQueryCatalog',1,'PUBLISHED','2026-10-12',0,0,NOW()),
  ('finance_subject.query','CAPABILITY','finance_subject','查询会计科目','通过受控工具查询授权账套的科目档案。',JSON_ARRAY('查询科目档案'),'FINANCE','ControlledQueryCatalog',1,'PUBLISHED','2026-10-12',0,0,NOW())
ON DUPLICATE KEY UPDATE item_key=item_key;

UPDATE ea_ontology_item SET tool_code='finance.query_sequence_entries'
WHERE item_key='finance_sequence.query' AND version_no=1 AND tool_code IS NULL;
UPDATE ea_ontology_item SET tool_code='finance.query_balance_sheet'
WHERE item_key='finance_balance_sheet.query' AND version_no=1 AND tool_code IS NULL;
UPDATE ea_ontology_item SET tool_code='finance.query_account_subjects'
WHERE item_key='finance_subject.query' AND version_no=1 AND tool_code IS NULL;

INSERT INTO ea_ontology_result_link
  (link_key,version_no,source_tool_code,source_field,source_label,target_tool_code,target_field,target_label,
   relation_kind,explanation,scope_note,schema_evidence,owner_domain,source_ref,status,effective_from,reviewed_by,reviewed_at)
VALUES
  ('sequence_subject',1,'finance.query_sequence_entries','ccode','分录科目编码','finance.query_account_subjects','subject_code','科目编码',
   'REFERENCE','分录使用的科目编码对应科目档案编码；一个科目可出现在多条分录中。','仅在两个查询的账套范围一致时对应；编码相同不代表跨账套同一科目。','gl_accvouch(company_id,ccode): idx_gl_accvouch_company_ccode; fin_account_subject(company_id,subject_code): uk_fin_account_subject; no FK','FINANCE','init_finance_gl.sql / init_finance_system_management.sql','PUBLISHED','2026-10-12',0,NOW()),
  ('balance_subject',1,'finance.query_balance_sheet','subjectCode','余额表科目编码','finance.query_account_subjects','subject_code','科目编码',
   'AGGREGATE','余额表科目行按科目编码对应科目档案；余额值是期间汇总，不是一条凭证。','同一账套且余额表期间明确；父科目汇总不得与子科目重复相加。','gl_accsum(company_id,iyear,iperiod,ccode,currency_code): unique; fin_account_subject(company_id,subject_code): uk_fin_account_subject','FINANCE','FinanceAgentQueryService / 总账共享余额口径','PUBLISHED','2026-10-12',0,NOW()),
  ('subject_parent',1,'finance.query_account_subjects','parent_subject_code','上级科目编码','finance.query_account_subjects','subject_code','科目编码',
   'HIERARCHY','子科目的上级编码指向同一账套的父科目；空上级编码表示无上级。','仅在同一账套内有效；父子级余额不能重复计入合计。','fin_account_subject(company_id,parent_subject_code): idx_fin_account_subject_company_parent; target uk_fin_account_subject; no self FK','FINANCE','init_finance_system_management.sql','PUBLISHED','2026-10-12',0,NOW()),
  ('project_class',1,'finance.query_projects','citemccode','项目分类编码','finance.query_project_classes','project_class_code','分类编码',
   'REFERENCE','项目档案的分类编码指向其项目分类。','必须属于同一账套。','fin_project_archive(company_id,citemccode) FK -> fin_project_class(company_id,project_class_code); fk_fin_project_archive_class','FINANCE','init_finance_project_archive.sql','PUBLISHED','2026-10-12',0,NOW()),
  ('sequence_project',1,'finance.query_sequence_entries','citem_id','分录项目编码','finance.query_projects','citemcode','项目编码',
   'REFERENCE','分录项目编码对应项目档案编码；一项目可关联多条分录。','须在同一账套；有项目分类时还应核对分类编码。','gl_accvouch(company_id,citem_id): idx_gl_accvouch_company_citem_id; fin_project_archive(company_id,citemcode): uk_fin_project_archive_company_code; no FK','FINANCE','init_finance_gl.sql / init_finance_project_archive.sql','PUBLISHED','2026-10-12',0,NOW()),
  ('sequence_project_class',1,'finance.query_sequence_entries','citem_class','分录项目分类编码','finance.query_project_classes','project_class_code','分类编码',
   'REFERENCE','分录中的项目分类编码对应项目分类；与项目编码组合使用可校验归类。','须在同一账套，并以项目档案所属分类为准。','gl_accvouch(company_id,citem_class,citem_id): idx_gl_accvouch_company_citem_class_id; no FK','FINANCE','init_finance_gl.sql','PUBLISHED','2026-10-12',0,NOW()),
  ('sequence_vendor',1,'finance.query_sequence_entries','csup_id','分录供应商编码','finance.query_vendors','cVenCode','供应商编码',
   'REFERENCE','分录供应商编码对应供应商档案编码；不要用供应商名称作连接键。','仅在同一账套的授权查询结果中比较。','gl_accvouch(company_id,csup_id): idx_gl_accvouch_company_csup; gl_Vender(cVenCode,company_id); no FK','FINANCE','init_finance_gl.sql / FinanceAgentQueryService','PUBLISHED','2026-10-12',0,NOW()),
  ('sequence_customer',1,'finance.query_sequence_entries','ccus_id','分录客户编码','finance.query_customers','cCusCode','客户编码',
   'REFERENCE','分录客户编码对应客户档案编码；不要用客户名称作连接键。','仅在同一账套的授权查询结果中比较。','gl_accvouch(company_id,ccus_id): idx_gl_accvouch_company_ccus; gl_Customer(cCusCode,company_id); no FK','FINANCE','init_finance_gl.sql / FinanceAgentQueryService','PUBLISHED','2026-10-12',0,NOW())
ON DUPLICATE KEY UPDATE link_key=link_key;
