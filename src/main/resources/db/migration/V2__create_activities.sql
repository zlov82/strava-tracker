CREATE TABLE activities (
    id              BIGSERIAL PRIMARY KEY,
    strava_id       BIGINT          NOT NULL UNIQUE,
    athlete_id      BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    type            VARCHAR(64)     NOT NULL,
    sport_type      VARCHAR(64),
    start_date      TIMESTAMPTZ     NOT NULL,
    distance        DOUBLE PRECISION NOT NULL DEFAULT 0,
    moving_time     INT             NOT NULL DEFAULT 0,
    elapsed_time    INT             NOT NULL DEFAULT 0,
    total_elevation_gain DOUBLE PRECISION NOT NULL DEFAULT 0,
    average_speed   DOUBLE PRECISION,
    max_speed       DOUBLE PRECISION,
    average_heartrate DOUBLE PRECISION,
    max_heartrate   DOUBLE PRECISION,
    trainer         BOOLEAN         NOT NULL DEFAULT FALSE,
    commute         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activities_athlete_id ON activities (athlete_id);
CREATE INDEX idx_activities_start_date ON activities (start_date);
CREATE INDEX idx_activities_type       ON activities (type);
