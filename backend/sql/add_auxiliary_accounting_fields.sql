-- 辅助核算配置功能 - 数据库结构扩展
-- 执行时间：2026-09-11
-- 用途：为凭证科目映射和模板策略添加辅助核算规则字段

-- 1. 扩展 exp_voucher_subject_mapping 表（借方科目映射）
ALTER TABLE exp_voucher_subject_mapping
ADD COLUMN person_rule VARCHAR(32) NULL COMMENT '个人核算规则：SUBMITTER(按提单人)|PAYEE(按收款人)' AFTER debit_account_name,
ADD COLUMN supplier_rule VARCHAR(32) NULL COMMENT '供应商核算规则：PAYEE_COMPANY(按收款单位)' AFTER person_rule,
ADD COLUMN dept_rule VARCHAR(32) NULL COMMENT '部门核算规则：SUBMITTER_DEPT(按提单人部门)|EXPENSE_DEPT(按承担部门)' AFTER supplier_rule,
ADD COLUMN project_rule VARCHAR(32) NULL COMMENT '项目核算规则：BY_PROJECT(按项目)' AFTER dept_rule;

-- 2. 扩展 exp_voucher_template_policy 表（贷方科目策略）
ALTER TABLE exp_voucher_template_policy
ADD COLUMN person_rule VARCHAR(32) NULL COMMENT '个人核算规则：SUBMITTER(按提单人)|PAYEE(按收款人)' AFTER credit_account_name,
ADD COLUMN supplier_rule VARCHAR(32) NULL COMMENT '供应商核算规则：PAYEE_COMPANY(按收款单位)' AFTER person_rule,
ADD COLUMN dept_rule VARCHAR(32) NULL COMMENT '部门核算规则：SUBMITTER_DEPT(按提单人部门)|EXPENSE_DEPT(按承担部门)' AFTER supplier_rule,
ADD COLUMN project_rule VARCHAR(32) NULL COMMENT '项目核算规则：BY_PROJECT(按项目)' AFTER dept_rule;

-- 3. 扩展 exp_voucher_push_entry 表（凭证分录实际辅助核算值）
ALTER TABLE exp_voucher_push_entry
ADD COLUMN person_id BIGINT NULL COMMENT '个人核算ID' AFTER amount,
ADD COLUMN person_name VARCHAR(100) NULL COMMENT '个人核算名称' AFTER person_id,
ADD COLUMN supplier_id BIGINT NULL COMMENT '供应商核算ID' AFTER person_name,
ADD COLUMN supplier_name VARCHAR(200) NULL COMMENT '供应商核算名称' AFTER supplier_id,
ADD COLUMN dept_id BIGINT NULL COMMENT '部门核算ID' AFTER supplier_name,
ADD COLUMN dept_name VARCHAR(100) NULL COMMENT '部门核算名称' AFTER dept_id,
ADD COLUMN project_id BIGINT NULL COMMENT '项目核算ID' AFTER dept_name,
ADD COLUMN project_name VARCHAR(100) NULL COMMENT '项目核算名称' AFTER project_id;

-- 4. 添加索引以提升查询性能
CREATE INDEX idx_person_id ON exp_voucher_push_entry(person_id);
CREATE INDEX idx_supplier_id ON exp_voucher_push_entry(supplier_id);
CREATE INDEX idx_dept_id ON exp_voucher_push_entry(dept_id);
CREATE INDEX idx_project_id ON exp_voucher_push_entry(project_id);
