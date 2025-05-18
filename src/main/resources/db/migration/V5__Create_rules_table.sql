-- Create rules table
CREATE TABLE confx_schema.rules (
    id BIGSERIAL PRIMARY KEY,
    config_version_id BIGINT NOT NULL,
    priority INT NOT NULL, -- Lower numbers evaluated first
    condition_expression TEXT NOT NULL, -- The rule expression, e.g., user.region == \"EU\"
    value_to_serve TEXT NOT NULL, -- Value to serve if condition is true. Must be valid for ConfigItem's DataType.
    description TEXT, -- Optional description for the rule
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    CONSTRAINT fk_rule_config_version FOREIGN KEY (config_version_id) REFERENCES confx_schema.config_versions(id) ON DELETE CASCADE,
    CONSTRAINT uq_config_version_priority UNIQUE (config_version_id, priority) -- Priority must be unique per config version
);

-- Index for fetching rules by config_version_id ordered by priority
CREATE INDEX idx_rules_cv_priority ON confx_schema.rules (config_version_id, priority ASC);

-- Trigger to update updated_at timestamp on any change
CREATE TRIGGER update_rule_modtime
BEFORE UPDATE ON confx_schema.rules
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); 