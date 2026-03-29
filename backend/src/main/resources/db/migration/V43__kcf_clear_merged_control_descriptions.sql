-- V41 merged sub-controls into one row per base number and concatenated each part's
-- description with " | ". That long text matches spreadsheet column E; catalog list
-- should show identifier + title only, so drop the merged blobs.
UPDATE controls
SET description = NULL
WHERE framework = 'KROGER_CCF'
  AND description IS NOT NULL
  AND description LIKE '% | %';
