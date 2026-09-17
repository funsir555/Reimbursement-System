-- FinEx AI Agent Platform - 数据库初始化脚本
-- 版本: v1.0
-- 创建时间: 2026-09-12
-- 说明: 阶段 0 核心表结构

-- ============================================================
-- 1. Agent 会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  company_id BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  context_json JSON COMMENT '上下文数据',
  summary_text TEXT COMMENT '会话摘要',
  message_count INT DEFAULT 0 COMMENT '消息数量',
  last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  expires_at TIMESTAMP NULL COMMENT '过期时间',
  trace_id VARCHAR(64) COMMENT '追踪ID',
  deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
  created_by BIGINT COMMENT '创建人',
  version_no INT DEFAULT 0 COMMENT '版本号',
  UNIQUE KEY uk_session (session_id),
  INDEX idx_tenant_user (tenant_id, company_id, user_id),
  INDEX idx_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent会话表';

-- ============================================================
-- 2. Agent 消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  role ENUM('user', 'assistant', 'system') NOT NULL COMMENT '角色',
  content TEXT NOT NULL COMMENT '消息内容',
  masked_content TEXT COMMENT '脱敏后内容',
  intent VARCHAR(50) COMMENT '意图分类',
  tokens INT COMMENT 'Token数量',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  trace_id VARCHAR(64) COMMENT '追踪ID',
  INDEX idx_session (session_id),
  INDEX idx_tenant (tenant_id),
  INDEX idx_trace (trace_id),
  INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent消息表';

-- ============================================================
-- 3. 工具执行日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_tool_execution_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  company_id BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  session_id VARCHAR(64) COMMENT '会话ID',
  run_id BIGINT COMMENT 'Agent Run ID',
  tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
  tool_version VARCHAR(20) COMMENT '工具版本',
  input_params JSON COMMENT '输入参数',
  output_result JSON COMMENT '输出结果',
  status ENUM('success', 'failure', 'timeout') NOT NULL COMMENT '执行状态',
  error_message TEXT COMMENT '错误信息',
  execution_time_ms INT COMMENT '执行时间(毫秒)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  created_by BIGINT COMMENT '创建人',
  trace_id VARCHAR(64) COMMENT '追踪ID',
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_session (session_id),
  INDEX idx_tool_time (tool_name, created_at),
  INDEX idx_trace (trace_id),
  INDEX idx_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具执行日志表';

