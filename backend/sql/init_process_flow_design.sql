CREATE TABLE IF NOT EXISTS pm_process_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程ID',
    flow_code VARCHAR(64) NOT NULL COMMENT '流程编码',
    flow_name VARCHAR(64) NOT NULL COMMENT '流程名称',
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
    scene_name VARCHAR(64) NOT NULL COMMENT '场景名称',
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
    node_name VARCHAR(64) NOT NULL COMMENT '节点名称',
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
    route_name VARCHAR(64) NOT NULL COMMENT '路由名称',
    priority INT NOT NULL DEFAULT 1 COMMENT '路由优先级，值越小越先匹配',
    default_route TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认路由:1是 0否',
    attach_below_nodes TINYINT NOT NULL DEFAULT 0 COMMENT '是否附带下方节点:1是 0否',
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

-- comment standardization begin
ALTER TABLE pm_process_flow_node
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
    MODIFY COLUMN version_id bigint NOT NULL COMMENT '所属流程版本ID',
    MODIFY COLUMN node_key varchar(64) NOT NULL COMMENT '节点唯一标识',
    MODIFY COLUMN node_type varchar(32) NOT NULL COMMENT '节点类型:APPROVAL审批/CC抄送/PAYMENT支付/BRANCH分支',
    MODIFY COLUMN node_name varchar(64) NOT NULL COMMENT '节点名称',
    MODIFY COLUMN scene_id bigint NULL COMMENT '关联场景ID，仅分支节点可选',
    MODIFY COLUMN parent_node_key varchar(64) NULL COMMENT '父容器节点标识',
    MODIFY COLUMN display_order int NOT NULL DEFAULT 0 COMMENT '节点显示顺序',
    MODIFY COLUMN config_json longtext NULL COMMENT '节点配置JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程节点表';

ALTER TABLE pm_process_flow_route
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '路由ID',
    MODIFY COLUMN version_id bigint NOT NULL COMMENT '所属流程版本ID',
    MODIFY COLUMN route_key varchar(64) NOT NULL COMMENT '路由唯一标识',
    MODIFY COLUMN source_node_key varchar(64) NOT NULL COMMENT '来源节点标识',
    MODIFY COLUMN target_node_key varchar(64) NULL COMMENT '目标节点标识，为空表示流转结束',
    MODIFY COLUMN route_name varchar(64) NOT NULL COMMENT '路由名称',
    MODIFY COLUMN priority int NOT NULL DEFAULT 1 COMMENT '路由优先级，值越小越先匹配',
    MODIFY COLUMN default_route tinyint NOT NULL DEFAULT 0 COMMENT '是否默认路由:1是 0否',
    MODIFY COLUMN attach_below_nodes tinyint NOT NULL DEFAULT 0 COMMENT '是否附带下方节点:1是 0否',
    MODIFY COLUMN condition_json longtext NULL COMMENT '路由条件组JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程路由表';

ALTER TABLE pm_process_flow_scene
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '场景ID',
    MODIFY COLUMN scene_code varchar(64) NOT NULL COMMENT '场景编码',
    MODIFY COLUMN scene_name varchar(64) NOT NULL COMMENT '场景名称',
    MODIFY COLUMN scene_description varchar(500) NULL COMMENT '场景说明',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程场景表';

ALTER TABLE pm_process_flow_version
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '流程版本ID',
    MODIFY COLUMN flow_id bigint NOT NULL COMMENT '所属流程ID',
    MODIFY COLUMN version_no int NOT NULL COMMENT '版本号',
    MODIFY COLUMN version_status varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态:DRAFT草稿/PUBLISHED已发布/HISTORY历史版本',
    MODIFY COLUMN snapshot_json longtext NULL COMMENT '流程快照JSON',
    MODIFY COLUMN published_at datetime NULL COMMENT '发布时间',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程版本表';

-- comment standardization end
