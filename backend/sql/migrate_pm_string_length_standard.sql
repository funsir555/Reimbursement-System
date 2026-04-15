SET NAMES utf8mb4;

USE finex_db;

ALTER TABLE pm_custom_archive_design
    MODIFY COLUMN archive_name VARCHAR(64) NOT NULL COMMENT '档案名称';

ALTER TABLE pm_custom_archive_item
    MODIFY COLUMN item_name VARCHAR(64) NOT NULL COMMENT '结果项名称';

ALTER TABLE pm_document_action_log
    MODIFY COLUMN node_name VARCHAR(64) NULL COMMENT '节点名称',
    MODIFY COLUMN actor_name VARCHAR(64) NULL COMMENT '操作人姓名';

ALTER TABLE pm_document_expense_detail
    MODIFY COLUMN detail_title VARCHAR(128) NULL COMMENT '明细标题';

ALTER TABLE pm_document_instance
    MODIFY COLUMN template_name VARCHAR(64) NOT NULL COMMENT '模板名称',
    MODIFY COLUMN flow_name VARCHAR(64) NULL COMMENT '审批流程名称',
    MODIFY COLUMN submitter_name VARCHAR(64) NOT NULL COMMENT '提单人姓名',
    MODIFY COLUMN document_title VARCHAR(128) NULL COMMENT '单据标题',
    MODIFY COLUMN current_node_name VARCHAR(64) NULL COMMENT '当前节点名称';

ALTER TABLE pm_document_relation
    MODIFY COLUMN source_field_key VARCHAR(64) NOT NULL COMMENT '源字段标识';

ALTER TABLE pm_document_task
    MODIFY COLUMN node_name VARCHAR(64) NULL COMMENT '节点名称',
    MODIFY COLUMN assignee_name VARCHAR(64) NULL COMMENT '处理人姓名';

ALTER TABLE pm_document_template
    MODIFY COLUMN template_name VARCHAR(64) NOT NULL COMMENT '模板名称',
    MODIFY COLUMN flow_name VARCHAR(64) NULL COMMENT '审批流程名称';

ALTER TABLE pm_document_write_off
    MODIFY COLUMN source_field_key VARCHAR(64) NOT NULL COMMENT '源字段标识';

ALTER TABLE pm_expense_detail_design
    MODIFY COLUMN detail_name VARCHAR(64) NOT NULL COMMENT '明细设计名称';

ALTER TABLE pm_expense_type
    MODIFY COLUMN expense_name VARCHAR(64) NOT NULL COMMENT '费用类型名称';

ALTER TABLE pm_form_design
    MODIFY COLUMN form_name VARCHAR(64) NOT NULL COMMENT '表单设计名称';

ALTER TABLE pm_process_flow
    MODIFY COLUMN flow_name VARCHAR(64) NOT NULL COMMENT '流程名称';

ALTER TABLE pm_process_flow_node
    MODIFY COLUMN node_name VARCHAR(64) NOT NULL COMMENT '节点名称';

ALTER TABLE pm_process_flow_route
    MODIFY COLUMN route_name VARCHAR(64) NOT NULL COMMENT '路由名称';

ALTER TABLE pm_process_flow_scene
    MODIFY COLUMN scene_name VARCHAR(64) NOT NULL COMMENT '场景名称';
