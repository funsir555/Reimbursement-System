-- Run after backing up pm_document_instance and deploying the company-snapshot writer.
-- Pause document writes while this migration runs. This script never changes sys_user.
-- Priority: saved payment company > reviewed company > existing document snapshot
--           > submitter's sys_user.company_id at migration time.
-- The last source is used only for documents that have no payment company and no
-- existing company snapshot. Later changes to sys_user must not rewrite a snapshot.

CREATE TABLE IF NOT EXISTS pm_document_company_backfill_review (
  document_code VARCHAR(64) PRIMARY KEY,
  company_id VARCHAR(64) NOT NULL,
  evidence_ref VARCHAR(255) NOT NULL,
  approved_by VARCHAR(64) NOT NULL,
  approved_at DATETIME NOT NULL,
  KEY idx_pm_document_company_backfill_review_company (company_id)
);

-- The requester confirmed both historical company IDs on 2026-10-02.
-- Preserve an existing review decision; the procedure rejects a conflicting one.
INSERT INTO pm_document_company_backfill_review
  (document_code, company_id, evidence_ref, approved_by, approved_at)
SELECT d.document_code, 'COMPANY202603260001',
       'Requester confirmation on 2026-10-02', 'requester', NOW()
FROM pm_document_instance d
WHERE d.document_code IN ('DOC202604010001', 'DOC202604010002')
  AND NOT EXISTS (
    SELECT 1 FROM pm_document_company_backfill_review r
    WHERE BINARY r.document_code = BINARY d.document_code
  );

