-- Insert basic roles
INSERT INTO roles (name) 
VALUES ('USER') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) 
VALUES ('ADMIN') 
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) 
VALUES ('MODERATOR') 
ON CONFLICT (name) DO NOTHING;

-- Insert admin user
-- Password is 'password123'
INSERT INTO users (
    username, 
    email, 
    password, 
    first_name, 
    last_name, 
    country, 
    status
) VALUES (
    'KarolisJal',
    'karolis@example.com',
    '$2a$10$dTj9SQL12TpCCd09y774uujBiwkDmiGj9es9t55Lmh0Kxkio6.RP6',
    'Karolis',
    'Jal',
    'LT',
    'ACTIVE'
) ON CONFLICT (username) DO NOTHING;

-- Assign admin role to the admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'KarolisJal' 
AND r.name IN ('ADMIN', 'USER')
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
); 