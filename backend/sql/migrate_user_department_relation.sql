USE finex_db;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_department_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工部门归属关系ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_sys_user_department_rel_user_dept UNIQUE (user_id, dept_id),
    KEY idx_sys_user_department_rel_user_id (user_id),
    KEY idx_sys_user_department_rel_dept_id (dept_id),
    CONSTRAINT fk_sys_user_department_rel_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_sys_user_department_rel_dept_id FOREIGN KEY (dept_id) REFERENCES sys_department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工部门归属关系表';

INSERT IGNORE INTO sys_user_department_rel (user_id, dept_id, created_at, updated_at)
SELECT id, dept_id, NOW(), NOW()
FROM sys_user
WHERE dept_id IS NOT NULL;
