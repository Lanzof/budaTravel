ALTER TABLE location ADD COLUMN IF NOT EXISTS name_normalized VARCHAR(255) NOT NULL;

CREATE INDEX IF NOT EXISTS ix_location_name_normalized
    ON location(name_normalized);
