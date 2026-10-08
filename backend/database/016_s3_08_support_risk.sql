-- S3-08 only. Select the intended CRM database before running.
-- Requires existing customers/users/permissions/roles and Customer module scopes.
CREATE TABLE IF NOT EXISTS support_requests (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,customer_id BIGINT NOT NULL,
 title VARCHAR(255) NOT NULL,description TEXT NULL,
 priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
 assignee_user_id BIGINT NOT NULL,status VARCHAR(30) NOT NULL DEFAULT 'NEW',
 created_by BIGINT NOT NULL,updated_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 is_deleted TINYINT(1) NOT NULL DEFAULT 0,deleted_by BIGINT NULL,deleted_at DATETIME NULL,
 KEY ix_s3_08_support_customer(customer_id,is_deleted,status,id),
 KEY ix_s3_08_support_assignee(assignee_user_id,is_deleted,status),
 FOREIGN KEY(customer_id) REFERENCES customers(id),
 FOREIGN KEY(assignee_user_id) REFERENCES users(id),
 FOREIGN KEY(created_by) REFERENCES users(id),FOREIGN KEY(updated_by) REFERENCES users(id),
 FOREIGN KEY(deleted_by) REFERENCES users(id),
 CHECK(priority IN ('URGENT','HIGH','MEDIUM','LOW')),
 CHECK(status IN ('NEW','PROCESSING','WAITING_CUSTOMER','RESOLVED','CLOSED'))
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS notifications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,customer_id BIGINT NOT NULL,
 type VARCHAR(50) NOT NULL,title VARCHAR(255) NOT NULL,message VARCHAR(1000) NOT NULL,
 event_key VARCHAR(150) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,read_at DATETIME NULL,
 UNIQUE KEY uq_s3_08_notification_event(event_key),
 KEY ix_s3_08_notification_recipient(user_id,read_at,id),
 KEY ix_s3_08_notification_customer(customer_id),
 FOREIGN KEY(user_id) REFERENCES users(id),FOREIGN KEY(customer_id) REFERENCES customers(id)
) ENGINE=InnoDB;
DELIMITER $$
DROP PROCEDURE IF EXISTS EnsureS308RiskColumns$$
CREATE PROCEDURE EnsureS308RiskColumns()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='support_risk_active') THEN
  ALTER TABLE customers ADD COLUMN support_risk_active TINYINT(1) NOT NULL DEFAULT 0;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='support_risk_episode') THEN
  ALTER TABLE customers ADD COLUMN support_risk_episode BIGINT NOT NULL DEFAULT 0;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='support_risk_notified_owner_id') THEN
  ALTER TABLE customers ADD COLUMN support_risk_notified_owner_id BIGINT NULL;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='customers' AND COLUMN_NAME='support_risk_updated_at') THEN
  ALTER TABLE customers ADD COLUMN support_risk_updated_at DATETIME NULL;
 END IF;
END$$
CALL EnsureS308RiskColumns()$$
DROP PROCEDURE EnsureS308RiskColumns$$
DELIMITER ;
INSERT IGNORE INTO permissions(code,name) VALUES
 ('support.read','Xem yêu cầu hỗ trợ'),('support.create','Tạo yêu cầu hỗ trợ'),
 ('support.update','Sửa yêu cầu hỗ trợ'),('support.delete','Xóa yêu cầu hỗ trợ'),
 ('notification.read','Xem thông báo');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r JOIN permissions p
 ON p.code IN ('support.read','support.create','support.update','support.delete','notification.read')
 WHERE r.code IN ('ADMIN','DIRECTOR','TEAM_LEAD','SALES_REP','CUSTOMER_SUCCESS');
-- Other roles can be granted explicit permissions using the existing role management.
-- No existing CRM rows or role scopes are rewritten.
