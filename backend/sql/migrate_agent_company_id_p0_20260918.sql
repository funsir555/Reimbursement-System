-- FinEx Agent P0 公司编码迁移
-- 已批准映射：旧 company_id = 1 -> COMPANY202604050001
-- 执行范围：本机/经明确授权的同构数据库。
-- 执行前必须备份以下三张表；脚本不会删除会话、消息或脱敏配置数据。

SET NAMES utf8mb4;
SET @finex_company_id = 'COMPANY202604050001';

DROP PROCEDURE IF EXISTS migrate_agent_company_id_p0_20260918;

DELIMITER //

CREATE PROCEDURE migrate_agent_company_id_p0_20260918()
BEGIN
    DECLARE v_count BIGINT DEFAULT 0;
    DECLARE v_column_count INT DEFAULT 0;
    DECLARE v_index_count INT DEFAULT 0;
    DECLARE v_data_type VARCHAR(64);

    SELECT COUNT(*) INTO v_count
    FROM sys_company
    WHERE BINARY company_id = BINARY @finex_company_id;
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Target company does not exist or is not unique';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_conversation
    WHERE BINARY CAST(company_id AS CHAR) NOT IN (BINARY '1', BINARY @finex_company_id);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unexpected conversation company_id detected';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_desensitization_entity_type
    WHERE BINARY CAST(company_id AS CHAR) NOT IN (BINARY '1', BINARY @finex_company_id);
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unexpected desensitization company_id detected';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_message m
    LEFT JOIN ea_agent_conversation c ON c.session_id = m.session_id
    WHERE c.session_id IS NULL;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Orphan Agent messages detected';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_message m
    JOIN ea_agent_conversation c ON c.session_id = m.session_id
    WHERE m.tenant_id <> c.tenant_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Agent message tenant mismatch detected';
    END IF;

    ALTER TABLE ea_agent_conversation
        MODIFY COLUMN company_id VARCHAR(64) NOT NULL
        COMMENT 'Business company code';

    UPDATE ea_agent_conversation
    SET company_id = @finex_company_id
    WHERE company_id = '1';

    ALTER TABLE ea_desensitization_entity_type
        MODIFY COLUMN company_id VARCHAR(64) NOT NULL
        COMMENT 'Business company code';

    UPDATE ea_desensitization_entity_type
    SET company_id = @finex_company_id
    WHERE company_id = '1';

    SELECT COUNT(*) INTO v_column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ea_agent_message'
      AND COLUMN_NAME = 'company_id';

    IF v_column_count = 0 THEN
        ALTER TABLE ea_agent_message
            ADD COLUMN company_id VARCHAR(64) NULL
            COMMENT 'Business company code'
            AFTER tenant_id;
    ELSE
        SELECT DATA_TYPE INTO v_data_type
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ea_agent_message'
          AND COLUMN_NAME = 'company_id';
        IF v_data_type <> 'varchar' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Unexpected Agent message company_id type';
        END IF;
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_message
    WHERE company_id IS NOT NULL
      AND company_id <> ''
      AND BINARY company_id <> BINARY @finex_company_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unexpected Agent message company_id detected';
    END IF;

    UPDATE ea_agent_message m
    JOIN ea_agent_conversation c
      ON c.tenant_id = m.tenant_id
     AND c.session_id = m.session_id
    SET m.company_id = c.company_id
    WHERE m.company_id IS NULL OR m.company_id = '';

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_message
    WHERE company_id IS NULL OR company_id = '';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Agent message company_id backfill incomplete';
    END IF;

    ALTER TABLE ea_agent_message
        MODIFY COLUMN company_id VARCHAR(64) NOT NULL
        COMMENT 'Business company code';

    SELECT COUNT(*) INTO v_index_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ea_agent_conversation'
      AND INDEX_NAME = 'idx_tenant_company_session';
    IF v_index_count = 0 THEN
        ALTER TABLE ea_agent_conversation
            ADD INDEX idx_tenant_company_session
                (tenant_id, company_id, session_id);
    END IF;

    SELECT COUNT(*) INTO v_index_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ea_agent_conversation'
      AND INDEX_NAME = 'idx_tenant_company_user';
    IF v_index_count = 0 THEN
        ALTER TABLE ea_agent_conversation
            ADD INDEX idx_tenant_company_user
                (tenant_id, company_id, user_id, last_message_at);
    END IF;

    SELECT COUNT(*) INTO v_index_count
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ea_agent_message'
      AND INDEX_NAME = 'idx_tenant_company_session';
    IF v_index_count = 0 THEN
        ALTER TABLE ea_agent_message
            ADD INDEX idx_tenant_company_session
                (tenant_id, company_id, session_id, created_at);
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_conversation
    WHERE BINARY company_id <> BINARY @finex_company_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Conversation company migration verification failed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_agent_message
    WHERE BINARY company_id <> BINARY @finex_company_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Message company migration verification failed';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM ea_desensitization_entity_type
    WHERE BINARY company_id <> BINARY @finex_company_id;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Desensitization company migration verification failed';
    END IF;
END//

DELIMITER ;

CALL migrate_agent_company_id_p0_20260918();
DROP PROCEDURE migrate_agent_company_id_p0_20260918;

SELECT 'ea_agent_conversation' AS table_name, COUNT(*) AS row_count,
       COUNT(DISTINCT company_id) AS company_count
FROM ea_agent_conversation
UNION ALL
SELECT 'ea_agent_message', COUNT(*), COUNT(DISTINCT company_id)
FROM ea_agent_message
UNION ALL
SELECT 'ea_desensitization_entity_type', COUNT(*), COUNT(DISTINCT company_id)
FROM ea_desensitization_entity_type;
