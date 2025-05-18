-- Create config_versions table
CREATE TABLE confx_schema.config_versions (
    id BIGSERIAL PRIMARY KEY,
    config_item_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    value TEXT, -- Stores the actual config value as string; JSON for JSON type. Can be NULL if a config is conceptually "disabled" by rules rather than value.
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- True if this is the current active version for the config_item_id and environment_id
    version_number INT NOT NULL, -- Sequential per config_item_id and environment_id
    change_description TEXT, -- "Commit message" for this version/change
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000), -- Less critical as versions are somewhat immutable in value
    CONSTRAINT fk_cv_config_item FOREIGN KEY (config_item_id) REFERENCES confx_schema.config_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_cv_environment FOREIGN KEY (environment_id) REFERENCES confx_schema.environments(id) ON DELETE CASCADE,
    CONSTRAINT uq_config_item_env_version UNIQUE (config_item_id, environment_id, version_number) -- Ensures version number is unique per item & env
);

-- Index to quickly find the active version for a config item in an environment
CREATE INDEX idx_cv_item_env_active ON confx_schema.config_versions (config_item_id, environment_id, is_active) WHERE is_active = TRUE;

-- Index for retrieving version history
CREATE INDEX idx_cv_item_env_version_num ON confx_schema.config_versions (config_item_id, environment_id, version_number DESC);

-- Trigger to update updated_at timestamp on any change (though primarily for is_active changes)
CREATE TRIGGER update_config_version_modtime
BEFORE UPDATE ON confx_schema.config_versions
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); 