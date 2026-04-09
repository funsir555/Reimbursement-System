USE finex_db;

SET NAMES utf8mb4;

/*
全局部门树标准脚本

用途:
1. 定义新的全局部门树模型 sys_department
2. 为 sys_user 增加飞书用户ID
3. 统一 sys_user.dept_id -> sys_department.id 的关联方式

前置条件:
1. 已具备 sys_company(company_id) 主体主数据表
2. 若要执行外键约束, 需先确保现有数据满足引用完整性
3. 本脚本以“设计稿可复制执行”为目标, 不自动回填历史数据
*/

/* ========================================================================== */
/* 1. 新部门表定义                                                             */
/* ========================================================================== */

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    company_id VARCHAR(64) NULL COMMENT '公司主体编码',
    dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
    leader_user_id BIGINT NULL COMMENT '部门负责人用户ID',
    dept_name VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NULL COMMENT '上级部门ID',
    wecom_department_id VARCHAR(100) NULL COMMENT '企微部门ID',
    dingtalk_department_id VARCHAR(100) NULL COMMENT '钉钉部门ID',
    feishu_department_id VARCHAR(100) NULL COMMENT '飞书部门ID',
    sync_source VARCHAR(32) NULL COMMENT '同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED',
    sync_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用同步',
    stat_department_belong VARCHAR(100) NULL COMMENT '统计部门归属',
    stat_region_belong VARCHAR(100) NULL COMMENT '统计大区归属',
    stat_area_belong VARCHAR(100) NULL COMMENT '统计区域归属',
    last_sync_at DATETIME NULL COMMENT '最近同步时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_sys_department_dept_code UNIQUE (dept_code),
    CONSTRAINT uk_sys_department_wecom_department_id UNIQUE (wecom_department_id),
    CONSTRAINT uk_sys_department_dingtalk_department_id UNIQUE (dingtalk_department_id),
    CONSTRAINT uk_sys_department_feishu_department_id UNIQUE (feishu_department_id),
    KEY idx_sys_department_company_id (company_id),
    KEY idx_sys_department_leader_user_id (leader_user_id),
    KEY idx_sys_department_parent_id (parent_id),
    KEY idx_sys_department_status_sort (status, sort_order),
    KEY idx_sys_department_company_status_sort (company_id, status, sort_order),
    CONSTRAINT fk_sys_department_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id),
    CONSTRAINT fk_sys_department_parent_id FOREIGN KEY (parent_id) REFERENCES sys_department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局部门树主数据表';

