CREATE TABLE policy_csf_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    csf_function VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_csf_policy FOREIGN KEY (policy_id) REFERENCES policies(id),
    CONSTRAINT uk_policy_csf_unique UNIQUE (policy_id, csf_function)
);

CREATE TABLE policy_revision_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    policy_version_id BIGINT,
    event_type VARCHAR(40) NOT NULL,
    event_summary VARCHAR(1200) NOT NULL,
    actor_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_policy_revision_policy FOREIGN KEY (policy_id) REFERENCES policies(id),
    CONSTRAINT fk_policy_revision_version FOREIGN KEY (policy_version_id) REFERENCES policy_versions(id),
    CONSTRAINT fk_policy_revision_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX idx_policy_csf_policy ON policy_csf_mappings(policy_id);
CREATE INDEX idx_policy_revision_policy_created ON policy_revision_events(policy_id, created_at);
