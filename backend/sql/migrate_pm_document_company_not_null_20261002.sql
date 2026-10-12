-- Final deployment gate: run only after the new writer and historical backfill are verified.
DROP PROCEDURE IF EXISTS enforce_pm_document_company_20261002;
DELIMITER $$
CREATE PROCEDURE enforce_pm_document_company_20261002()
BEGIN
  DECLARE missing_count INT DEFAULT 0;
  DECLARE nullable_count INT DEFAULT 0;
  SELECT COUNT(*) INTO missing_count FROM pm_document_instance WHERE company_id IS NULL OR company_id = '';
  IF missing_count > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Document companies remain unresolved; NOT NULL was not applied';
  END IF;
  SELECT COUNT(*) INTO nullable_count FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_document_instance'
    AND COLUMN_NAME = 'company_id' AND IS_NULLABLE = 'YES';
  IF nullable_count > 0 THEN
    ALTER TABLE pm_document_instance MODIFY COLUMN company_id VARCHAR(64) NOT NULL;
  END IF;
END$$
DELIMITER ;
CALL enforce_pm_document_company_20261002();
DROP PROCEDURE IF EXISTS enforce_pm_document_company_20261002;
