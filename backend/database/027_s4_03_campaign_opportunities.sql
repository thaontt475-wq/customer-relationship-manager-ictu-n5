-- S4-03: apply after 024_sprint4_campaigns_leads.sql and the Opportunity foundation.
-- Operates only on the selected schema; no demo data, USE, or mass business update.
CREATE TABLE IF NOT EXISTS campaign_opportunities (
 campaign_id BIGINT NOT NULL, opportunity_id BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(campaign_id,opportunity_id),
 CONSTRAINT fk_s403_co_campaign FOREIGN KEY(campaign_id) REFERENCES campaigns(id),
 CONSTRAINT fk_s403_co_opportunity FOREIGN KEY(opportunity_id) REFERENCES opportunities(id)
);
INSERT IGNORE INTO permissions(code,name) VALUES('campaign.opportunity_manage','Liên kết cơ hội chiến dịch');
INSERT IGNORE INTO role_permissions(role_id,permission_id)
 SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
 WHERE r.code IN('ADMIN','DIRECTOR','TEAM_LEAD','MARKETING') AND p.code LIKE 'campaign.%';
INSERT IGNORE INTO role_module_scopes(role_id,module_code,scope_type)
 SELECT r.id,'campaign',CASE WHEN r.code IN('ADMIN','DIRECTOR') THEN 'ALL' WHEN r.code='TEAM_LEAD' THEN 'TEAM' ELSE 'SELF' END
 FROM roles r WHERE r.code IN('ADMIN','DIRECTOR','TEAM_LEAD','MARKETING');
