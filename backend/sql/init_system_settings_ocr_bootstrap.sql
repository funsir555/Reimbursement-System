USE finex_db;

SET NAMES utf8mb4;

/*
仅回填系统设置中心 > API接口 > OCR 启动所需的最小数据库对象。
- 幂等执行：重复运行不会插入重复数据。
- 不依赖整包权限刷新脚本，避免把已有乱码文案再次写入数据库。
*/

CREATE TABLE IF NOT EXISTS sys_ocr_provider_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'OCR 厂商配置ID',
    provider_code VARCHAR(32) NOT NULL COMMENT '厂商编码',
    provider_name VARCHAR(50) NOT NULL COMMENT '厂商名称',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用',
    config_json JSON NULL COMMENT '厂商配置',
    last_test_at DATETIME NULL COMMENT '最近测试时间',
    last_test_status VARCHAR(32) NULL COMMENT '最近测试状态',
    last_test_message VARCHAR(500) NULL COMMENT '最近测试消息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_sys_ocr_provider_config_code UNIQUE (provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云端 OCR 厂商配置表';

INSERT INTO sys_ocr_provider_config (
    provider_code,
    provider_name,
    enabled,
    config_json,
    last_test_status,
    last_test_message
) VALUES
    ('ALIYUN', '阿里云', 0, JSON_OBJECT('endpoint', 'ocr-api.cn-hangzhou.aliyuncs.com', 'connectTimeoutMs', 5000, 'readTimeoutMs', 15000), 'IDLE', '尚未测试'),
    ('TENCENT', '腾讯云', 0, JSON_OBJECT('endpoint', '', 'connectTimeoutMs', 5000, 'readTimeoutMs', 15000), 'IDLE', '待接入'),
    ('BAIDU', '百度云', 0, JSON_OBJECT('endpoint', '', 'connectTimeoutMs', 5000, 'readTimeoutMs', 15000), 'IDLE', '待接入')
ON DUPLICATE KEY UPDATE
    provider_name = VALUES(provider_name),
    config_json = IFNULL(config_json, VALUES(config_json)),
    last_test_status = IFNULL(last_test_status, VALUES(last_test_status)),
    last_test_message = CASE
        WHEN last_test_message IS NULL OR last_test_message = '' THEN VALUES(last_test_message)
        WHEN last_test_status = 'IDLE' THEN VALUES(last_test_message)
        ELSE last_test_message
    END,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT
    'settings:api_interfaces:view',
    'API接口',
    'MENU',
    parent.id,
    'apiInterfaces',
    '/settings?tab=apiInterfaces',
    606,
    1
FROM sys_permission parent
WHERE parent.permission_code = 'settings:menu'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.permission_code = 'settings:api_interfaces:view'
  );

UPDATE sys_permission permission
JOIN sys_permission parent
  ON parent.permission_code = 'settings:menu'
SET permission.permission_name = 'API接口',
    permission.permission_type = 'MENU',
    permission.parent_id = parent.id,
    permission.module_code = 'apiInterfaces',
    permission.route_path = '/settings?tab=apiInterfaces',
    permission.sort_order = 606,
    permission.status = 1
WHERE permission.permission_code = 'settings:api_interfaces:view';

INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT
    'settings:api_interfaces:ocr_edit',
    '编辑 OCR 配置',
    'BUTTON',
    parent.id,
    'apiInterfaces',
    NULL,
    6061,
    1
FROM sys_permission parent
WHERE parent.permission_code = 'settings:api_interfaces:view'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.permission_code = 'settings:api_interfaces:ocr_edit'
  );

UPDATE sys_permission permission
JOIN sys_permission parent
  ON parent.permission_code = 'settings:api_interfaces:view'
SET permission.permission_name = '编辑 OCR 配置',
    permission.permission_type = 'BUTTON',
    permission.parent_id = parent.id,
    permission.module_code = 'apiInterfaces',
    permission.route_path = NULL,
    permission.sort_order = 6061,
    permission.status = 1
WHERE permission.permission_code = 'settings:api_interfaces:ocr_edit';

INSERT INTO sys_permission (
    permission_code,
    permission_name,
    permission_type,
    parent_id,
    module_code,
    route_path,
    sort_order,
    status
)
SELECT
    'settings:api_interfaces:ocr_test',
    '测试 OCR 配置',
    'BUTTON',
    parent.id,
    'apiInterfaces',
    NULL,
    6062,
    1
FROM sys_permission parent
WHERE parent.permission_code = 'settings:api_interfaces:view'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.permission_code = 'settings:api_interfaces:ocr_test'
  );

UPDATE sys_permission permission
JOIN sys_permission parent
  ON parent.permission_code = 'settings:api_interfaces:view'
SET permission.permission_name = '测试 OCR 配置',
    permission.permission_type = 'BUTTON',
    permission.parent_id = parent.id,
    permission.module_code = 'apiInterfaces',
    permission.route_path = NULL,
    permission.sort_order = 6062,
    permission.status = 1
WHERE permission.permission_code = 'settings:api_interfaces:ocr_test';

INSERT INTO sys_role (role_code, role_name, role_description, status)
SELECT 'SUPER_ADMIN', '超级管理员', '拥有系统全部启用权限', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
JOIN sys_permission permission
  ON permission.permission_code IN (
      'settings:api_interfaces:view',
      'settings:api_interfaces:ocr_edit',
      'settings:api_interfaces:ocr_test'
  )
WHERE role.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user.id, role.id
FROM sys_user user
JOIN sys_role role
  ON role.role_code = 'SUPER_ADMIN'
WHERE user.username = 'admin';
