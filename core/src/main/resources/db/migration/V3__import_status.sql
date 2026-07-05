CREATE TABLE IF NOT EXISTS import_status (
    id UUID PRIMARY KEY,
    dataset_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    locations_count BIGINT NOT NULL DEFAULT 0,
    connections_count BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS ix_import_status_dataset_started
    ON import_status(dataset_name, started_at DESC);
