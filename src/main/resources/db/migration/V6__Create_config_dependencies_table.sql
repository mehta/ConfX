-- Create config_dependencies table
CREATE TABLE confx_schema.config_dependencies (
    id BIGSERIAL PRIMARY KEY,
    dependent_config_item_id BIGINT NOT NULL,
    prerequisite_config_item_id BIGINT NOT NULL,
    -- The expected value of the prerequisite. Stored as TEXT, validated against prerequisite's data type during evaluation.
    prerequisite_expected_value TEXT NOT NULL,
    description TEXT, -- Optional: describe why this dependency exists
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    CONSTRAINT fk_cd_dependent_item FOREIGN KEY (dependent_config_item_id) REFERENCES confx_schema.config_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_cd_prerequisite_item FOREIGN KEY (prerequisite_config_item_id) REFERENCES confx_schema.config_items(id) ON DELETE CASCADE,
    -- A config item cannot depend on itself directly, enforced by different IDs usually.
    -- A specific config item can only depend on another specific config item once.
    CONSTRAINT uq_dependent_prerequisite UNIQUE (dependent_config_item_id, prerequisite_config_item_id),
    -- Ensure dependent and prerequisite are not the same config item.
    CONSTRAINT chk_not_self_dependent CHECK (dependent_config_item_id <> prerequisite_config_item_id)
);

-- Index for quickly finding prerequisites for a dependent config
CREATE INDEX idx_cd_dependent ON confx_schema.config_dependencies (dependent_config_item_id);
-- Index for quickly finding configs that depend on a prerequisite config
CREATE INDEX idx_cd_prerequisite ON confx_schema.config_dependencies (prerequisite_config_item_id);

-- Trigger to update updated_at timestamp
CREATE TRIGGER update_config_dependency_modtime
BEFORE UPDATE ON confx_schema.config_dependencies
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); 