USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @gl_accvouch_cash_flow_item_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'cash_flow_item_id'
);

SET @gl_accvouch_cash_flow_item_id_sql = IF(
    @gl_accvouch_cash_flow_item_id_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN cash_flow_item_id BIGINT NULL COMMENT ''现金流量档案ID'' AFTER citem_class',
    'SELECT 1'
);
PREPARE stmt FROM @gl_accvouch_cash_flow_item_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @gl_accvouch_cash_flow_item_name_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND COLUMN_NAME = 'cash_flow_item_name'
);

SET @gl_accvouch_cash_flow_item_name_sql = IF(
    @gl_accvouch_cash_flow_item_name_exists = 0,
    'ALTER TABLE gl_accvouch ADD COLUMN cash_flow_item_name VARCHAR(200) NULL COMMENT ''现金流量名称快照'' AFTER cash_flow_item_id',
    'SELECT 1'
);
PREPARE stmt FROM @gl_accvouch_cash_flow_item_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @gl_accvouch_cash_flow_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'gl_accvouch'
      AND INDEX_NAME = 'idx_gl_accvouch_company_cash_flow'
);

SET @gl_accvouch_cash_flow_index_sql = IF(
    @gl_accvouch_cash_flow_index_exists = 0,
    'ALTER TABLE gl_accvouch ADD KEY idx_gl_accvouch_company_cash_flow (company_id, cash_flow_item_id)',
    'SELECT 1'
);
PREPARE stmt FROM @gl_accvouch_cash_flow_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS fin_cash_flow_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    cash_flow_code VARCHAR(32) NOT NULL COMMENT '现金流量编码',
    cash_flow_name VARCHAR(200) NOT NULL COMMENT '现金流量名称',
    direction VARCHAR(16) NOT NULL COMMENT '现金流量方向：INFLOW/OUTFLOW',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    sort_order INT NOT NULL DEFAULT 1 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_cash_flow_item_company_code (company_id, cash_flow_code),
    KEY idx_fin_cash_flow_item_company_status (company_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='现金流量档案表';

INSERT INTO fin_cash_flow_item (company_id, cash_flow_code, cash_flow_name, direction, status, sort_order)
SELECT CONVERT(fas.company_id USING utf8mb4) COLLATE utf8mb4_unicode_ci,
       seed.cash_flow_code,
       seed.cash_flow_name,
       seed.direction,
       1,
       seed.sort_order
FROM fin_account_set fas
JOIN (
    SELECT '1001' AS cash_flow_code, '销售商品、提供劳务收到的现金' AS cash_flow_name, 'INFLOW' AS direction, 10 AS sort_order
    UNION ALL SELECT '1002', '收到其他与经营活动有关的现金', 'INFLOW', 20
    UNION ALL SELECT '2001', '购买商品、接受劳务支付的现金', 'OUTFLOW', 30
    UNION ALL SELECT '2002', '支付给职工以及为职工支付的现金', 'OUTFLOW', 40
    UNION ALL SELECT '2003', '支付的各项税费', 'OUTFLOW', 50
    UNION ALL SELECT '2004', '支付其他与经营活动有关的现金', 'OUTFLOW', 60
    UNION ALL SELECT '3001', '收回投资收到的现金', 'INFLOW', 70
    UNION ALL SELECT '3002', '购建固定资产、无形资产和其他长期资产支付的现金', 'OUTFLOW', 80
    UNION ALL SELECT '4001', '吸收投资收到的现金', 'INFLOW', 90
    UNION ALL SELECT '4002', '偿还债务支付的现金', 'OUTFLOW', 100
) seed
LEFT JOIN fin_cash_flow_item existing
    ON existing.company_id = CONVERT(fas.company_id USING utf8mb4) COLLATE utf8mb4_unicode_ci
   AND existing.cash_flow_code = seed.cash_flow_code
WHERE fas.status = 'ACTIVE'
  AND existing.id IS NULL;
