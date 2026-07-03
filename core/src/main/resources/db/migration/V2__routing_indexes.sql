CREATE UNIQUE INDEX IF NOT EXISTS ux_location_stop_id ON location(stop_id);
CREATE INDEX IF NOT EXISTS ix_connections_from_departure ON connections(from_location_id, departure_time);
CREATE INDEX IF NOT EXISTS ix_connections_type_departure ON connections(type, departure_time);

