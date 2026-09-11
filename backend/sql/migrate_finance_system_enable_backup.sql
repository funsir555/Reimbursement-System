USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fin_account_set_module_backup_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    company_id VARCHAR(64) NOT NULL COMMENT '公司主体编码',
    module_code VARCHAR(32) NOT NULL COMMENT '模块编码',
    backup_file_name VARCHAR(255) NULL COMMENT '备份文件名称',
    backup_file_path VARCHAR(500) NULL COMMENT '备份文件路径',
    backup_status VARCHAR(16) NOT NULL COMMENT '备份状态:SUCCESS成功/FAILED失败',
    backup_started_at DATETIME NOT NULL COMMENT '备份开始时间',
    backup_finished_at DATETIME NULL COMMENT '备份结束时间',
    backup_user_id BIGINT NULL COMMENT '备份人用户ID',
    backup_user_name VARCHAR(64) NULL COMMENT '备份人姓名',
    file_size_bytes BIGINT NULL COMMENT '备份文件字节数',
    remark VARCHAR(500) NULL COMMENT '备注信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_fin_account_set_module_backup_log_company_module (company_id, module_code),
    KEY idx_fin_account_set_module_backup_log_started_at (backup_started_at),
    CONSTRAINT fk_fin_account_set_module_backup_log_company
        FOREIGN KEY (company_id) REFERENCES fin_account_set(company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账套模块备份日志表';
