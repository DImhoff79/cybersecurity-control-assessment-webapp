-- Optional regulatory tags: tagged controls are omitted from audits when the app explicitly marks that scope out-of-scope.

CREATE TABLE control_regulatory_scope (
    control_id BIGINT NOT NULL,
    scope VARCHAR(32) NOT NULL,
    PRIMARY KEY (control_id, scope),
    CONSTRAINT fk_crs_control FOREIGN KEY (control_id) REFERENCES controls(id) ON DELETE CASCADE
);

-- Heuristic seed: PCI DSS–style controls (payment card / PCI in name or id)
INSERT INTO control_regulatory_scope (control_id, scope)
SELECT id, 'PCI' FROM controls
WHERE enabled = TRUE
  AND (
    LOWER(control_id) LIKE '%pci%'
    OR LOWER(name) LIKE '%pci%'
    OR LOWER(name) LIKE '%payment card%'
    OR LOWER(description) LIKE '%payment card%'
  );
