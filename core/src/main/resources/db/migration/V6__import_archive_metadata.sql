ALTER TABLE import_status ADD COLUMN IF NOT EXISTS source VARCHAR(64);
ALTER TABLE import_status ADD COLUMN IF NOT EXISTS archive_etag VARCHAR(255);
ALTER TABLE import_status ADD COLUMN IF NOT EXISTS archive_last_modified VARCHAR(255);
ALTER TABLE import_status ADD COLUMN IF NOT EXISTS archive_content_length BIGINT;
ALTER TABLE import_status ADD COLUMN IF NOT EXISTS archive_downloaded_at VARCHAR(255);