/* ========================================================================== */
/* 2. sys_user 结构补充                                                         */
/* ========================================================================== */

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'feishu_user_id'
    ),
    'ALTER TABLE sys_user MODIFY COLUMN feishu_user_id VARCHAR(100) NULL COMMENT ''飞书用户ID''',
    'ALTER TABLE sys_user ADD COLUMN feishu_user_id VARCHAR(100) NULL COMMENT ''飞书用户ID'' AFTER dingtalk_user_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'stat_department_belong'
    ),
    'ALTER TABLE sys_user MODIFY COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''统计部门归属''',
    'ALTER TABLE sys_user ADD COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''统计部门归属'' AFTER labor_relation_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'stat_region_belong'
    ),
    'ALTER TABLE sys_user MODIFY COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''统计大区归属''',
    'ALTER TABLE sys_user ADD COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''统计大区归属'' AFTER stat_department_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'stat_area_belong'
    ),
    'ALTER TABLE sys_user MODIFY COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''统计区域归属''',
    'ALTER TABLE sys_user ADD COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''统计区域归属'' AFTER stat_region_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'stat_department_belong'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''统计部门归属''',
    'ALTER TABLE sys_department ADD COLUMN stat_department_belong VARCHAR(100) NULL COMMENT ''统计部门归属'' AFTER sync_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'stat_region_belong'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''统计大区归属''',
    'ALTER TABLE sys_department ADD COLUMN stat_region_belong VARCHAR(100) NULL COMMENT ''统计大区归属'' AFTER stat_department_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'stat_area_belong'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''统计区域归属''',
    'ALTER TABLE sys_department ADD COLUMN stat_area_belong VARCHAR(100) NULL COMMENT ''统计区域归属'' AFTER stat_region_belong'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'leader_user_id'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN leader_user_id BIGINT NULL COMMENT ''部门负责人用户ID''',
    'ALTER TABLE sys_department ADD COLUMN leader_user_id BIGINT NULL COMMENT ''部门负责人用户ID'' AFTER dept_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND INDEX_NAME = 'idx_feishu_user_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_feishu_user_id ON sys_user (feishu_user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND INDEX_NAME = 'idx_sys_department_leader_user_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_department_leader_user_id ON sys_department (leader_user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 3. sys_department 结构补充/统一                                              */
/* ========================================================================== */

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'company_id'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码''',
    'ALTER TABLE sys_department ADD COLUMN company_id VARCHAR(64) NULL COMMENT ''公司主体编码'' AFTER id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'wecom_department_id'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN wecom_department_id VARCHAR(100) NULL COMMENT ''企微部门ID''',
    'ALTER TABLE sys_department ADD COLUMN wecom_department_id VARCHAR(100) NULL COMMENT ''企微部门ID'' AFTER parent_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'dingtalk_department_id'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN dingtalk_department_id VARCHAR(100) NULL COMMENT ''钉钉部门ID''',
    'ALTER TABLE sys_department ADD COLUMN dingtalk_department_id VARCHAR(100) NULL COMMENT ''钉钉部门ID'' AFTER wecom_department_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'feishu_department_id'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN feishu_department_id VARCHAR(100) NULL COMMENT ''飞书部门ID''',
    'ALTER TABLE sys_department ADD COLUMN feishu_department_id VARCHAR(100) NULL COMMENT ''飞书部门ID'' AFTER dingtalk_department_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'sync_source'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN sync_source VARCHAR(32) NULL COMMENT ''同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED''',
    'ALTER TABLE sys_department ADD COLUMN sync_source VARCHAR(32) NULL COMMENT ''同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED'' AFTER feishu_department_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'sync_enabled'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN sync_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用同步''',
    'ALTER TABLE sys_department ADD COLUMN sync_enabled TINYINT NOT NULL DEFAULT 1 COMMENT ''是否启用同步'' AFTER sync_source'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND COLUMN_NAME = 'last_sync_at'
    ),
    'ALTER TABLE sys_department MODIFY COLUMN last_sync_at DATETIME NULL COMMENT ''最近同步时间''',
    'ALTER TABLE sys_department ADD COLUMN last_sync_at DATETIME NULL COMMENT ''最近同步时间'' AFTER sync_enabled'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND INDEX_NAME = 'idx_sys_department_company_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_department_company_id ON sys_department (company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND INDEX_NAME = 'idx_sys_department_parent_id'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_department_parent_id ON sys_department (parent_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND INDEX_NAME = 'idx_sys_department_company_status_sort'
    ),
    'SELECT 1',
    'CREATE INDEX idx_sys_department_company_status_sort ON sys_department (company_id, status, sort_order)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 4. 外键约束                                                                  */
/* ========================================================================== */

