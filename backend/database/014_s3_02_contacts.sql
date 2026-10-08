-- S3-02. Apply once after migrations 012 and 013; no existing rows are rewritten.
USE crm_db;
-- Fails on existing duplicate active primary contacts; resolve explicitly before retry.
ALTER TABLE contacts
 ADD COLUMN primary_customer_id BIGINT GENERATED ALWAYS AS
 (CASE WHEN is_primary=1 AND is_deleted=0 THEN customer_id ELSE NULL END) STORED,
 ADD UNIQUE KEY uq_s3_02_contact_primary(primary_customer_id);
CREATE TABLE IF NOT EXISTS contact_company_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,contact_id BIGINT NOT NULL,
 from_customer_id BIGINT NOT NULL,to_customer_id BIGINT NOT NULL,
 changed_by BIGINT NOT NULL,changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 note VARCHAR(1000),KEY ix_s3_02_history_contact(contact_id,changed_at,id),
 FOREIGN KEY(contact_id) REFERENCES contacts(id),
 FOREIGN KEY(from_customer_id) REFERENCES customers(id),
 FOREIGN KEY(to_customer_id) REFERENCES customers(id),
 FOREIGN KEY(changed_by) REFERENCES users(id)
) ENGINE=InnoDB;
INSERT IGNORE INTO permissions(code,name) VALUES
 ('contact.read','Xem người liên hệ'),('contact.create','Tạo người liên hệ'),
 ('contact.update','Sửa người liên hệ'),('contact.delete','Xóa người liên hệ');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
 WHERE r.code='ADMIN' AND p.code IN ('contact.read','contact.create','contact.update','contact.delete');
-- Other roles retain existing grants; customer scopes govern contact ownership.
