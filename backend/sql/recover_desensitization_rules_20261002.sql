-- Recover the existing desensitization rules for the current admin company.
-- The approved source-company records remain unchanged.
SET NAMES utf8mb4;
SET collation_connection = 'utf8mb4_unicode_ci';

SET @source_company_id = 'COMPANY202604050001';
SET @target_company_id = 'COMPANY202603260001';
SET @tenant_id = 'default';

CREATE TABLE IF NOT EXISTS ea_desensitization_entity_type_backup_20261002
LIKE ea_desensitization_entity_type;

INSERT IGNORE INTO ea_desensitization_entity_type_backup_20261002
SELECT *
FROM ea_desensitization_entity_type
WHERE tenant_id = @tenant_id
  AND company_id IN (@source_company_id, @target_company_id);

DROP PROCEDURE IF EXISTS recover_desensitization_rules_20261002;

DELIMITER //

CREATE PROCEDURE recover_desensitization_rules_20261002()
BEGIN
    DECLARE v_source_count INT DEFAULT 0;
    DECLARE v_target_company_count INT DEFAULT 0;
    DECLARE v_mismatch_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_target_company_count
    FROM sys_company
    WHERE BINARY company_id = BINARY @target_company_id
      AND status = 1;

    IF v_target_company_count <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Target company is missing or inactive';
    END IF;

    SELECT COUNT(*) INTO v_source_count
    FROM ea_desensitization_entity_type
    WHERE tenant_id = @tenant_id
      AND BINARY company_id = BINARY @source_company_id;

    IF v_source_count <> 10 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Expected exactly 10 source desensitization rules';
    END IF;

    INSERT INTO ea_desensitization_entity_type (
        tenant_id,
        company_id,
        code,
        name,
        description,
        placeholder_prefix,
        regex_patterns,
        examples,
        enabled,
        created_by,
        created_at,
        updated_at
    )
    SELECT
        source_rule.tenant_id,
        @target_company_id,
        source_rule.code,
        source_rule.name,
        source_rule.description,
        source_rule.placeholder_prefix,
        source_rule.regex_patterns,
        source_rule.examples,
        source_rule.enabled,
        source_rule.created_by,
        source_rule.created_at,
        NOW()
    FROM ea_desensitization_entity_type source_rule
    WHERE source_rule.tenant_id = @tenant_id
      AND BINARY source_rule.company_id = BINARY @source_company_id
      AND NOT EXISTS (
          SELECT 1
          FROM ea_desensitization_entity_type target_rule
          WHERE target_rule.tenant_id = source_rule.tenant_id
            AND BINARY target_rule.company_id = BINARY @target_company_id
            AND target_rule.code = source_rule.code
      );

    SELECT COUNT(*) INTO v_mismatch_count
    FROM ea_desensitization_entity_type source_rule
    LEFT JOIN ea_desensitization_entity_type target_rule
      ON target_rule.tenant_id = source_rule.tenant_id
     AND BINARY target_rule.company_id = BINARY @target_company_id
     AND target_rule.code = source_rule.code
    WHERE source_rule.tenant_id = @tenant_id
      AND BINARY source_rule.company_id = BINARY @source_company_id
      AND (
          target_rule.id IS NULL
          OR NOT (target_rule.name <=> source_rule.name)
          OR NOT (target_rule.description <=> source_rule.description)
          OR NOT (target_rule.placeholder_prefix <=> source_rule.placeholder_prefix)
          OR NOT (target_rule.regex_patterns <=> source_rule.regex_patterns)
          OR NOT (target_rule.examples <=> source_rule.examples)
          OR NOT (target_rule.enabled <=> source_rule.enabled)
      );

    IF v_mismatch_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Recovered desensitization rules do not match the source';
    END IF;
END//

DELIMITER ;

CALL recover_desensitization_rules_20261002();
DROP PROCEDURE recover_desensitization_rules_20261002;

SELECT tenant_id, company_id, COUNT(*) AS rule_count
FROM ea_desensitization_entity_type
WHERE tenant_id = @tenant_id
  AND company_id IN (@source_company_id, @target_company_id)
GROUP BY tenant_id, company_id
ORDER BY company_id;
