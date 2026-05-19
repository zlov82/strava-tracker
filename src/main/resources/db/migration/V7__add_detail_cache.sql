ALTER TABLE activities
    ADD COLUMN map_polyline TEXT,
    ADD COLUMN activity_raw JSONB,
    ADD COLUMN laps_raw     JSONB;

ALTER TABLE activities DROP COLUMN raw_data;
