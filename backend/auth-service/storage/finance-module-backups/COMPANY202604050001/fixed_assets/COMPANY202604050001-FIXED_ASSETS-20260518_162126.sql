USE finex_db;

SET NAMES utf8mb4;

-- 公司主体: COMPANY202604050001
-- 模块编码: FIXED_ASSETS

-- 恢复前清理当前公司该模块旧数据
DELETE FROM fa_asset_category WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_card WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_period_close WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_opening_import WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_opening_import_line WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_change_bill WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_change_line WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_disposal_bill WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_disposal_line WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_depr_run WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_depr_line WHERE company_id = 'COMPANY202604050001';
DELETE FROM fa_asset_voucher_link WHERE company_id = 'COMPANY202604050001';

-- 表: fa_asset_category
INSERT INTO fa_asset_category (id, company_id, category_code, category_name, share_scope, depreciation_method, useful_life_months, residual_rate, depreciable, status, remark, created_by, updated_by, created_at, updated_at) VALUES (16, 'COMPANY202604050001', 'HOUSE_BUILD', '房屋及建筑物', 'COMPANY', 'STRAIGHT_LINE', 240, 0.0500, 1, 'ACTIVE', NULL, 'system', 'system', '2026-04-26 20:11:26', '2026-04-26 20:11:26');
INSERT INTO fa_asset_category (id, company_id, category_code, category_name, share_scope, depreciation_method, useful_life_months, residual_rate, depreciable, status, remark, created_by, updated_by, created_at, updated_at) VALUES (18, 'COMPANY202604050001', 'MACHINE_EQUIP', '机器设备', 'COMPANY', 'STRAIGHT_LINE', 120, 0.0500, 1, 'ACTIVE', NULL, 'system', 'system', '2026-04-26 20:11:26', '2026-04-26 20:11:26');
INSERT INTO fa_asset_category (id, company_id, category_code, category_name, share_scope, depreciation_method, useful_life_months, residual_rate, depreciable, status, remark, created_by, updated_by, created_at, updated_at) VALUES (20, 'COMPANY202604050001', 'TRANS_EQUIP', '运输设备', 'COMPANY', 'STRAIGHT_LINE', 72, 0.0500, 1, 'ACTIVE', NULL, 'system', 'system', '2026-04-26 20:11:26', '2026-04-26 20:11:26');
INSERT INTO fa_asset_category (id, company_id, category_code, category_name, share_scope, depreciation_method, useful_life_months, residual_rate, depreciable, status, remark, created_by, updated_by, created_at, updated_at) VALUES (22, 'COMPANY202604050001', 'ELECTRONIC_EQUIP', '电子设备', 'COMPANY', 'DOUBLE_DECLINING', 36, 0.0300, 1, 'ACTIVE', NULL, 'system', 'system', '2026-04-26 20:11:26', '2026-04-26 20:11:26');
INSERT INTO fa_asset_category (id, company_id, category_code, category_name, share_scope, depreciation_method, useful_life_months, residual_rate, depreciable, status, remark, created_by, updated_by, created_at, updated_at) VALUES (24, 'COMPANY202604050001', 'OFFICE_EQUIP', '办公设备', 'COMPANY', 'STRAIGHT_LINE', 60, 0.0500, 1, 'ACTIVE', NULL, 'system', 'system', '2026-04-26 20:11:26', '2026-04-26 20:11:26');

-- 表: fa_asset_card
-- 无数据

-- 表: fa_asset_change_bill
-- 无数据

-- 表: fa_asset_change_line
-- 无数据

-- 表: fa_asset_depr_run
-- 无数据

-- 表: fa_asset_depr_line
-- 无数据

-- 表: fa_asset_disposal_bill
-- 无数据

-- 表: fa_asset_disposal_line
-- 无数据

-- 表: fa_asset_opening_import
-- 无数据

-- 表: fa_asset_opening_import_line
-- 无数据

-- 表: fa_asset_period_close
-- 无数据

-- 表: fa_asset_voucher_link
-- 无数据

