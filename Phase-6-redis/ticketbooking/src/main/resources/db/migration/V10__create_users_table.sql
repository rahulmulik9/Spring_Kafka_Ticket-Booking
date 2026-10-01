CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ORGANIZER', 'ADMIN'))
);