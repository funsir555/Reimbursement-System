CREATE TABLE IF NOT EXISTS pm_process_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_code VARCHAR(64) NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    flow_description VARCHAR(500) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    current_draft_version_id BIGINT NULL,
    current_published_version_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_process_flow_code (flow_code)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_code VARCHAR(64) NOT NULL,
    scene_name VARCHAR(100) NOT NULL,
    scene_description VARCHAR(500) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_process_flow_scene_code (scene_code)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    snapshot_json LONGTEXT NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_process_flow_version (flow_id, version_no),
    KEY idx_pm_process_flow_version_flow_id (flow_id)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    scene_id BIGINT NULL,
    parent_node_key VARCHAR(64) NULL,
    display_order INT NOT NULL DEFAULT 0,
    config_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_process_flow_node (version_id, node_key),
    KEY idx_pm_process_flow_node_version_id (version_id)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    route_key VARCHAR(64) NOT NULL,
    source_node_key VARCHAR(64) NOT NULL,
    target_node_key VARCHAR(64) NULL,
    route_name VARCHAR(100) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    default_route TINYINT NOT NULL DEFAULT 0,
    condition_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_process_flow_route (version_id, route_key),
    KEY idx_pm_process_flow_route_version_id (version_id),
    KEY idx_pm_process_flow_route_source_node_key (source_node_key)
);

INSERT INTO pm_process_flow_scene (scene_code, scene_name, scene_description, status)
SELECT 'PS202603270001', CONVERT(0xE6A087E58786E8B4B9E794A8E5AEA1E689B9 USING utf8mb4), CONVERT(0xE98082E794A8E4BA8EE5B8B8E8A784E68AA5E99480E4B88EE794B3E8AFB7E6B581E7A88B USING utf8mb4), 1
WHERE NOT EXISTS (SELECT 1 FROM pm_process_flow_scene WHERE scene_code = 'PS202603270001');

INSERT INTO pm_process_flow_scene (scene_code, scene_name, scene_description, status)
SELECT 'PS202603270002', CONVERT(0xE8B4A2E58AA1E5A48DE6A0B8 USING utf8mb4), CONVERT(0xE98082E794A8E4BA8EE8B4A2E58AA1E5AEA1E6A0B8E4B88EE4BB98E6ACBEE5898DE5A48DE6A0B8 USING utf8mb4), 1
WHERE NOT EXISTS (SELECT 1 FROM pm_process_flow_scene WHERE scene_code = 'PS202603270002');

INSERT INTO pm_process_flow_scene (scene_code, scene_name, scene_description, status)
SELECT 'PS202603270003', CONVERT(0xE587BAE7BAB3E694AFE4BB98 USING utf8mb4), CONVERT(0xE98082E794A8E4BA8EE694AFE4BB98E689A7E8A18CE4B88EE4BB98E6ACBEE7A1AEE8AEA4E88A82E782B9 USING utf8mb4), 1
WHERE NOT EXISTS (SELECT 1 FROM pm_process_flow_scene WHERE scene_code = 'PS202603270003');