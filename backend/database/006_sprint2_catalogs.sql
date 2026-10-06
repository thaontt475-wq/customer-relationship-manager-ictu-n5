USE crm_db;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    unit VARCHAR(50) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    effective_from DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_book_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    price_book_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    price DECIMAL(18,2) NOT NULL,

    UNIQUE KEY uq_pricebook_product(
        price_book_id,
        product_id
    ),

    FOREIGN KEY (price_book_id)
        REFERENCES price_books(id)
        ON DELETE CASCADE,

    FOREIGN KEY (product_id)
        REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS master_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_master_type_code(
        type,
        code
    )
);

CREATE TABLE IF NOT EXISTS custom_fields (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(200) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required_field BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_custom_entity_key(
        entity_type,
        field_key
    )
);

CREATE TABLE IF NOT EXISTS pipeline_stages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pipeline_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(150) NOT NULL,
    order_no INT NOT NULL,
    probability INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uq_pipeline_order(
        pipeline_id,
        order_no
    )
);

INSERT IGNORE INTO permissions(code, name)
VALUES
('user.import', 'Import User'),

('product.read', 'Xem sản phẩm'),
('product.create', 'Tạo sản phẩm'),
('product.update', 'Cập nhật sản phẩm'),
('product.delete', 'Xóa sản phẩm'),

('pricebook.read', 'Xem bảng giá'),
('pricebook.create', 'Tạo bảng giá'),

('masterdata.read', 'Xem danh mục dùng chung'),
('masterdata.manage', 'Quản lý danh mục dùng chung'),

('customfield.read', 'Xem custom field'),
('customfield.manage', 'Quản lý custom field'),

('pipeline.read', 'Xem pipeline stage'),
('pipeline.manage', 'Quản lý pipeline stage');

INSERT IGNORE INTO role_permissions(
    role_id,
    permission_id
)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';