-- Create approval_workflows table
CREATE TABLE IF NOT EXISTS approval_workflows (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL
);

-- Create approval_steps table
CREATE TABLE IF NOT EXISTS approval_steps (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    approval_threshold INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (workflow_id) REFERENCES approval_workflows(id)
);

-- Create approval_step_approvers table (junction table for many-to-many)
CREATE TABLE IF NOT EXISTS approval_step_approvers (
    step_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (step_id, user_id),
    FOREIGN KEY (step_id) REFERENCES approval_steps(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Create resources table if it doesn't exist
CREATE TABLE IF NOT EXISTS resources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create access_requests table if it doesn't exist
CREATE TABLE IF NOT EXISTS access_requests (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id),
    FOREIGN KEY (requester_id) REFERENCES users(id)
);

-- Create approval_step_executions table
CREATE TABLE IF NOT EXISTS approval_step_executions (
    id BIGSERIAL PRIMARY KEY,
    access_request_id BIGINT NOT NULL,
    step_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (access_request_id) REFERENCES access_requests(id),
    FOREIGN KEY (step_id) REFERENCES approval_steps(id)
);

-- Create approval_actions table
CREATE TABLE IF NOT EXISTS approval_actions (
    id BIGSERIAL PRIMARY KEY,
    step_execution_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment TEXT,
    action_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (step_execution_id) REFERENCES approval_step_executions(id),
    FOREIGN KEY (approver_id) REFERENCES users(id)
);

-- Add workflow reference to resources table if column doesn't exist
ALTER TABLE resources 
ADD COLUMN IF NOT EXISTS approval_workflow_id BIGINT;

-- Add foreign key if it doesn't exist (using DO block with proper error handling)
DO $$
BEGIN
    -- Try to add the foreign key constraint
    ALTER TABLE resources
    ADD CONSTRAINT fk_resources_workflow
    FOREIGN KEY (approval_workflow_id) REFERENCES approval_workflows(id);
EXCEPTION
    -- Catch the error if constraint already exists
    WHEN duplicate_object THEN
        NULL;
END $$;

-- Add workflow tracking columns to access_requests table if they don't exist
ALTER TABLE access_requests 
ADD COLUMN IF NOT EXISTS current_step_order INT; 