/*
执行外键前请先确认:
1. sys_department.company_id 的非空值均已存在于 sys_company
2. sys_department.parent_id 的非空值均已存在于 sys_department.id
3. sys_user.dept_id 的非空值均已存在于 sys_department.id
*/

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND CONSTRAINT_NAME = 'fk_sys_department_company_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_department ADD CONSTRAINT fk_sys_department_company_id FOREIGN KEY (company_id) REFERENCES sys_company(company_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND CONSTRAINT_NAME = 'fk_sys_department_leader_user_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_department ADD CONSTRAINT fk_sys_department_leader_user_id FOREIGN KEY (leader_user_id) REFERENCES sys_user(id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_department'
          AND CONSTRAINT_NAME = 'fk_sys_department_parent_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_department ADD CONSTRAINT fk_sys_department_parent_id FOREIGN KEY (parent_id) REFERENCES sys_department(id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND CONSTRAINT_NAME = 'fk_sys_user_dept_id'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT 1',
    'ALTER TABLE sys_user ADD CONSTRAINT fk_sys_user_dept_id FOREIGN KEY (dept_id) REFERENCES sys_department(id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* ========================================================================== */
/* 5. 后续新表设计约束说明                                                      */
/* ========================================================================== */

/*
部门表固定策略:
1. 继续使用 sys_department 作为唯一部门表
2. dept_code 为内部全局唯一编码, 不直接拿三方平台ID替代
3. 企微/钉钉/飞书部门ID分别独立存储
4. 部门树使用 parent_id 指向 sys_department.id
5. company_id 当前允许为空, 便于渐进接入, 后续可再收紧

用户表固定策略:
1. 保留 sys_user.dept_id BIGINT
2. sys_user.dept_id 始终关联 sys_department.id
3. 保留 wecom_user_id / dingtalk_user_id
4. 新增 feishu_user_id
*/

-- comment standardization begin
ALTER TABLE sys_department
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    MODIFY COLUMN company_id varchar(64) NULL COMMENT '公司主体编码',
    MODIFY COLUMN dept_code varchar(64) NOT NULL COMMENT '部门编码',
    MODIFY COLUMN leader_user_id bigint NULL COMMENT '部门负责人用户ID',
    MODIFY COLUMN dept_name varchar(128) NOT NULL COMMENT '部门名称',
    MODIFY COLUMN parent_id bigint NULL COMMENT '上级部门ID',
    MODIFY COLUMN wecom_department_id varchar(100) NULL COMMENT '企微部门ID',
    MODIFY COLUMN dingtalk_department_id varchar(100) NULL COMMENT '钉钉部门ID',
    MODIFY COLUMN feishu_department_id varchar(100) NULL COMMENT '飞书部门ID',
    MODIFY COLUMN sync_source varchar(32) NULL COMMENT '同步来源:MANUAL/WECOM/DINGTALK/FEISHU/MIXED',
    MODIFY COLUMN sync_enabled tinyint NOT NULL DEFAULT 1 COMMENT '是否启用同步',
    MODIFY COLUMN sync_managed tinyint NOT NULL DEFAULT 0 COMMENT '是否纳入同步管理:1是 0否',
    MODIFY COLUMN sync_status varchar(32) NULL COMMENT '同步状态',
    MODIFY COLUMN sync_remark varchar(500) NULL COMMENT '同步备注',
    MODIFY COLUMN stat_department_belong varchar(100) NULL COMMENT '统计部门归属',
    MODIFY COLUMN stat_region_belong varchar(100) NULL COMMENT '统计大区归属',
    MODIFY COLUMN stat_area_belong varchar(100) NULL COMMENT '统计区域归属',
    MODIFY COLUMN last_sync_at datetime NULL COMMENT '最近同步时间',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
    MODIFY COLUMN sort_order int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '全局部门树主数据表';

ALTER TABLE sys_user
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    MODIFY COLUMN username varchar(50) NOT NULL COMMENT '用户名',
    MODIFY COLUMN password varchar(64) NOT NULL COMMENT '密码(MD5)',
    MODIFY COLUMN name varchar(50) NULL COMMENT '姓名',
    MODIFY COLUMN phone varchar(20) NULL COMMENT '手机号',
    MODIFY COLUMN email varchar(100) NULL COMMENT '邮箱',
    MODIFY COLUMN dept_id bigint NULL COMMENT '部门ID',
    MODIFY COLUMN position varchar(50) NULL COMMENT '岗位',
    MODIFY COLUMN labor_relation_belong varchar(100) NULL COMMENT '劳动关系归属',
    MODIFY COLUMN stat_department_belong varchar(100) NULL COMMENT '统计部门归属',
    MODIFY COLUMN stat_region_belong varchar(100) NULL COMMENT '统计大区归属',
    MODIFY COLUMN stat_area_belong varchar(100) NULL COMMENT '统计区域归属',
    MODIFY COLUMN company_id varchar(64) NULL COMMENT '公司主体编码',
    MODIFY COLUMN status tinyint NULL DEFAULT 1 COMMENT '状态:1正常 0停用',
    MODIFY COLUMN source_type varchar(32) NULL COMMENT '来源类型',
    MODIFY COLUMN sync_managed tinyint NOT NULL DEFAULT 0 COMMENT '是否纳入同步管理:1是 0否',
    MODIFY COLUMN wecom_user_id varchar(100) NULL COMMENT '企微用户ID',
    MODIFY COLUMN dingtalk_user_id varchar(100) NULL COMMENT '钉钉用户ID',
    MODIFY COLUMN feishu_user_id varchar(100) NULL COMMENT '飞书用户ID',
    MODIFY COLUMN last_sync_at datetime NULL COMMENT '最近同步时间',
    MODIFY COLUMN created_at datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '用户表';

-- comment standardization end
