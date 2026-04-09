USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ea_agent_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_code VARCHAR(32) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    agent_description VARCHAR(500) NULL,
    icon_key VARCHAR(64) NULL,
    theme_key VARCHAR(64) NULL,
    cover_color VARCHAR(32) NULL,
    tags_json TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    latest_version_no INT NOT NULL DEFAULT 1,
    published_version_id BIGINT NULL,
    last_run_id BIGINT NULL,
    last_run_status VARCHAR(32) NULL,
    last_run_summary VARCHAR(500) NULL,
    last_run_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_definition_code (agent_code),
    KEY idx_ea_agent_definition_owner (owner_user_id, status)
);

CREATE TABLE IF NOT EXISTS ea_agent_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_label VARCHAR(64) NULL,
    config_json LONGTEXT NOT NULL,
    published INT NOT NULL DEFAULT 0,
    created_by_user_id BIGINT NOT NULL,
    created_by_name VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_version_agent_no (agent_id, version_no),
    KEY idx_ea_agent_version_agent (agent_id, created_at)
);

CREATE TABLE IF NOT EXISTS ea_agent_trigger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    schedule_mode VARCHAR(32) NULL,
    cron_expression VARCHAR(64) NULL,
    interval_minutes INT NULL,
    event_code VARCHAR(64) NULL,
    config_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_trigger_agent (agent_id, trigger_type)
);

CREATE TABLE IF NOT EXISTS ea_agent_tool_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    tool_code VARCHAR(64) NOT NULL,
    enabled INT NOT NULL DEFAULT 1,
    credential_ref_code VARCHAR(64) NULL,
    config_json TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_tool_binding_agent (agent_id, tool_code)
);

CREATE TABLE IF NOT EXISTS ea_agent_credential_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    credential_code VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    credential_name VARCHAR(100) NOT NULL,
    masked_key VARCHAR(128) NULL,
    secret_payload TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_credential_ref_code (owner_user_id, credential_code)
);

CREATE TABLE IF NOT EXISTS ea_agent_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no VARCHAR(40) NOT NULL,
    agent_id BIGINT NOT NULL,
    agent_version_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    trigger_source VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500) NULL,
    summary VARCHAR(500) NULL,
    input_json LONGTEXT NULL,
    output_json LONGTEXT NULL,
    scheduled_fire_at DATETIME NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_run_no (run_no),
    KEY idx_ea_agent_run_agent (agent_id, created_at),
    KEY idx_ea_agent_run_owner (owner_user_id, created_at)
);

CREATE TABLE IF NOT EXISTS ea_agent_run_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    node_label VARCHAR(100) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(500) NULL,
    input_json LONGTEXT NULL,
    output_json LONGTEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    duration_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_run_step_run (run_id, step_no)
);

CREATE TABLE IF NOT EXISTS ea_agent_run_artifact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    artifact_key VARCHAR(64) NOT NULL,
    artifact_type VARCHAR(32) NOT NULL,
    artifact_name VARCHAR(100) NULL,
    summary VARCHAR(500) NULL,
    content_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ea_agent_run_artifact_run (run_id, artifact_key)
);

CREATE TABLE IF NOT EXISTS ea_agent_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trigger_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    schedule_status VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    last_fire_at DATETIME NULL,
    next_fire_at DATETIME NULL,
    last_run_id BIGINT NULL,
    locked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ea_agent_schedule_trigger (trigger_id),
    KEY idx_ea_agent_schedule_due (schedule_status, next_fire_at),
    KEY idx_ea_agent_schedule_agent (agent_id)
);

