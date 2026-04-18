USE finex_db;

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

SET @pm_route_attach_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'pm_process_flow_route'
      AND COLUMN_NAME = 'attach_below_nodes'
);

SET @pm_route_attach_add_sql = IF(
    @pm_route_attach_column_exists = 0,
    'ALTER TABLE pm_process_flow_route ADD COLUMN attach_below_nodes TINYINT NOT NULL DEFAULT 0 COMMENT ''是否附带下方节点:1是 0否'' AFTER default_route',
    'SELECT 1'
);
PREPARE stmt FROM @pm_route_attach_add_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE pm_process_flow_route
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT '路由ID',
    MODIFY COLUMN version_id bigint NOT NULL COMMENT '所属流程版本ID',
    MODIFY COLUMN route_key varchar(64) NOT NULL COMMENT '路由唯一标识',
    MODIFY COLUMN source_node_key varchar(64) NOT NULL COMMENT '来源节点标识',
    MODIFY COLUMN target_node_key varchar(64) NULL COMMENT '目标节点标识，为空表示流转结束',
    MODIFY COLUMN route_name varchar(64) NOT NULL COMMENT '路由名称',
    MODIFY COLUMN priority int NOT NULL DEFAULT 1 COMMENT '路由优先级，值越小越先匹配',
    MODIFY COLUMN default_route tinyint NOT NULL DEFAULT 0 COMMENT '是否默认路由:1是 0否',
    MODIFY COLUMN attach_below_nodes tinyint NOT NULL DEFAULT 0 COMMENT '是否附带下方节点:1是 0否',
    MODIFY COLUMN condition_json longtext NULL COMMENT '路由条件组JSON',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    COMMENT = '流程路由表';
