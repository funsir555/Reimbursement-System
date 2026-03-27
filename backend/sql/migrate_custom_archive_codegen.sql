ALTER TABLE pm_custom_archive_design
DROP COLUMN IF EXISTS sort_order;

ALTER TABLE pm_custom_archive_item
DROP INDEX idx_archive_item_status;

ALTER TABLE pm_custom_archive_item
DROP COLUMN IF EXISTS item_value,
DROP COLUMN IF EXISTS sort_order;

ALTER TABLE pm_custom_archive_item
ADD INDEX idx_archive_item_status (archive_id, status, id);

ALTER TABLE pm_custom_archive_rule
DROP INDEX idx_archive_item_group;

ALTER TABLE pm_custom_archive_rule
DROP COLUMN IF EXISTS sort_order;

ALTER TABLE pm_custom_archive_rule
ADD INDEX idx_archive_item_group (archive_item_id, group_no, id);
