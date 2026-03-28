-- Isolated demo: versioned branching questionnaire (not wired to live audits)

CREATE TABLE demo_branching_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000)
);

CREATE TABLE demo_branching_workflow_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    version_label VARCHAR(64) NOT NULL,
    start_node_id BIGINT NULL,
    FOREIGN KEY (workflow_id) REFERENCES demo_branching_workflow(id) ON DELETE CASCADE
);

CREATE TABLE demo_branching_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    stable_key VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    body VARCHAR(4000),
    question_type VARCHAR(24) NOT NULL,
    choices_json VARCHAR(4000),
    pos_x INT NOT NULL DEFAULT 0,
    pos_y INT NOT NULL DEFAULT 0,
    UNIQUE (version_id, stable_key),
    FOREIGN KEY (version_id) REFERENCES demo_branching_workflow_version(id) ON DELETE CASCADE
);

CREATE TABLE demo_branching_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    from_node_id BIGINT NOT NULL,
    to_node_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    condition_kind VARCHAR(24) NOT NULL,
    condition_value VARCHAR(255),
    FOREIGN KEY (version_id) REFERENCES demo_branching_workflow_version(id) ON DELETE CASCADE,
    FOREIGN KEY (from_node_id) REFERENCES demo_branching_node(id) ON DELETE CASCADE,
    FOREIGN KEY (to_node_id) REFERENCES demo_branching_node(id) ON DELETE CASCADE
);

INSERT INTO demo_branching_workflow (id, name, description) VALUES (1, 'Demo branching assessment',
    'Sample workflow: card data → scope or framework choice → end. Edit in the admin demo screen.');

INSERT INTO demo_branching_workflow_version (id, workflow_id, version_label, start_node_id) VALUES (1, 1, 'v1', NULL);

INSERT INTO demo_branching_node (id, version_id, stable_key, title, body, question_type, choices_json, pos_x, pos_y) VALUES
(1, 1, 'card_data', 'Do you store or process payment card data?', 'If yes, we will ask for a short scope description.', 'YES_NO', NULL, 80, 120),
(2, 1, 'describe_scope', 'Describe the cardholder data environment briefly.', NULL, 'TEXT', NULL, 320, 40),
(3, 1, 'framework', 'Which framework is your primary focus for this review?', NULL, 'CHOICE',
 '[{"id":"pci","label":"PCI DSS"},{"id":"soc2","label":"SOC 2"},{"id":"other","label":"Other / not sure"}]', 320, 220),
(4, 1, 'end', 'Thank you — this path is complete.', 'You can reset the demo run and try another branch.', 'END', NULL, 560, 120);

UPDATE demo_branching_workflow_version SET start_node_id = 1 WHERE id = 1;

INSERT INTO demo_branching_edge (id, version_id, from_node_id, to_node_id, sort_order, condition_kind, condition_value) VALUES
(1, 1, 1, 2, 0, 'YES', NULL),
(2, 1, 1, 3, 1, 'NO', NULL),
(3, 1, 2, 4, 0, 'ALWAYS', NULL),
(4, 1, 3, 4, 0, 'OPTION', 'pci'),
(5, 1, 3, 4, 1, 'OPTION', 'soc2'),
(6, 1, 3, 4, 2, 'OPTION', 'other'),
(7, 1, 3, 4, 99, 'ALWAYS', NULL);
