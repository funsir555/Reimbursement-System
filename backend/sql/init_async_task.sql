USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '异步任务ID',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    company_id VARCHAR(64) COMMENT '公司主体编码',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型',
    business_type VARCHAR(64) COMMENT '业务类型',
    business_key VARCHAR(128) COMMENT '业务唯一键',
    display_name VARCHAR(200) COMMENT '任务名称',
    status VARCHAR(20) NOT NULL COMMENT '任务状态:PENDING/RUNNING/SUCCESS/FAILED',
    progress INT DEFAULT 0 COMMENT '任务进度',
    result_message VARCHAR(255) COMMENT '任务结果信息',
    result_payload VARCHAR(1000) COMMENT '任务扩展结果',
    download_record_id BIGINT COMMENT '关联下载记录ID',
    started_at DATETIME NULL COMMENT '开始时间',
    finished_at DATETIME NULL COMMENT '结束时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_sys_async_task_company_id (company_id),
    KEY idx_user_task_created (user_id, created_at),
    KEY idx_user_task_type_status (user_id, task_type, status),
    KEY idx_task_business (task_type, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';

CREATE TABLE IF NOT EXISTS sys_notification_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    company_id VARCHAR(64) COMMENT '公司主体编码',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(255) NOT NULL COMMENT '通知内容',
    type VARCHAR(32) NOT NULL COMMENT '通知类型',
    status VARCHAR(16) DEFAULT 'UNREAD' COMMENT '通知状态:UNREAD/READ',
    related_task_no VARCHAR(64) COMMENT '关联任务编号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_at DATETIME NULL COMMENT '已读时间',
    KEY idx_sys_notification_record_company_id (company_id),
    KEY idx_user_notification_status (user_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';
