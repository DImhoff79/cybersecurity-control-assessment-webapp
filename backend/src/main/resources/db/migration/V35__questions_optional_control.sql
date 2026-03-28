-- Allow library-only questions (no primary control row) while mappings live in question_control_mappings.
ALTER TABLE questions ALTER COLUMN control_id DROP NOT NULL;
