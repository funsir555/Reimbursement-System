CREATE TABLE IF NOT EXISTS pm_form_design (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '表单设计ID',
    form_code VARCHAR(64) NOT NULL COMMENT '表单设计编码',
    form_name VARCHAR(100) NOT NULL COMMENT '表单设计名称',
    template_type VARCHAR(32) NOT NULL COMMENT '适用模板类型:report/application/loan',
    form_description VARCHAR(500) NULL COMMENT '表单设计说明',
    schema_json LONGTEXT NULL COMMENT '表单结构定义JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pm_form_design_code (form_code),
    KEY idx_pm_form_design_template_type (template_type)
);

-- comment standardization begin
ALTER TABLE pm_form_design
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '表单设计ID',
    MODIFY COLUMN form_code varchar(64) NOT NULL COMMENT '表单设计编码',
    MODIFY COLUMN form_name varchar(100) NOT NULL COMMENT '表单设计名称',
    MODIFY COLUMN template_type varchar(32) NOT NULL COMMENT '适用模板类型:report/application/loan',
    MODIFY COLUMN form_description varchar(500) NULL COMMENT '表单设计说明',
    MODIFY COLUMN schema_json longtext NULL COMMENT '表单结构定义JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '表单设计表';

-- comment standardization end
