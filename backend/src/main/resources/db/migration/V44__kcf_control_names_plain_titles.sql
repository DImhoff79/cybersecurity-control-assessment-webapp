-- Column C: persist plain-language titles only (strip leading "AC-1", "SC-13(1)", etc. from names).
-- Matches frontend utils/controlDisplay.js so API consumers see correct titles everywhere.

UPDATE controls
SET name = TRIM(REGEXP_REPLACE(
  name,
  '^[A-Z]{2}-[0-9]+[a-z]?(\\s*\\([0-9]+\\))*(\\([a-z]\\))*\\s+',
  ''
))
WHERE framework = 'KROGER_CCF'
  AND name IS NOT NULL;

-- Merged rows that still have only the short id as name (e.g. "AC-1"): derive title from help text.
UPDATE controls c
SET name = (
  SELECT TRIM(REGEXP_REPLACE(
    TRIM(SUBSTRING(q.help_text FROM LOCATE('Related control:', q.help_text) + LENGTH('Related control:'))),
    '^[A-Z]{2}-[0-9]+[a-z]?(\\s*\\([0-9]+\\))*(\\([a-z]\\))*\\s+',
    ''
  ))
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
  )
  AND REGEXP_LIKE(TRIM(c.name), '^[A-Z]{2}-[0-9]+[a-z]?$');
