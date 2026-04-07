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
