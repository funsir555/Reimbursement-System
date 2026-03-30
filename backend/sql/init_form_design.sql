CREATE TABLE IF NOT EXISTS pm_form_design (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    form_code VARCHAR(64) NOT NULL,
    form_name VARCHAR(100) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    form_description VARCHAR(500) NULL,
    schema_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pm_form_design_code (form_code),
    KEY idx_pm_form_design_template_type (template_type)
);
