CREATE TABLE applications (
    id BIGSERIAL NOT NULL PRIMARY KEY ,
    user_id BIGINT NOT NULL ,
    status VARCHAR(255) NOT NULL
                         check ( status IN ('APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')),
    company VARCHAR(255) NOT NULL ,
    role VARCHAR(255) NOT NULL ,
    job_url VARCHAR(255),
    salary_range VARCHAR(500),
    applied_at TIMESTAMPTZ NOT NULL  DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_applications_user_applied ON  applications (user_id, applied_at);
CREATE INDEX idx_applications_user_status ON applications (user_id, status);