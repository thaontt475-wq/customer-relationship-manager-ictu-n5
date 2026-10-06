USE crm_db;

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);

INSERT IGNORE INTO permissions(code, name)
VALUES
('organization.read', 'Xem cơ cấu tổ chức'),
('organization.manage', 'Quản lý cơ cấu tổ chức'),
('winloss.read', 'Xem lý do thắng thua'),
('winloss.manage', 'Quản lý lý do thắng thua'),
('competitor.read', 'Xem đối thủ'),
('competitor.manage', 'Quản lý đối thủ');

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

INSERT IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code IN (
        'organization.read',
        'winloss.read',
        'competitor.read'
    )
WHERE r.code IN ('MANAGER', 'SALE');