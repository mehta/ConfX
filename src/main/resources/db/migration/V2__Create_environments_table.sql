-- Create environments table
CREATE TABLE confx_schema.environments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color_tag VARCHAR(50), -- Optional: for UI indication e.g., #FF0000 for prod
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES confx_schema.projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_environment_name UNIQUE (project_id, name) -- Environment names must be unique within a project
);

-- Trigger to update updated_at timestamp on any change
CREATE TRIGGER update_environment_modtime
BEFORE UPDATE ON confx_schema.environments
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); -- Reusing the function from V1 