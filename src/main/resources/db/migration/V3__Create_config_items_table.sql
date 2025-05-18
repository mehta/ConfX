-- Create config_items table
CREATE TABLE confx_schema.config_items (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    config_key VARCHAR(255) NOT NULL, -- Renamed from 'key' to avoid SQL keyword conflict
    data_type VARCHAR(50) NOT NULL, -- e.g., BOOLEAN, STRING, INTEGER, JSON
    description TEXT,
    notes TEXT, -- For any additional notes about the config item
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    CONSTRAINT fk_project_config FOREIGN KEY (project_id) REFERENCES confx_schema.projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_config_key UNIQUE (project_id, config_key) -- Config keys must be unique within a project
);

-- Index for faster lookups by project_id and key
CREATE INDEX idx_config_items_project_key ON confx_schema.config_items (project_id, config_key);

-- Trigger to update updated_at timestamp on any change
CREATE TRIGGER update_config_item_modtime
BEFORE UPDATE ON confx_schema.config_items
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); -- Reusing the function 