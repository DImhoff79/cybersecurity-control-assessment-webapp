-- Policy & procedures (*-1) controls are organization-level; security/compliance attests (ask_owner = false).
-- Operational controls keep ask_owner = true for application owners (aligned with Question Manager + flow intent).

UPDATE questions
SET ask_owner = FALSE
WHERE control_id IN (
    SELECT id FROM controls
    WHERE framework = 'KROGER_CCF'
      AND control_id IN (
          'KCF_AC-1', 'KCF_AT-1', 'KCF_AU-1', 'KCF_CA-1', 'KCF_CM-1', 'KCF_CP-1', 'KCF_IA-1', 'KCF_IR-1',
          'KCF_MA-1', 'KCF_MP-1', 'KCF_PE-1', 'KCF_PL-1', 'KCF_PS-1', 'KCF_RA-1', 'KCF_SA-1', 'KCF_SC-1',
          'KCF_SI-1', 'KCF_SR-1'
      )
);
