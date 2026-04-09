USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_custom_archive_design (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自定义档案ID',
    archive_code VARCHAR(64) NOT NULL COMMENT '档案编码',
    archive_name VARCHAR(100) NOT NULL COMMENT '档案名称',
    archive_type VARCHAR(32) NOT NULL COMMENT '档案类型:SELECT可选档案/AUTO_RULE自动划分',
    archive_description VARCHAR(255) NULL COMMENT '档案说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_archive_code (archive_code),
    KEY idx_archive_type (archive_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案设计表';

CREATE TABLE IF NOT EXISTS pm_custom_archive_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '档案结果项ID',
    archive_id BIGINT NOT NULL COMMENT '所属档案ID',
    item_code VARCHAR(64) NOT NULL COMMENT '结果项编码',
    item_name VARCHAR(100) NOT NULL COMMENT '结果项名称',
    priority INT NULL DEFAULT 1 COMMENT '优先级，值越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_archive_item_code (archive_id, item_code),
    KEY idx_archive_item_status (archive_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案结果项表';

CREATE TABLE IF NOT EXISTS pm_custom_archive_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自动划分规则ID',
    archive_item_id BIGINT NOT NULL COMMENT '归属结果项ID',
    group_no INT NOT NULL DEFAULT 1 COMMENT '规则组号，同组条件为且、组间为或',
    field_key VARCHAR(64) NOT NULL COMMENT '匹配字段标识',
    operator VARCHAR(32) NOT NULL COMMENT '比较运算符:EQ/NE/IN/NOT_IN/GT/BETWEEN/CONTAINS',
    compare_value VARCHAR(500) NULL COMMENT '比较值，按JSON序列化存储',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_archive_item_group (archive_item_id, group_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义档案自动划分规则表';

INSERT INTO pm_custom_archive_design (archive_code, archive_name, archive_type, archive_description, status)
VALUES
    (
        'PROCESS_TAG_OPTIONS',
        CONVERT(0xE6A087E7ADBEE8AEBEE7BDAE USING utf8mb4),
        'SELECT',
        CONVERT(0xE794A8E4BA8EE6B581E7A88BE7AEA1E79086E4B8ADE6A087E7ADBEE8AEBEE7BDAEE79A84E9BB98E8AEA4E98089E68BA9E6A1A3E6A188 USING utf8mb4),
        1
    ),
    (
        'PROCESS_INSTALLMENT_OPTIONS',
        CONVERT(0xE58886E69C9FE4BB98E6ACBE USING utf8mb4),
        'SELECT',
        CONVERT(0xE794A8E4BA8EE6B581E7A88BE7AEA1E79086E4B8ADE58886E69C9FE4BB98E6ACBEE79A84E9BB98E8AEA4E98089E68BA9E6A1A3E6A188 USING utf8mb4),
        1
    )
ON DUPLICATE KEY UPDATE
    archive_name = VALUES(archive_name),
    archive_type = VALUES(archive_type),
    archive_description = VALUES(archive_description),
    status = VALUES(status);

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'high-frequency', CONVERT(0xE9AB98E9A291E68AA5E99480 USING utf8mb4), 1, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'high-frequency'
  );

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'public-payment', CONVERT(0xE5AFB9E585ACE694AFE4BB98 USING utf8mb4), 2, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'public-payment'
  );

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'ai-audit', CONVERT(0x4149E5AEA1E6A0B8 USING utf8mb4), 3, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'ai-audit'
  );

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'phase-payment', CONVERT(0xE998B6E6AEB5E4BB98E6ACBE USING utf8mb4), 1, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'phase-payment'
  );

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'milestone-payment', CONVERT(0xE9878CE7A88BE7A291E4BB98E6ACBE USING utf8mb4), 2, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'milestone-payment'
  );

INSERT INTO pm_custom_archive_item (archive_id, item_code, item_name, priority, status)
SELECT d.id, 'monthly-settlement', CONVERT(0xE69C88E5BAA6E7BB93E7AE97 USING utf8mb4), 3, 1
FROM pm_custom_archive_design d
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND NOT EXISTS (
      SELECT 1 FROM pm_custom_archive_item i
      WHERE i.archive_id = d.id AND i.item_code = 'monthly-settlement'
  );

-- comment standardization begin
ALTER TABLE pm_custom_archive_rule
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '自动划分规则ID',
    MODIFY COLUMN archive_item_id bigint NOT NULL COMMENT '归属结果项ID',
    MODIFY COLUMN group_no int NOT NULL DEFAULT 1 COMMENT '规则组号，同组条件为且、组间为或',
    MODIFY COLUMN field_key varchar(64) NOT NULL COMMENT '匹配字段标识',
    MODIFY COLUMN operator varchar(32) NOT NULL COMMENT '比较运算符:EQ/NE/IN/NOT_IN/GT/BETWEEN/CONTAINS',
    MODIFY COLUMN compare_value varchar(500) NULL COMMENT '比较值，按JSON序列化存储',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '自定义档案自动划分规则表';

-- comment standardization end
