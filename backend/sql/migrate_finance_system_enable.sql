USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fin_account_set_module_enable (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    module_code VARCHAR(32) NOT NULL COMMENT '模块编码',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用：1启用 0关闭',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_account_set_module_enable_company_module (company_id, module_code),
    KEY idx_fin_account_set_module_enable_company (company_id),
    CONSTRAINT fk_fin_account_set_module_enable_company
        FOREIGN KEY (company_id) REFERENCES fin_account_set(company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账套模块启用状态表';

INSERT INTO fin_account_set_module_enable (company_id, module_code, enabled)
SELECT account_set.company_id, 'GENERAL_LEDGER', 1
FROM fin_account_set account_set
WHERE NOT EXISTS (
    SELECT 1
    FROM fin_account_set_module_enable target
    WHERE target.company_id = account_set.company_id
      AND target.module_code = 'GENERAL_LEDGER'
);

INSERT INTO fin_account_set_module_enable (company_id, module_code, enabled)
SELECT
    account_set.company_id,
    'FIXED_ASSETS',
    CASE
        WHEN EXISTS (
            SELECT 1 FROM fa_asset_category source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_card source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_change_bill source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_change_line source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_depr_run source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_depr_line source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_disposal_bill source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_disposal_line source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_opening_import source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_opening_import_line source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_period_close source WHERE source.company_id = account_set.company_id
            UNION ALL
            SELECT 1 FROM fa_asset_voucher_link source WHERE source.company_id = account_set.company_id
        ) THEN 1
        ELSE 0
    END
FROM fin_account_set account_set
WHERE NOT EXISTS (
    SELECT 1
    FROM fin_account_set_module_enable target
    WHERE target.company_id = account_set.company_id
      AND target.module_code = 'FIXED_ASSETS'
);

INSERT INTO fin_account_set_module_enable (company_id, module_code, enabled)
SELECT account_set.company_id, 'CASH_MANAGEMENT', 0
FROM fin_account_set account_set
WHERE NOT EXISTS (
    SELECT 1
    FROM fin_account_set_module_enable target
    WHERE target.company_id = account_set.company_id
      AND target.module_code = 'CASH_MANAGEMENT'
);

INSERT INTO fin_account_set_module_enable (company_id, module_code, enabled)
SELECT account_set.company_id, 'RECEIVABLE_MANAGEMENT', 0
FROM fin_account_set account_set
WHERE NOT EXISTS (
    SELECT 1
    FROM fin_account_set_module_enable target
    WHERE target.company_id = account_set.company_id
      AND target.module_code = 'RECEIVABLE_MANAGEMENT'
);

INSERT INTO fin_account_set_module_enable (company_id, module_code, enabled)
SELECT account_set.company_id, 'COST_MANAGEMENT', 0
FROM fin_account_set account_set
WHERE NOT EXISTS (
    SELECT 1
    FROM fin_account_set_module_enable target
    WHERE target.company_id = account_set.company_id
      AND target.module_code = 'COST_MANAGEMENT'
);
