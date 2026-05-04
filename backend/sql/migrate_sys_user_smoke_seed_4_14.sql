USE finex_db;

SET NAMES utf8mb4;

SELECT id, username, name, company_id, dept_id, status
FROM sys_user
WHERE id BETWEEN 4 AND 14
ORDER BY id;

INSERT IGNORE INTO sys_user (
    id,
    username,
    password,
    name,
    phone,
    email,
    company_id,
    dept_id,
    position,
    labor_relation_belong,
    status,
    source_type,
    sync_managed,
    wecom_user_id,
    dingtalk_user_id,
    feishu_user_id,
    last_sync_at
) VALUES
    (4, 'smoke04', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73034 USING utf8mb4), '13800138004', 'smoke04@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (5, 'smoke05', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73035 USING utf8mb4), '13800138005', 'smoke05@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (6, 'smoke06', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73036 USING utf8mb4), '13800138006', 'smoke06@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (7, 'smoke07', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73037 USING utf8mb4), '13800138007', 'smoke07@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (8, 'smoke08', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73038 USING utf8mb4), '13800138008', 'smoke08@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (9, 'smoke09', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73039 USING utf8mb4), '13800138009', 'smoke09@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (10, 'smoke10', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73130 USING utf8mb4), '13800138010', 'smoke10@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (11, 'smoke11', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73131 USING utf8mb4), '13800138011', 'smoke11@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (12, 'smoke12', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73132 USING utf8mb4), '13800138012', 'smoke12@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (13, 'smoke13', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73133 USING utf8mb4), '13800138013', 'smoke13@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL),
    (14, 'smoke14', 'e10adc3949ba59abbe56e057f20f883e', CONVERT(0xE58692E7839FE794A8E688B73134 USING utf8mb4), '13800138014', 'smoke14@finex.com', 'GROUP_HQ', NULL, CONVERT(0xE58692E7839FE6B58BE8AF95E59198 USING utf8mb4), CONVERT(0xE680BBE983A8 USING utf8mb4), 1, 'MANUAL', 0, NULL, NULL, NULL, NULL);

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'FINANCE_CENTER'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username IN ('smoke04', 'smoke08', 'smoke12');

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'EAST_OPERATION'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username IN ('smoke05', 'smoke09', 'smoke13');

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'HQ_FUNCTION'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username IN ('smoke06', 'smoke10', 'smoke14');

UPDATE sys_user u
JOIN sys_department d ON d.dept_code = 'HEAD_OFFICE'
SET u.company_id = 'GROUP_HQ',
    u.dept_id = d.id
WHERE u.username IN ('smoke07', 'smoke11');

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'RL000005'
WHERE u.username IN (
    'smoke04', 'smoke05', 'smoke06', 'smoke07', 'smoke08', 'smoke09', 'smoke10', 'smoke11', 'smoke12', 'smoke13', 'smoke14'
);

SELECT id, username, name, company_id, dept_id, status
FROM sys_user
WHERE id BETWEEN 4 AND 14
ORDER BY id;
