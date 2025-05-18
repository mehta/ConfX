CREATE SCHEMA IF NOT EXISTS confx_schema;

CREATE TABLE confx_schema.projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
);

-- Trigger to update updated_at timestamp on any change
CREATE OR REPLACE FUNCTION confx_schema.update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = (EXTRACT(EPOCH FROM NOW()) * 1000);
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_project_modtime
BEFORE UPDATE ON confx_schema.projects
FOR EACH ROW
EXECUTE FUNCTION confx_schema.update_modified_column(); 