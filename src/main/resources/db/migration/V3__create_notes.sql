CREATE TABLE notes (
    id BIGSERIAL NOT NULL PRIMARY KEY ,
    application_id BIGINT NOT NULL ,
    body VARCHAR(255) NOT NULL ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notes_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
);
CREATE INDEX idx_notes_application ON notes (application_id);