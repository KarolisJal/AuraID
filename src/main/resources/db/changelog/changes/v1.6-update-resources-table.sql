-- Add missing columns to resources table
ALTER TABLE resources
ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'FILE',
ADD COLUMN IF NOT EXISTS path VARCHAR(500) NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS created_by BIGINT,
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- Add foreign key constraints for audit fields
ALTER TABLE resources
ADD CONSTRAINT fk_resources_created_by FOREIGN KEY (created_by) REFERENCES users(id),
ADD CONSTRAINT fk_resources_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(type);
CREATE INDEX IF NOT EXISTS idx_resources_created_by ON resources(created_by); 