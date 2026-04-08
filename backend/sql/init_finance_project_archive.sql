USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fin_project_class (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    project_class_code VARCHAR(64) NOT NULL COMMENT '项目分类编码',
    project_class_name VARCHAR(200) NOT NULL COMMENT '项目分类名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    sort_order INT NOT NULL DEFAULT 1 COMMENT '排序号',
    created_by VARCHAR(64) NULL COMMENT '创建人',
    updated_by VARCHAR(64) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_project_class_company_code (company_id, project_class_code),
    KEY idx_fin_project_class_company_status (company_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目分类表';

CREATE TABLE IF NOT EXISTS fin_project_archive (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    citemcode VARCHAR(64) NOT NULL COMMENT '项目编码',
    citemname VARCHAR(200) NOT NULL COMMENT '项目名称',
    bclose TINYINT NOT NULL DEFAULT 0 COMMENT '封存标志：1已封存 0未封存',
    citemccode VARCHAR(64) NOT NULL COMMENT '项目分类编码',
    iotherused INT NOT NULL DEFAULT 0 COMMENT '其它系统是否使用',
    dEndDate DATETIME NULL COMMENT '结束日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    sort_order INT NOT NULL DEFAULT 1 COMMENT '排序号',
    created_by VARCHAR(64) NULL COMMENT '创建人',
    updated_by VARCHAR(64) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fin_project_archive_company_code (company_id, citemcode),
    KEY idx_fin_project_archive_company_class (company_id, citemccode),
    KEY idx_fin_project_archive_company_status (company_id, status, bclose, sort_order),
    CONSTRAINT fk_fin_project_archive_class
        FOREIGN KEY (company_id, citemccode)
        REFERENCES fin_project_class (company_id, project_class_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目档案主目录表';
