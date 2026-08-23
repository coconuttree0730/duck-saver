-- demo 种子数据（幂等：INSERT IGNORE 依赖唯一键）
-- demo 用户密码为 demo（BCrypt 密文）

USE duck_saver_auth;
INSERT IGNORE INTO sys_user (username, password)
VALUES ('demo', '$2a$10$GgvOXt495sziCCnfsGP6M.H/TAbchDwPbXpJ9oCyd2MHx91i3Y5m2');

USE duck_saver_account;
INSERT IGNORE INTO account (name, currency) VALUES ('demo', 'CNY');

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT '3f7d8f9ae1c24b6f9d0a2c5e6b8d1234', id, '工资', 15000.00, 'CNY', '其他', 'INCOME', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT 'a1b2c3d4e5f60718293a4b5c6d7e8f90', id, '午餐', 48.00, 'CNY', '餐饮', 'EXPENSE', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT 'b2c3d4e5f60718293a4b5c6d7e8f9001', id, '地铁通勤', 12.50, 'CNY', '交通', 'EXPENSE', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO saving (account_id, amount, interest, deposit, currency)
SELECT id, 10000.00, 0.0150, 5000.00, 'CNY' FROM account WHERE name = 'demo';

USE duck_saver_notification;
INSERT IGNORE INTO recipient (account_name, email) VALUES ('demo', 'demo@example.com');

INSERT IGNORE INTO notification_config (recipient_id, type, cron_expression, active)
SELECT id, 'BACKUP', '0 0 12 * * *', 1 FROM recipient WHERE account_name = 'demo';

INSERT IGNORE INTO notification_config (recipient_id, type, cron_expression, active)
SELECT id, 'BILL_REMINDER', '0 0 10 1 * *', 0 FROM recipient WHERE account_name = 'demo';