-- ============================================================
-- 4. LLM API 调用日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_llm_api_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  session_id VARCHAR(64) COMMENT '会话ID',
  provider VARCHAR(50) NOT NULL COMMENT '模型提供商',
  model VARCHAR(50) COMMENT '模型名称',
  prompt_tokens INT COMMENT 'Prompt Tokens',
  completion_tokens INT COMMENT 'Completion Tokens',
  total_cost DECIMAL(10,6) COMMENT '总成本(元)',
  latency_ms INT COMMENT '延迟(毫秒)',
  status ENUM('success', 'failure', 'timeout') NOT NULL COMMENT '状态',
  error_message TEXT COMMENT '错误信息',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  trace_id VARCHAR(64) COMMENT '追踪ID',
  INDEX idx_tenant_session (tenant_id, session_id),
  INDEX idx_provider_time (provider, created_at),
  INDEX idx_trace (trace_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM API调用日志表';

-- ============================================================
-- 5. 脱敏映射表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_desensitization_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  mapping_id VARCHAR(64) NOT NULL COMMENT '映射ID',
  mappings JSON NOT NULL COMMENT '映射数据',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
  UNIQUE KEY uk_mapping_id (mapping_id),
  INDEX idx_session (session_id),
  INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='脱敏映射表';

-- ============================================================
-- 6. 用户记忆表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_user_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  company_id BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  memory_type VARCHAR(50) NOT NULL COMMENT '记忆类型',
  memory_key VARCHAR(100) NOT NULL COMMENT '记忆键',
  memory_value JSON COMMENT '记忆值',
  confidence_score DECIMAL(5,4) COMMENT '置信度',
  source VARCHAR(50) COMMENT '来源',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_memory (tenant_id, company_id, user_id, memory_type, memory_key),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户记忆表';

-- ============================================================
-- 7. 数字员工配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_digital_employee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  company_id BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  name VARCHAR(100) NOT NULL COMMENT '数字员工名称',
  type ENUM('审核型', '执行型', '咨询型', '催办型') NOT NULL COMMENT '类型',
  responsible_user_id BIGINT NOT NULL COMMENT '责任人ID',
  authority_level INT NOT NULL COMMENT '权限等级',
  max_amount DECIMAL(15,2) COMMENT '最大处理金额',
  config JSON COMMENT '配置参数',
  status ENUM('active', 'suspended') DEFAULT 'active' COMMENT '状态',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  created_by BIGINT NOT NULL COMMENT '创建人',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (responsible_user_id) REFERENCES sys_user(id),
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_responsible (responsible_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字员工配置表';

-- ============================================================
-- 8. 数字员工操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_digital_employee_action_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  employee_id BIGINT NOT NULL COMMENT '数字员工ID',
  responsible_user_id BIGINT NOT NULL COMMENT '责任人ID',
  action_type VARCHAR(50) NOT NULL COMMENT '操作类型',
  business_object_type VARCHAR(50) COMMENT '业务对象类型',
  business_object_id BIGINT COMMENT '业务对象ID',
  decision_reason TEXT COMMENT '决策原因',
  confidence_score DECIMAL(5,4) COMMENT '置信度',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  trace_id VARCHAR(64) COMMENT '追踪ID',
  FOREIGN KEY (employee_id) REFERENCES ea_digital_employee(id),
  INDEX idx_tenant (tenant_id),
  INDEX idx_employee_time (employee_id, created_at),
  INDEX idx_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字员工操作日志表';

-- ============================================================
-- 9. 实体定义表（业务本体）
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_entity_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  entity_key VARCHAR(50) NOT NULL COMMENT '实体键',
  display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
  table_name VARCHAR(100) COMMENT '表名',
  description TEXT COMMENT '描述',
  parent_entity_key VARCHAR(50) COMMENT '父实体键',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_entity_key (entity_key),
  INDEX idx_parent (parent_entity_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实体定义表';

-- ============================================================
-- 10. 字段语义表（业务本体）
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_field_semantics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  entity_key VARCHAR(50) NOT NULL COMMENT '实体键',
  field_key VARCHAR(50) NOT NULL COMMENT '字段键',
  field_type VARCHAR(20) COMMENT '字段类型',
  display_name VARCHAR(100) COMMENT '显示名称',
  aliases JSON COMMENT '别名',
  is_sensitive BOOLEAN DEFAULT FALSE COMMENT '是否敏感',
  is_searchable BOOLEAN DEFAULT TRUE COMMENT '是否可搜索',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_entity_field (entity_key, field_key),
  INDEX idx_entity (entity_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段语义表';

-- ============================================================
-- 11. 枚举值语义表（业务本体）
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_enum_semantics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  entity_key VARCHAR(50) NOT NULL COMMENT '实体键',
  field_key VARCHAR(50) NOT NULL COMMENT '字段键',
  db_value VARCHAR(50) NOT NULL COMMENT '数据库值',
  display_value VARCHAR(100) NOT NULL COMMENT '显示值',
  aliases JSON COMMENT '别名',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_enum (entity_key, field_key, db_value),
  INDEX idx_entity_field (entity_key, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='枚举值语义表';

-- ============================================================
-- 12. 知识库文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  tenant_id VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '租户ID',
  company_id BIGINT NOT NULL DEFAULT 1 COMMENT '公司ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  category ENUM('制度政策', '会计准则', '税务法规', '流程SOP', '合同模板', '历史案例') NOT NULL COMMENT '分类',
  file_path VARCHAR(500) COMMENT '文件路径',
  file_hash VARCHAR(64) COMMENT '文件哈希',
  version VARCHAR(20) COMMENT '版本',
  status ENUM('active', 'archived', 'expired') DEFAULT 'active' COMMENT '状态',
  valid_from DATE COMMENT '生效日期',
  valid_until DATE COMMENT '失效日期',
  uploaded_by BIGINT COMMENT '上传人',
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  vector_indexed BOOLEAN DEFAULT FALSE COMMENT '是否已向量化',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_tenant_company (tenant_id, company_id),
  INDEX idx_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- ============================================================
-- 13. 知识库分块表
-- ============================================================
CREATE TABLE IF NOT EXISTS ea_knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  document_id BIGINT NOT NULL COMMENT '文档ID',
  chunk_index INT NOT NULL COMMENT '分块索引',
  chunk_id VARCHAR(100) NOT NULL COMMENT '分块ID',
  content TEXT NOT NULL COMMENT '内容',
  content_hash VARCHAR(64) COMMENT '内容哈希',
  vector_id VARCHAR(100) COMMENT '向量ID',
  metadata JSON COMMENT '元数据',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (document_id) REFERENCES ea_knowledge_document(id) ON DELETE CASCADE,
  UNIQUE KEY uk_chunk_id (chunk_id),
  INDEX idx_document (document_id),
  INDEX idx_document_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分块表';

-- ============================================================
-- 扩展现有 agent_run 表（添加执行上下文字段）
-- ============================================================
ALTER TABLE agent_run
ADD COLUMN IF NOT EXISTS execution_context JSON COMMENT '执行上下文' AFTER run_output;

-- ============================================================
-- 定时清理过期脱敏映射
-- ============================================================
DROP EVENT IF EXISTS clean_expired_mappings;

CREATE EVENT clean_expired_mappings
ON SCHEDULE EVERY 1 HOUR
DO
  DELETE FROM ea_desensitization_mapping WHERE expires_at < NOW();
