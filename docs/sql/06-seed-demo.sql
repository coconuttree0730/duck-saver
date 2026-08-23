SET NAMES utf8mb4;
-- demo 种子数据（幂等：INSERT IGNORE 依赖唯一键）
-- demo 用户密码为 demo（BCrypt 密文）

USE duck_saver_auth;
INSERT IGNORE INTO sys_user (username, password)
VALUES ('demo', '$2a$10$GgvOXt495sziCCnfsGP6M.H/TAbchDwPbXpJ9oCyd2MHx91i3Y5m2');

USE duck_saver_account;
INSERT IGNORE INTO account (name, currency) VALUES ('demo', 'CNY');

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT '3f7d8f9ae1c24b6f9d0a2c5e6b8d1234', id, CONVERT(UNHEX('E5B7A5E8B584') USING utf8mb4), 15000.00, 'CNY', CONVERT(UNHEX('E585B6E4BB96') USING utf8mb4), 'INCOME', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT 'a1b2c3d4e5f60718293a4b5c6d7e8f90', id, CONVERT(UNHEX('E58D88E9A490') USING utf8mb4), 48.00, 'CNY', CONVERT(UNHEX('E9A490E9A5AE') USING utf8mb4), 'EXPENSE', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO `transaction` (id, account_id, title, amount, currency, category, type, date)
SELECT 'b2c3d4e5f60718293a4b5c6d7e8f9001', id, CONVERT(UNHEX('E59CB0E99381E9809AE58BA4') USING utf8mb4), 12.50, 'CNY', CONVERT(UNHEX('E4BAA4E9809A') USING utf8mb4), 'EXPENSE', CURDATE()
FROM account WHERE name = 'demo';

INSERT IGNORE INTO saving (account_id, amount, interest, deposit, currency)
SELECT id, 10000.00, 0.0150, 5000.00, 'CNY' FROM account WHERE name = 'demo';

USE duck_saver_notification;
INSERT IGNORE INTO recipient (account_name, email) VALUES ('demo', 'demo@example.com');

INSERT IGNORE INTO notification_config (recipient_id, type, cron_expression, active)
SELECT id, 'BACKUP', '0 0 12 * * *', 1 FROM recipient WHERE account_name = 'demo';

INSERT IGNORE INTO notification_config (recipient_id, type, cron_expression, active)
SELECT id, 'BILL_REMINDER', '0 0 10 1 * *', 0 FROM recipient WHERE account_name = 'demo';
