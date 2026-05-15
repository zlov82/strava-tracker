CREATE TABLE oauth_tokens (
    id           BIGSERIAL PRIMARY KEY,
    athlete_id   BIGINT       NOT NULL UNIQUE,
    access_token  VARCHAR(512) NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    expires_at   BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
