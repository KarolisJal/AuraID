-- Insert roles if they don't exist
INSERT INTO roles (name) 
VALUES ('ADMIN') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) 
VALUES ('MODERATOR') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) 
VALUES ('USER') 
ON CONFLICT (name) DO NOTHING;

-- Insert admin user if it doesn't exist
-- Password is 'admin123' encoded with BCrypt
INSERT INTO users (username, email, password, first_name, last_name, country, status)
SELECT 'admin', 'admin@auraid.com', '$2a$10$mR4MU5esBbUd6JWuwWKTA.4K.qxBxiywjWVE6qFolGAHMjNfxm1R6', 'Admin', 'User', 'Global', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin'
AND r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 
    FROM user_roles ur 
    WHERE ur.user_id = u.id 
    AND ur.role_id = r.id
); 