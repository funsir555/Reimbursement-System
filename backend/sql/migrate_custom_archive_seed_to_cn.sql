USE finex_db;

SET NAMES utf8mb4;

UPDATE pm_custom_archive_design
SET archive_name = CONVERT(0xE6A087E7ADBEE8AEBEE7BDAE USING utf8mb4),
    archive_description = CONVERT(0xE794A8E4BA8EE6B581E7A88BE7AEA1E79086E4B8ADE6A087E7ADBEE8AEBEE7BDAEE79A84E9BB98E8AEA4E98089E68BA9E6A1A3E6A188 USING utf8mb4)
WHERE archive_code = 'PROCESS_TAG_OPTIONS';

UPDATE pm_custom_archive_design
SET archive_name = CONVERT(0xE58886E69C9FE4BB98E6ACBE USING utf8mb4),
    archive_description = CONVERT(0xE794A8E4BA8EE6B581E7A88BE7AEA1E79086E4B8ADE58886E69C9FE4BB98E6ACBEE79A84E9BB98E8AEA4E98089E68BA9E6A1A3E6A188 USING utf8mb4)
WHERE archive_code = 'PROCESS_INSTALLMENT_OPTIONS';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0xE9AB98E9A291E68AA5E99480 USING utf8mb4)
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND i.item_code = 'high-frequency';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0xE5AFB9E585ACE694AFE4BB98 USING utf8mb4)
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND i.item_code = 'public-payment';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0x4149E5AEA1E6A0B8 USING utf8mb4)
WHERE d.archive_code = 'PROCESS_TAG_OPTIONS'
  AND i.item_code = 'ai-audit';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0xE998B6E6AEB5E4BB98E6ACBE USING utf8mb4)
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND i.item_code = 'phase-payment';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0xE9878CE7A88BE7A291E4BB98E6ACBE USING utf8mb4)
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND i.item_code = 'milestone-payment';

UPDATE pm_custom_archive_item i
JOIN pm_custom_archive_design d ON d.id = i.archive_id
SET i.item_name = CONVERT(0xE69C88E5BAA6E7BB93E7AE97 USING utf8mb4)
WHERE d.archive_code = 'PROCESS_INSTALLMENT_OPTIONS'
  AND i.item_code = 'monthly-settlement';
