-- Contact actions still require the matching Customer ownership scope in ContactService.
-- Grant the ordinary Sprint 3 Contact workflow to business roles; merge remains separate.
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
 WHERE r.code IN ('DIRECTOR','TEAM_LEAD','SALES_REP','CUSTOMER_SUCCESS')
 AND p.code IN ('contact.read','contact.create','contact.update','contact.delete');
