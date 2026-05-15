CREATE TABLE sync_state (
    id           BIGSERIAL PRIMARY KEY,
    athlete_id   BIGINT      NOT NULL UNIQUE,
    last_sync_at BIGINT      NOT NULL DEFAULT 0,
    status       VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    error        TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
