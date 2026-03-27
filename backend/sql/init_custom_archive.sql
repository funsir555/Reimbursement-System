USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS pm_custom_archive_design (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_code VARCHAR(64) NOT NULL,
    archive_name VARCHAR(100) NOT NULL,
    archive_type VARCHAR(32) NOT NULL,
    archive_description VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_archive_code (archive_code),
    KEY idx_archive_type (archive_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_custom_archive_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    priority INT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_archive_item_code (archive_id, item_code),
    KEY idx_archive_item_status (archive_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pm_custom_archive_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_item_id BIGINT NOT NULL,
    group_no INT NOT NULL DEFAULT 1,
    field_key VARCHAR(64) NOT NULL,
    operator VARCHAR(32) NOT NULL,
    compare_value VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_archive_item_group (archive_item_id, group_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
