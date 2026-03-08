CREATE TABLE IF NOT EXISTS auditor_saved_filters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    filter_json CLOB NOT NULL,
    shared BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_auditor_saved_filters_created_by ON auditor_saved_filters(created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_auditor_saved_filters_shared ON auditor_saved_filters(shared);
