-- 执行前必须由运维显式设置真实公司编码，例如：SET @finex_company_id = 'GROUP_HQ';
-- 未设置时默认配置插入将因 NOT NULL 约束失败，禁止回退到猜测的公司 1。
SET @finex_company_id = NULLIF(TRIM(@finex_company_id), '');

-- 脱敏实体类型配置表
CREATE TABLE IF NOT EXISTS ea_desensitization_entity_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  company_id VARCHAR(64) NOT NULL,
  code VARCHAR(50) NOT NULL COMMENT '实体类型编码',
  name VARCHAR(100) NOT NULL COMMENT '实体类型名称',
  description TEXT COMMENT '描述',
  placeholder_prefix VARCHAR(50) NOT NULL COMMENT '占位符前缀',
  regex_patterns TEXT COMMENT '正则表达式（JSON数组）',
  examples TEXT COMMENT '示例（JSON数组）',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_by BIGINT COMMENT '创建人',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_tenant_code (tenant_id, company_id, code),
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_enabled (enabled)
) COMMENT='脱敏实体类型配置';

-- 插入默认配置
INSERT INTO ea_desensitization_entity_type (tenant_id, company_id, code, name, description, placeholder_prefix, regex_patterns, examples, enabled)
VALUES
('default', @finex_company_id, 'PERSON', '人名', '员工姓名、客户姓名等', 'PERSON',
 '["[一-龥]{2,4}"]',
 '["张三", "李四", "王小明"]', 1),

('default', @finex_company_id, 'AMOUNT', '金额', '报销金额、发票金额等', 'AMOUNT',
 '["\\\\d+(\\\\.\\\\d{1,2})?元", "¥\\\\d+(\\\\.\\\\d{1,2})?", "\\\\d+(\\\\.\\\\d{1,2})?RMB"]',
 '["1000元", "¥500.50", "1200.00元"]', 1),

('default', @finex_company_id, 'DOCUMENT_ID', '单据编号', '报销单号、发票号等', 'DOC',
 '["EXP\\\\d{8,12}", "INV\\\\d{8,12}", "[A-Z]{2,4}\\\\d{6,12}"]',
 '["EXP20240101001", "INV202401010001"]', 1),

('default', @finex_company_id, 'PHONE', '手机号', '联系电话', 'PHONE',
 '["1[3-9]\\\\d{9}", "\\\\d{3}-\\\\d{4}-\\\\d{4}"]',
 '["13812345678", "021-1234-5678"]', 1),

('default', @finex_company_id, 'ID_CARD', '身份证号', '身份证号码', 'ID',
 '["\\\\d{17}[\\\\dXx]"]',
 '["110101199001011234"]', 1),

('default', @finex_company_id, 'BANK_ACCOUNT', '银行账号', '银行卡号', 'BANK',
 '["\\\\d{16,19}"]',
 '["6222021234567890123"]', 1),

('default', @finex_company_id, 'EMAIL', '邮箱', '电子邮件地址', 'EMAIL',
 '["[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}"]',
 '["zhangsan@company.com"]', 1),

('default', @finex_company_id, 'ADDRESS', '地址', '详细地址', 'ADDR',
 '["[一-龥]+省[一-龥]+市"]',
 '["北京市朝阳区某某街道123号"]', 1);

-- 检查 ea_desensitization_mapping 表是否存在，如果不存在则创建
CREATE TABLE IF NOT EXISTS ea_desensitization_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  mapping_id VARCHAR(64) UNIQUE NOT NULL COMMENT '映射ID（UUID）',
  mappings TEXT NOT NULL COMMENT '映射关系（JSON）',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
  INDEX idx_tenant (tenant_id),
  INDEX idx_session (session_id),
  INDEX idx_mapping_id (mapping_id),
  INDEX idx_expires (expires_at)
) COMMENT='脱敏映射表';

-- 添加定时清理事件（如果不存在）
DROP EVENT IF EXISTS clean_expired_desensitization_mappings;

CREATE EVENT clean_expired_desensitization_mappings
ON SCHEDULE EVERY 1 HOUR
DO
  DELETE FROM ea_desensitization_mapping WHERE expires_at < NOW();