-- comment standardization begin
ALTER TABLE ea_agent_credential_ref
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN owner_user_id bigint NOT NULL COMMENT '所属用户ID',
    MODIFY COLUMN credential_code varchar(64) NOT NULL COMMENT '凭据编码',
    MODIFY COLUMN provider_code varchar(64) NOT NULL COMMENT '提供方编码',
    MODIFY COLUMN credential_name varchar(100) NOT NULL COMMENT '凭据名称',
    MODIFY COLUMN masked_key varchar(128) NULL COMMENT '脱敏密钥',
    MODIFY COLUMN secret_payload text NULL COMMENT '敏感凭据载荷',
    MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE启用/DISABLED停用',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理凭据引用表';

ALTER TABLE ea_agent_definition
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN agent_code varchar(32) NOT NULL COMMENT '代理编码',
    MODIFY COLUMN owner_user_id bigint NOT NULL COMMENT '所属用户ID',
    MODIFY COLUMN agent_name varchar(100) NOT NULL COMMENT '代理名称',
    MODIFY COLUMN agent_description varchar(500) NULL COMMENT '代理说明',
    MODIFY COLUMN icon_key varchar(64) NULL COMMENT '图标标识',
    MODIFY COLUMN theme_key varchar(64) NULL COMMENT '主题标识',
    MODIFY COLUMN cover_color varchar(32) NULL COMMENT '封面颜色',
    MODIFY COLUMN tags_json text NULL COMMENT '标签JSON',
    MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT草稿/PUBLISHED已发布/DISABLED停用',
    MODIFY COLUMN latest_version_no int NOT NULL DEFAULT 1 COMMENT '最新版本号',
    MODIFY COLUMN published_version_id bigint NULL COMMENT '已发布版本ID',
    MODIFY COLUMN last_run_id bigint NULL COMMENT '最近运行ID',
    MODIFY COLUMN last_run_status varchar(32) NULL COMMENT '最近运行状态',
    MODIFY COLUMN last_run_summary varchar(500) NULL COMMENT '最近运行摘要',
    MODIFY COLUMN last_run_at datetime NULL COMMENT '最近运行时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理定义表';

ALTER TABLE ea_agent_run
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN run_no varchar(40) NOT NULL COMMENT '运行编号',
    MODIFY COLUMN agent_id bigint NOT NULL COMMENT '代理ID',
    MODIFY COLUMN agent_version_id bigint NOT NULL COMMENT '代理版本ID',
    MODIFY COLUMN owner_user_id bigint NOT NULL COMMENT '所属用户ID',
    MODIFY COLUMN trigger_type varchar(32) NOT NULL COMMENT '触发类型',
    MODIFY COLUMN trigger_source varchar(64) NULL COMMENT '触发来源',
    MODIFY COLUMN status varchar(32) NOT NULL COMMENT '运行状态',
    MODIFY COLUMN error_message varchar(500) NULL COMMENT '错误信息',
    MODIFY COLUMN summary varchar(500) NULL COMMENT '摘要',
    MODIFY COLUMN input_json longtext NULL COMMENT '输入JSON',
    MODIFY COLUMN output_json longtext NULL COMMENT '输出JSON',
    MODIFY COLUMN scheduled_fire_at datetime NULL COMMENT '计划触发时间',
    MODIFY COLUMN started_at datetime NULL COMMENT '开始时间',
    MODIFY COLUMN finished_at datetime NULL COMMENT '结束时间',
    MODIFY COLUMN duration_ms bigint NULL COMMENT '耗时毫秒',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理运行记录表';

ALTER TABLE ea_agent_run_artifact
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN run_id bigint NOT NULL COMMENT '批次ID',
    MODIFY COLUMN artifact_key varchar(64) NOT NULL COMMENT '产物标识',
    MODIFY COLUMN artifact_type varchar(32) NOT NULL COMMENT '产物类型',
    MODIFY COLUMN artifact_name varchar(100) NULL COMMENT '产物名称',
    MODIFY COLUMN summary varchar(500) NULL COMMENT '摘要',
    MODIFY COLUMN content_json longtext NULL COMMENT '内容JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理运行产物表';

