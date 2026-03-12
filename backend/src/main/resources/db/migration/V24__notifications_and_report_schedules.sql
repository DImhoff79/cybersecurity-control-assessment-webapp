CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    audit_id BIGINT,
    read_at TIMESTAMP,
    digest_sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_notifications_audit FOREIGN KEY (audit_id) REFERENCES audits(id)
);

CREATE INDEX idx_notifications_user_created_at ON notifications(user_id, created_at);
CREATE INDEX idx_notifications_user_read_at ON notifications(user_id, read_at);

CREATE TABLE report_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    report_type VARCHAR(40) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    recipient_emails VARCHAR(2000) NOT NULL,
    category_filter VARCHAR(40),
    search_filter VARCHAR(200),
    project_id_filter BIGINT,
    no_project_only BOOLEAN NOT NULL DEFAULT FALSE,
    next_run_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT,
    last_run_at TIMESTAMP,
    last_run_status VARCHAR(50),
    last_run_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_schedules_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_report_schedules_project FOREIGN KEY (project_id_filter) REFERENCES audit_projects(id)
);

CREATE INDEX idx_report_schedules_next_run ON report_schedules(active, next_run_at);
