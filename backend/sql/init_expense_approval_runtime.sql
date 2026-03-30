USE finex_db;

SET NAMES utf8mb4;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_node_key VARCHAR(64) NULL COMMENT ''当前节点 key'' AFTER status',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_node_key'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_node_name VARCHAR(100) NULL COMMENT ''当前节点名称'' AFTER current_node_key',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_node_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN current_task_type VARCHAR(32) NULL COMMENT ''当前处理类型'' AFTER current_node_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'current_task_type'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE pm_document_instance ADD COLUMN finished_at DATETIME NULL COMMENT ''流程完成时间'' AFTER updated_at',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pm_document_instance'
      AND COLUMN_NAME = 'finished_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS pm_document_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批任务ID',
    document_code VARCHAR(64) NOT NULL COMMENT '单据编码',
    node_key VARCHAR(64) NOT NULL COMMENT '节点key',
    node_name VARCHAR(100) NULL COMMENT '节点名称',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型',
    assignee_user_id BIGINT NOT NULL COMMENT '处理人用户ID',
    assignee_name VARCHAR(100) NULL COMMENT '处理人姓名',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    task_batch_no VARCHAR(64) NOT NULL COMMENT '同节点同批次任务号',
    approval_mode VARCHAR(32) NULL COMMENT '审批模式',
    action_comment VARCHAR(500) NULL COMMENT '处理意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    handled_at DATETIME NULL COMMENT '处理时间',
    KEY idx_pm_document_task_assignee (assignee_user_id, status, created_at),
    KEY idx_pm_document_task_document (document_code, created_at),
    KEY idx_pm_document_task_node_batch (document_code, node_key, task_batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务表';

CREATE TABLE IF NOT EXISTS pm_document_action_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '轨迹日志ID',
    document_code VARCHAR(64) NOT NULL COMMENT '单据编码',
    node_key VARCHAR(64) NULL COMMENT '节点key',
    node_name VARCHAR(100) NULL COMMENT '节点名称',
    action_type VARCHAR(32) NOT NULL COMMENT '动作类型',
    actor_user_id BIGINT NULL COMMENT '操作人用户ID',
    actor_name VARCHAR(100) NULL COMMENT '操作人姓名',
    action_comment VARCHAR(500) NULL COMMENT '动作说明/意见',
    payload_json LONGTEXT NULL COMMENT '扩展载荷',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_pm_document_action_log_document (document_code, created_at),
    KEY idx_pm_document_action_log_node (document_code, node_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批轨迹日志表';
