USE finex_db;

SET NAMES utf8mb4;

-- Before executing this script, confirm that existing values do not exceed:
-- code / bill_no / run_no / batch_no <= 32
-- asset_name / category_name <= 64

ALTER TABLE fa_asset_category
    MODIFY COLUMN category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    MODIFY COLUMN category_name VARCHAR(64) NOT NULL COMMENT '分类名称';

ALTER TABLE fa_asset_card
    MODIFY COLUMN asset_code VARCHAR(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name VARCHAR(64) NOT NULL COMMENT '资产名称',
    MODIFY COLUMN category_code VARCHAR(32) NOT NULL COMMENT '分类编码';

ALTER TABLE fa_asset_change_bill
    MODIFY COLUMN bill_no VARCHAR(32) NOT NULL COMMENT '单据编号';

ALTER TABLE fa_asset_change_line
    MODIFY COLUMN asset_code VARCHAR(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name VARCHAR(64) NULL COMMENT '资产名称',
    MODIFY COLUMN category_code VARCHAR(32) NULL COMMENT '分类编码';

ALTER TABLE fa_asset_depr_run
    MODIFY COLUMN run_no VARCHAR(32) NOT NULL COMMENT '运行编号';

ALTER TABLE fa_asset_depr_line
    MODIFY COLUMN asset_code VARCHAR(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name VARCHAR(64) NOT NULL COMMENT '资产名称';

ALTER TABLE fa_asset_disposal_bill
    MODIFY COLUMN bill_no VARCHAR(32) NOT NULL COMMENT '单据编号';

ALTER TABLE fa_asset_disposal_line
    MODIFY COLUMN asset_code VARCHAR(32) NOT NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name VARCHAR(64) NOT NULL COMMENT '资产名称';

ALTER TABLE fa_asset_opening_import
    MODIFY COLUMN batch_no VARCHAR(32) NOT NULL COMMENT '批次号';

ALTER TABLE fa_asset_opening_import_line
    MODIFY COLUMN asset_code VARCHAR(32) NULL COMMENT '资产编码',
    MODIFY COLUMN asset_name VARCHAR(64) NULL COMMENT '资产名称',
    MODIFY COLUMN category_code VARCHAR(32) NULL COMMENT '分类编码';
