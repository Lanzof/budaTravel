CREATE TABLE IF NOT EXISTS location (
    id UUID PRIMARY KEY,
    stop_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS connections (
    id UUID PRIMARY KEY,
    from_location_id UUID NOT NULL,
    to_location_id UUID NOT NULL,
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    carrier VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    CONSTRAINT fk_connections_from_location FOREIGN KEY (from_location_id) REFERENCES location(id),
    CONSTRAINT fk_connections_to_location FOREIGN KEY (to_location_id) REFERENCES location(id)
);

