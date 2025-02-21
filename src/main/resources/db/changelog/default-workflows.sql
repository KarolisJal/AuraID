-- Insert default admin-only workflow
INSERT INTO approval_workflows (name, description, type, active, created_at, created_by, updated_at, updated_by)
VALUES (
    'Admin Only Approval',
    'Simple workflow where only administrators can approve requests',
    'SINGLE_APPROVER',
    true,
    CURRENT_TIMESTAMP,
    1, -- Admin user ID
    CURRENT_TIMESTAMP,
    1
);

-- Get the workflow ID
SET @workflow_id = LAST_INSERT_ID();

-- Insert single step for admin approval
INSERT INTO approval_steps (workflow_id, step_order, name, description, active)
VALUES (
    @workflow_id,
    1,
    'Admin Approval',
    'Requires approval from any system administrator',
    true
);

-- Get the step ID
SET @step_id = LAST_INSERT_ID();

-- Insert admin users as approvers (assuming admin role ID is 1)
INSERT INTO approval_step_approvers (step_id, user_id)
SELECT @step_id, u.id
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role_id = 1; 