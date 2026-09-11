USE finex_db;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS gl_period_transfer_voucher_link;
DROP TABLE IF EXISTS gl_period_transfer_run_detail;
DROP TABLE IF EXISTS gl_period_transfer_run;
DROP TABLE IF EXISTS gl_period_transfer_rule_line;
DROP TABLE IF EXISTS gl_period_transfer_rule;

CREATE TABLE gl_period_transfer_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    rule_type VARCHAR(32) NOT NULL COMMENT '规则类型:PROFIT/CUSTOM/MANUFACTURE',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    transfer_granularity VARCHAR(32) NULL COMMENT '结转粒度:SUBJECT/ASSIST',
    subject_level INT NULL COMMENT '科目级次',
    source_subject_code VARCHAR(64) NULL COMMENT '默认转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '默认转出科目名称',
    target_subject_code VARCHAR(64) NULL COMMENT '默认转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '默认转入科目名称',
    allocation_mode VARCHAR(32) NULL COMMENT '分配方式:FULL/MANUAL_RATIO/PROJECT',
    regenerate_strategy VARCHAR(32) NOT NULL DEFAULT 'REPLACE_UNPOSTED' COMMENT '重生成策略',
    created_by VARCHAR(64) NULL COMMENT '创建人',
    updated_by VARCHAR(64) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gl_period_transfer_rule_company_type (company_id, rule_type),
    KEY idx_gl_period_transfer_rule_enabled (company_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转规则表';

CREATE TABLE gl_period_transfer_rule_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    line_no INT NOT NULL COMMENT '行号',
    source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    metric_type VARCHAR(32) NULL COMMENT '取数指标:ENDING_BALANCE/CURRENT_DEBIT/CURRENT_CREDIT/CURRENT_NET',
    ratio DECIMAL(18,6) NULL COMMENT '比例系数',
    allocation_project_class VARCHAR(2) NULL COMMENT '分配项目分类',
    allocation_project_id VARCHAR(6) NULL COMMENT '分配项目编码',
    allocation_project_name VARCHAR(128) NULL COMMENT '分配项目名称',
    remark VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_rule_line_rule (rule_id, line_no),
    CONSTRAINT fk_gl_period_transfer_rule_line_rule FOREIGN KEY (rule_id) REFERENCES gl_period_transfer_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转规则明细表';

CREATE TABLE gl_period_transfer_run (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_type VARCHAR(32) NOT NULL COMMENT '规则类型',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称快照',
    iyear INT NOT NULL COMMENT '会计年度',
    iperiod TINYINT NOT NULL COMMENT '会计期间',
    iyperiod INT NOT NULL COMMENT '会计年月(YYYYMM)',
    status VARCHAR(32) NOT NULL COMMENT '运行状态:PREVIEWED/GENERATED/CANCELLED',
    preview_token VARCHAR(64) NULL COMMENT '预览令牌',
    include_unposted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含未记账凭证',
    voucher_type VARCHAR(32) NOT NULL COMMENT '凭证类别字',
    voucher_no VARCHAR(128) NULL COMMENT '生成凭证号',
    generated_entry_count INT NOT NULL DEFAULT 0 COMMENT '生成分录数',
    skipped_entry_count INT NOT NULL DEFAULT 0 COMMENT '跳过分录数',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '生成总金额',
    blocked_message VARCHAR(500) NULL COMMENT '阻断或说明信息',
    rule_updated_at DATETIME NULL COMMENT '规则更新时间快照',
    previewed_by VARCHAR(64) NULL COMMENT '预览人',
    previewed_at DATETIME NULL COMMENT '预览时间',
    generated_by VARCHAR(64) NULL COMMENT '生成人',
    generated_at DATETIME NULL COMMENT '生成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_run_period (company_id, iyear, iperiod, rule_type),
    KEY idx_gl_period_transfer_run_rule (rule_id, status),
    CONSTRAINT fk_gl_period_transfer_run_rule FOREIGN KEY (rule_id) REFERENCES gl_period_transfer_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转运行表';

CREATE TABLE gl_period_transfer_run_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_line_id BIGINT NULL COMMENT '规则明细ID',
    line_no INT NOT NULL COMMENT '行号',
    source_subject_code VARCHAR(64) NULL COMMENT '转出科目编码',
    source_subject_name VARCHAR(128) NULL COMMENT '转出科目名称',
    source_assist_key VARCHAR(255) NULL COMMENT '转出辅助维度键',
    source_assist_label VARCHAR(255) NULL COMMENT '转出辅助维度标签',
    target_subject_code VARCHAR(64) NULL COMMENT '转入科目编码',
    target_subject_name VARCHAR(128) NULL COMMENT '转入科目名称',
    target_cdept_id VARCHAR(64) NULL COMMENT '目标部门编码',
    target_cperson_id VARCHAR(64) NULL COMMENT '目标人员编码',
    target_ccus_id VARCHAR(64) NULL COMMENT '目标客户编码',
    target_csup_id VARCHAR(64) NULL COMMENT '目标供应商编码',
    target_citem_class VARCHAR(2) NULL COMMENT '目标项目分类',
    target_citem_id VARCHAR(6) NULL COMMENT '目标项目编码',
    metric_type VARCHAR(32) NULL COMMENT '取数指标',
    direction VARCHAR(16) NULL COMMENT '方向:DEBIT/CREDIT',
    amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    skip_reason VARCHAR(255) NULL COMMENT '跳过原因',
    source_trace_key VARCHAR(255) NULL COMMENT '来源追溯键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_run_detail_run (run_id, line_no),
    CONSTRAINT fk_gl_period_transfer_run_detail_run FOREIGN KEY (run_id) REFERENCES gl_period_transfer_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转运行明细表';

CREATE TABLE gl_period_transfer_voucher_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    run_id BIGINT NOT NULL COMMENT '运行ID',
    run_detail_id BIGINT NOT NULL COMMENT '运行明细ID',
    voucher_no VARCHAR(128) NOT NULL COMMENT '凭证号',
    entry_no INT NOT NULL COMMENT '分录行号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gl_period_transfer_voucher_link_run (run_id),
    KEY idx_gl_period_transfer_voucher_link_voucher (voucher_no),
    CONSTRAINT fk_gl_period_transfer_voucher_link_run FOREIGN KEY (run_id) REFERENCES gl_period_transfer_run(id),
    CONSTRAINT fk_gl_period_transfer_voucher_link_detail FOREIGN KEY (run_detail_id) REFERENCES gl_period_transfer_run_detail(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='总账期末结转凭证关联表';
