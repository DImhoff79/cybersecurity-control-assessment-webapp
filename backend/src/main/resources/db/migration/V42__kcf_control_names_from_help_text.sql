-- Restore readable control titles for Kroger CCF (merged rows had name = 'AC-1' only).
-- Prefer the first question whose help_text contains a non-empty "Related control:" value.

UPDATE controls c
SET name = (
    SELECT TRIM(SUBSTRING(q.help_text FROM LOCATE('Related control:', q.help_text) + LENGTH('Related control:')))
    FROM questions q
    WHERE q.control_id = c.id
      AND q.help_text LIKE '%Related control:%'
      AND LENGTH(TRIM(SUBSTRING(q.help_text FROM LOCATE('Related control:', q.help_text) + LENGTH('Related control:')))) > 0
    ORDER BY q.display_order, q.id
    FETCH FIRST 1 ROW ONLY
)
WHERE c.framework = 'KROGER_CCF'
  AND EXISTS (
      SELECT 1
      FROM questions q
      WHERE q.control_id = c.id
        AND q.help_text LIKE '%Related control:%'
        AND LENGTH(TRIM(SUBSTRING(q.help_text FROM LOCATE('Related control:', q.help_text) + LENGTH('Related control:')))) > 0
  );