DROP PROCEDURE IF EXISTS backfill_pm_document_company_20261002;
DELIMITER $$
CREATE PROCEDURE backfill_pm_document_company_20261002()
BEGIN
  DECLARE problem_count INT DEFAULT 0;

  SELECT COUNT(*) INTO problem_count FROM pm_document_instance
  WHERE JSON_VALID(form_schema_snapshot_json) = 0 OR JSON_VALID(form_data_json) = 0;
  IF problem_count > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Document form JSON is invalid; no company backfill was applied';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_pm_document_company_candidate;
  CREATE TEMPORARY TABLE tmp_pm_document_company_candidate AS
  SELECT d.id, d.document_code,
         NULLIF(TRIM(d.company_id), '') AS current_company_id,
         NULLIF(TRIM(su.company_id), '') AS submitter_company_id,
         SUM(CASE WHEN jt.component_code = 'payment-company' THEN 1 ELSE 0 END) AS payment_component_count,
         COUNT(CASE WHEN jt.component_code = 'payment-company'
                         AND (jt.field_key IS NULL OR TRIM(jt.field_key) = '') THEN 1 END) AS missing_field_keys,
         SUM(CASE WHEN jt.component_code = 'payment-company' AND jt.raw_value IS NOT NULL
                       AND jt.raw_value <> 'null' AND jt.normalized_value IS NULL THEN 1 ELSE 0 END) AS invalid_values,
         SUM(CASE WHEN jt.component_code = 'payment-company' AND jt.normalized_value IS NOT NULL THEN 1 ELSE 0 END) AS payer_count,
         MAX(CASE WHEN jt.component_code = 'payment-company' THEN jt.normalized_value END) AS payment_company_id,
         NULLIF(TRIM(MAX(r.company_id)), '') AS reviewed_company_id,
         NULLIF(TRIM(MAX(r.evidence_ref)), '') AS review_evidence_ref
  FROM pm_document_instance d
  LEFT JOIN sys_user su ON su.id = d.submitter_user_id
  LEFT JOIN JSON_TABLE(d.form_schema_snapshot_json, '$.blocks[*]'
    COLUMNS (
      field_key VARCHAR(128) PATH '$.fieldKey',
      component_code VARCHAR(64) PATH '$.props.componentCode'
    )) fields ON fields.component_code = 'payment-company'
  LEFT JOIN LATERAL (
    SELECT fields.component_code, fields.field_key,
           JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key))) AS raw_value,
           NULLIF(TRIM(CASE JSON_TYPE(JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key))))
             WHEN 'STRING' THEN JSON_UNQUOTE(JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key))))
             WHEN 'OBJECT' THEN COALESCE(
               JSON_UNQUOTE(JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key), '.value'))),
               JSON_UNQUOTE(JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key), '.code'))),
               JSON_UNQUOTE(JSON_EXTRACT(d.form_data_json, CONCAT('$.', JSON_QUOTE(fields.field_key), '.id'))))
             ELSE NULL END), '') AS normalized_value
  ) jt ON TRUE
  LEFT JOIN pm_document_company_backfill_review r
    ON BINARY r.document_code = BINARY d.document_code
  GROUP BY d.id, d.document_code, d.company_id, su.company_id;

  SELECT COUNT(*) INTO problem_count FROM tmp_pm_document_company_candidate
  WHERE missing_field_keys > 0 OR invalid_values > 0 OR payment_component_count > 1;
  IF problem_count > 0 THEN
    SELECT document_code, payment_component_count, missing_field_keys, invalid_values, payer_count
    FROM tmp_pm_document_company_candidate
    WHERE missing_field_keys > 0 OR invalid_values > 0 OR payment_component_count > 1;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ambiguous or invalid payment-company form value; no backfill was applied';
  END IF;

  SELECT COUNT(*) INTO problem_count FROM tmp_pm_document_company_candidate
  WHERE document_code IN ('DOC202604010001', 'DOC202604010002')
    AND (reviewed_company_id IS NULL OR reviewed_company_id <> 'COMPANY202603260001');
  IF problem_count > 0 THEN
    SELECT document_code, reviewed_company_id FROM tmp_pm_document_company_candidate
    WHERE document_code IN ('DOC202604010001', 'DOC202604010002')
      AND (reviewed_company_id IS NULL OR reviewed_company_id <> 'COMPANY202603260001');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Confirmed document company conflicts with an existing review; no backfill was applied';
  END IF;

  SELECT COUNT(*) INTO problem_count FROM tmp_pm_document_company_candidate c
  LEFT JOIN sys_company sc ON sc.company_id = COALESCE(
    c.payment_company_id, c.reviewed_company_id, c.current_company_id, c.submitter_company_id)
  WHERE sc.company_id IS NULL
     OR (c.reviewed_company_id IS NOT NULL AND c.review_evidence_ref IS NULL)
     OR (c.payment_company_id IS NOT NULL AND c.reviewed_company_id IS NOT NULL
         AND BINARY c.payment_company_id <> BINARY c.reviewed_company_id)
     OR (c.current_company_id IS NOT NULL AND c.payment_company_id IS NOT NULL
         AND BINARY c.current_company_id <> BINARY c.payment_company_id)
     OR (c.current_company_id IS NOT NULL AND c.reviewed_company_id IS NOT NULL
         AND BINARY c.current_company_id <> BINARY c.reviewed_company_id);
  IF problem_count > 0 THEN
    SELECT c.document_code, c.payment_company_id, c.reviewed_company_id,
           c.current_company_id, c.submitter_company_id
    FROM tmp_pm_document_company_candidate c
    LEFT JOIN sys_company sc ON sc.company_id = COALESCE(
      c.payment_company_id, c.reviewed_company_id, c.current_company_id, c.submitter_company_id)
    WHERE sc.company_id IS NULL
       OR (c.reviewed_company_id IS NOT NULL AND c.review_evidence_ref IS NULL)
       OR (c.payment_company_id IS NOT NULL AND c.reviewed_company_id IS NOT NULL
           AND BINARY c.payment_company_id <> BINARY c.reviewed_company_id)
       OR (c.current_company_id IS NOT NULL AND c.payment_company_id IS NOT NULL
           AND BINARY c.current_company_id <> BINARY c.payment_company_id)
       OR (c.current_company_id IS NOT NULL AND c.reviewed_company_id IS NOT NULL
           AND BINARY c.current_company_id <> BINARY c.reviewed_company_id);
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unknown or conflicting document company; no backfill was applied';
  END IF;

  START TRANSACTION;
  UPDATE pm_document_instance d
  JOIN tmp_pm_document_company_candidate c ON c.id = d.id
  SET d.company_id = COALESCE(
    c.payment_company_id, c.reviewed_company_id, c.current_company_id, c.submitter_company_id)
  WHERE d.company_id IS NULL OR TRIM(d.company_id) = '';
  COMMIT;

  SELECT COUNT(*) AS remaining_without_company FROM pm_document_instance
  WHERE company_id IS NULL OR TRIM(company_id) = '';
  SELECT document_code, company_id FROM pm_document_instance
  WHERE document_code IN ('DOC202604010001', 'DOC202604010002')
  ORDER BY document_code;
END$$
DELIMITER ;

CALL backfill_pm_document_company_20261002();
DROP PROCEDURE IF EXISTS backfill_pm_document_company_20261002;
