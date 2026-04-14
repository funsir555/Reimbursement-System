SET NAMES utf8mb4;

USE finex_db;

ALTER TABLE fin_account_set_template_subject
    DROP FOREIGN KEY fk_fin_account_set_template_subject_template;

ALTER TABLE fin_account_subject
    DROP FOREIGN KEY fk_fin_account_subject_template;

ALTER TABLE fin_account_set
    MODIFY COLUMN template_code VARCHAR(32) NULL COMMENT '模板编码',
    MODIFY COLUMN subject_code_scheme VARCHAR(32) NULL COMMENT '科目编码规则',
    MODIFY COLUMN last_task_no VARCHAR(32) NULL COMMENT '最近任务号';

ALTER TABLE fin_account_set_code_rule
    MODIFY COLUMN rule_type VARCHAR(32) NOT NULL COMMENT '规则类型',
    MODIFY COLUMN scheme VARCHAR(32) NOT NULL COMMENT '规则表达式';

ALTER TABLE fin_account_set_template
    MODIFY COLUMN template_code VARCHAR(32) NOT NULL COMMENT '模板编码';

ALTER TABLE fin_account_set_template_subject
    MODIFY COLUMN template_code VARCHAR(32) NOT NULL COMMENT '模板编码';

ALTER TABLE fin_account_subject
    MODIFY COLUMN template_code VARCHAR(32) NULL COMMENT '来源模板编码';

ALTER TABLE fin_account_set_template_subject
    ADD CONSTRAINT fk_fin_account_set_template_subject_template
        FOREIGN KEY (template_code) REFERENCES fin_account_set_template(template_code);

ALTER TABLE fin_account_subject
    ADD CONSTRAINT fk_fin_account_subject_template
        FOREIGN KEY (template_code) REFERENCES fin_account_set_template(template_code);
