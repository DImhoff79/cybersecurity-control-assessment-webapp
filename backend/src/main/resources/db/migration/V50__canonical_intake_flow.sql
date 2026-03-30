-- Single-row store for published intake graph JSON (synced from branching demo workflow version 1).

CREATE TABLE canonical_intake_flow (
    id BIGINT NOT NULL PRIMARY KEY,
    graph_json CLOB NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_user_id BIGINT,
    CONSTRAINT fk_cif_updater FOREIGN KEY (updated_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO canonical_intake_flow (id, graph_json, updated_at) VALUES (1, '{}', CURRENT_TIMESTAMP);
