-- 创建数据库
CREATE DATABASE IF NOT EXISTS finex_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE finex_db;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(64) NOT NULL COMMENT '密码（MD5加密）',
    name VARCHAR(50) COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    dept_id BIGINT COMMENT '部门ID',
    position VARCHAR(50) COMMENT '职位',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    wecom_user_id VARCHAR(100) COMMENT '企微用户ID',
    dingtalk_user_id VARCHAR(100) COMMENT '钉钉用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据
INSERT INTO sys_user (username, password, name, phone, email, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '13800138000', 'admin@finex.com', 1),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13800138001', 'zhangsan@finex.com', 1),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13800138002', 'lisi@finex.com', 1);

-- 注意：密码都是 123456 的MD5值
