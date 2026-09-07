CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    email        varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    name         varchar(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);