ALTER TABLE ea_agent_run_step
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN run_id bigint NOT NULL COMMENT '批次ID',
    MODIFY COLUMN step_no int NOT NULL COMMENT '步骤序号',
    MODIFY COLUMN node_key varchar(64) NOT NULL COMMENT '节点标识',
    MODIFY COLUMN node_type varchar(32) NOT NULL COMMENT '节点类型',
    MODIFY COLUMN node_label varchar(100) NULL COMMENT '节点标签',
    MODIFY COLUMN status varchar(32) NOT NULL COMMENT '步骤状态',
    MODIFY COLUMN error_message varchar(500) NULL COMMENT '错误信息',
    MODIFY COLUMN input_json longtext NULL COMMENT '输入JSON',
    MODIFY COLUMN output_json longtext NULL COMMENT '输出JSON',
    MODIFY COLUMN started_at datetime NULL COMMENT '开始时间',
    MODIFY COLUMN finished_at datetime NULL COMMENT '结束时间',
    MODIFY COLUMN duration_ms bigint NULL COMMENT '耗时毫秒',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理运行步骤表';

ALTER TABLE ea_agent_schedule
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN trigger_id bigint NOT NULL COMMENT '触发器ID',
    MODIFY COLUMN agent_id bigint NOT NULL COMMENT '代理ID',
    MODIFY COLUMN schedule_status varchar(32) NOT NULL DEFAULT 'IDLE' COMMENT '调度状态',
    MODIFY COLUMN last_fire_at datetime NULL COMMENT '上次触发时间',
    MODIFY COLUMN next_fire_at datetime NULL COMMENT '下次触发时间',
    MODIFY COLUMN last_run_id bigint NULL COMMENT '最近运行ID',
    MODIFY COLUMN locked_at datetime NULL COMMENT '锁定时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理调度表';

ALTER TABLE ea_agent_tool_binding
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN agent_id bigint NOT NULL COMMENT '代理ID',
    MODIFY COLUMN tool_code varchar(64) NOT NULL COMMENT '工具编码',
    MODIFY COLUMN enabled int NOT NULL DEFAULT 1 COMMENT '是否启用:1是 0否',
    MODIFY COLUMN credential_ref_code varchar(64) NULL COMMENT '凭据引用编码',
    MODIFY COLUMN config_json text NULL COMMENT '工具绑定配置JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理工具绑定表';

ALTER TABLE ea_agent_trigger
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN agent_id bigint NOT NULL COMMENT '代理ID',
    MODIFY COLUMN trigger_type varchar(32) NOT NULL COMMENT '触发类型',
    MODIFY COLUMN enabled int NOT NULL DEFAULT 1 COMMENT '是否启用:1是 0否',
    MODIFY COLUMN schedule_mode varchar(32) NULL COMMENT '调度模式',
    MODIFY COLUMN cron_expression varchar(64) NULL COMMENT 'Cron表达式',
    MODIFY COLUMN interval_minutes int NULL COMMENT '间隔分钟数',
    MODIFY COLUMN event_code varchar(64) NULL COMMENT '事件编码',
    MODIFY COLUMN config_json text NULL COMMENT '触发配置JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '档案代理触发器表';

ALTER TABLE ea_agent_version
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN agent_id bigint NOT NULL COMMENT '代理ID',
    MODIFY COLUMN version_no int NOT NULL COMMENT '版本号',
    MODIFY COLUMN version_label varchar(64) NULL COMMENT '版本标签',
    MODIFY COLUMN config_json longtext NOT NULL COMMENT '配置JSON',
    MODIFY COLUMN published int NOT NULL DEFAULT 0 COMMENT '是否已发布:1是 0否',
    MODIFY COLUMN created_by_user_id bigint NOT NULL COMMENT '创建人用户ID',
    MODIFY COLUMN created_by_name varchar(100) NULL COMMENT '创建人姓名',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    COMMENT = '档案代理版本表';

-- comment standardization end
