CREATE TABLE IF NOT EXISTS pm_process_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程ID',
    flow_code VARCHAR(64) NOT NULL COMMENT '流程编码',
    flow_name VARCHAR(100) NOT NULL COMMENT '流程名称',
    flow_description VARCHAR(500) NULL COMMENT '流程说明',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '流程状态:DRAFT草稿/ENABLED启用/DISABLED停用',
    current_draft_version_id BIGINT NULL COMMENT '当前草稿版本ID',
    current_published_version_id BIGINT NULL COMMENT '当前已发布版本ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_process_flow_code (flow_code)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '场景ID',
    scene_code VARCHAR(64) NOT NULL COMMENT '场景编码',
    scene_name VARCHAR(100) NOT NULL COMMENT '场景名称',
    scene_description VARCHAR(500) NULL COMMENT '场景说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_process_flow_scene_code (scene_code)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程版本ID',
    flow_id BIGINT NOT NULL COMMENT '所属流程ID',
    version_no INT NOT NULL COMMENT '版本号',
    version_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态:DRAFT草稿/PUBLISHED已发布/HISTORY历史版本',
    snapshot_json LONGTEXT NULL COMMENT '流程快照JSON',
    published_at DATETIME NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_process_flow_version (flow_id, version_no),
    KEY idx_pm_process_flow_version_flow_id (flow_id)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    version_id BIGINT NOT NULL COMMENT '所属流程版本ID',
    node_key VARCHAR(64) NOT NULL COMMENT '节点唯一标识',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型:APPROVAL审批/CC抄送/PAYMENT支付/BRANCH分支',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    scene_id BIGINT NULL COMMENT '关联场景ID，仅分支节点可选',
    parent_node_key VARCHAR(64) NULL COMMENT '父容器节点标识',
    display_order INT NOT NULL DEFAULT 0 COMMENT '节点显示顺序',
    config_json LONGTEXT NULL COMMENT '节点配置JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_process_flow_node (version_id, node_key),
    KEY idx_pm_process_flow_node_version_id (version_id)
);

CREATE TABLE IF NOT EXISTS pm_process_flow_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路由ID',
    version_id BIGINT NOT NULL COMMENT '所属流程版本ID',
    route_key VARCHAR(64) NOT NULL COMMENT '路由唯一标识',
    source_node_key VARCHAR(64) NOT NULL COMMENT '来源节点标识',
    target_node_key VARCHAR(64) NULL COMMENT '目标节点标识，为空表示流转结束',
    route_name VARCHAR(100) NOT NULL COMMENT '路由名称',
    priority INT NOT NULL DEFAULT 1 COMMENT '路由优先级，值越小越先匹配',
    default_route TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认路由:1是 0否',
    condition_json LONGTEXT NULL COMMENT '路由条件组JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
