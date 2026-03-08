CREATE TABLE IF NOT EXISTS access_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(255),
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    decided_by_user_id BIGINT,
    decision_notes VARCHAR(2000),
    FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_access_requests_status ON access_requests(status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_access_requests_provider_subject ON access_requests(provider, provider_subject);
