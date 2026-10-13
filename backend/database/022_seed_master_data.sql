-- Sprint 3 / Catalogs Seed: Default Master Data for CRM
USE crm_db;

-- 1. Ngành nghề kinh doanh (industry)
INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
('industry', 'CNTT', 'Công nghệ thông tin', 1, 1),
('industry', 'RETAIL', 'Bán lẻ / Thương mại', 1, 2),
('industry', 'MANUFACTURING', 'Sản xuất / Chế tạo', 1, 3),
('industry', 'FINANCE', 'Tài chính / Ngân hàng / Bảo hiểm', 1, 4),
('industry', 'REAL_ESTATE', 'Bất động sản / Xây dựng', 1, 5),
('industry', 'LOGISTICS', 'Vận tải / Kho bãi / Logistics', 1, 6),
('industry', 'HEALTHCARE', 'Y tế / Dược phẩm', 1, 7),
('industry', 'EDUCATION', 'Giáo dục / Đào tạo', 1, 8),
('industry', 'FNB', 'Thực phẩm / Đồ uống (F&B)', 1, 9),
('industry', 'SERVICES', 'Dịch vụ / Du lịch / Khách sạn', 1, 10),
('industry', 'OTHER', 'Ngành nghề khác', 1, 11);

-- 2. Quy mô doanh nghiệp (company-size)
INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
('company-size', 'MICRO', 'Dưới 10 nhân sự (Siêu nhỏ)', 1, 1),
('company-size', 'SMALL', '10 - 50 nhân sự (Nhỏ)', 1, 2),
('company-size', 'MEDIUM', '50 - 200 nhân sự (Vừa)', 1, 3),
('company-size', 'LARGE', '200 - 500 nhân sự (Lớn)', 1, 4),
('company-size', 'ENTERPRISE', 'Trên 500 nhân sự (Tập đoàn)', 1, 5);

-- 3. Nguồn khách hàng tiềm năng (lead-source)
INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
('lead-source', 'WEBSITE', 'Website / Đăng ký trực tuyến', 1, 1),
('lead-source', 'REFERRAL', 'Giới thiệu từ đối tác / khách hàng', 1, 2),
('lead-source', 'COLD_CALL', 'Telesales / Tiếp cận trực tiếp', 1, 3),
('lead-source', 'SOCIAL_MEDIA', 'Mạng xã hội (Facebook, LinkedIn)', 1, 4),
('lead-source', 'EVENT', 'Hội thảo / Sự kiện / Triển lãm', 1, 5),
('lead-source', 'OTHER', 'Nguồn khác', 1, 6);

-- 4. Loại hoạt động chăm sóc (activity-type)
INSERT IGNORE INTO master_data (type, code, name, active, display_order) VALUES
('activity-type', 'CALL', 'Cuộc gọi điện thoại', 1, 1),
('activity-type', 'MEETING', 'Gặp mặt trực tiếp', 1, 2),
('activity-type', 'EMAIL', 'Gửi thư điện tử (Email)', 1, 3),
('activity-type', 'DEMO', 'Demo giới thiệu sản phẩm', 1, 4),
('activity-type', 'NOTE', 'Ghi chú nội bộ', 1, 5);
