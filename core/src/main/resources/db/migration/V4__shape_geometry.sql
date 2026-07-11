ALTER TABLE connections ADD COLUMN IF NOT EXISTS trip_id VARCHAR(255);
ALTER TABLE connections ADD COLUMN IF NOT EXISTS route_id VARCHAR(255);
ALTER TABLE connections ADD COLUMN IF NOT EXISTS shape_id VARCHAR(255);
ALTER TABLE connections ADD COLUMN IF NOT EXISTS from_stop_sequence INTEGER;
ALTER TABLE connections ADD COLUMN IF NOT EXISTS to_stop_sequence INTEGER;
ALTER TABLE connections ADD COLUMN IF NOT EXISTS from_shape_dist_traveled DOUBLE PRECISION;
ALTER TABLE connections ADD COLUMN IF NOT EXISTS to_shape_dist_traveled DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS gtfs_shape_points (
    id UUID PRIMARY KEY,
    shape_id VARCHAR(255) NOT NULL,
    shape_pt_sequence INTEGER NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    shape_dist_traveled DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS ix_connections_shape_id
    ON connections(shape_id);

CREATE INDEX IF NOT EXISTS ix_gtfs_shape_points_shape_sequence
    ON gtfs_shape_points(shape_id, shape_pt_sequence);

CREATE INDEX IF NOT EXISTS ix_gtfs_shape_points_shape_dist
    ON gtfs_shape_points(shape_id, shape_dist_traveled);
