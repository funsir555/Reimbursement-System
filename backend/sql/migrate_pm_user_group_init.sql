SET NAMES utf8mb4;

USE finex_db;

CREATE TABLE IF NOT EXISTS pm_user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户组ID',
    parent_id BIGINT NULL COMMENT '父级用户组ID',
    group_code VARCHAR(16) NOT NULL COMMENT '用户组编码，4-2-2 分级',
    group_name VARCHAR(64) NOT NULL COMMENT '用户组名称',
    code_level INT NOT NULL COMMENT '编码层级：1/2/3',
    code_prefix VARCHAR(4) NOT NULL COMMENT '一级编码前缀',
    member_user_ids_json JSON NULL COMMENT '三级功能组成员ID列表',
    scope_condition_groups_json JSON NULL COMMENT '三级功能组管理范围条件组',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_user_group_code (group_code),
    KEY idx_pm_user_group_parent_id (parent_id),
    CONSTRAINT fk_pm_user_group_parent_id
        FOREIGN KEY (parent_id) REFERENCES pm_user_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程管理用户组';

INSERT INTO pm_user_group (
    parent_id,
    group_code,
    group_name,
    code_level,
    code_prefix,
    member_user_ids_json,
    scope_condition_groups_json
)
SELECT
    NULL,
    '0001',
    '冒烟一级组',
    1,
    '0001',
    NULL,
    NULL
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM pm_user_group
    WHERE group_code = '0001'
);
