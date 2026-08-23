-- Duck Saver · 库与账号初始化（幂等）
-- 每服务独立账号，仅授权自己的 Schema（见 docs/dev-environment.md）

CREATE DATABASE IF NOT EXISTS duck_saver_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS duck_saver_account DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS duck_saver_statistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS duck_saver_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'dev_password';
CREATE USER IF NOT EXISTS 'account_user'@'%' IDENTIFIED BY 'dev_password';
CREATE USER IF NOT EXISTS 'statistics_user'@'%' IDENTIFIED BY 'dev_password';
CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED BY 'dev_password';

GRANT ALL PRIVILEGES ON duck_saver_auth.* TO 'auth_user'@'%';
GRANT ALL PRIVILEGES ON duck_saver_account.* TO 'account_user'@'%';
GRANT ALL PRIVILEGES ON duck_saver_statistics.* TO 'statistics_user'@'%';
GRANT ALL PRIVILEGES ON duck_saver_notification.* TO 'notification_user'@'%';

FLUSH PRIVILEGES;
