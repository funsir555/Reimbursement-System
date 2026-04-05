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
