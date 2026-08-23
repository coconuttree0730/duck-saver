-- duck_saver_notification：通知接收人 · 通知配置

CREATE TABLE IF NOT EXISTS recipient (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    account_name VARCHAR(50)  NOT NULL COMMENT '账户名',
    email        VARCHAR(128) NOT NULL COMMENT '接收邮箱',
    frequency    VARCHAR(20)  NOT NULL DEFAULT 'WEEKLY' COMMENT '通知频率：WEEKLY/MONTHLY/QUARTERLY',
    enabled      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用通知',
    version      BIGINT       NOT NULL DEFAULT 0,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipient_account (account_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知接收人';

CREATE TABLE IF NOT EXISTS notification_config (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    recipient_id   BIGINT      NOT NULL COMMENT 'recipient.id',
    type           VARCHAR(20) NOT NULL COMMENT '通知类型：BACKUP/BILL_REMINDER',
    cron_expression VARCHAR(30) NOT NULL COMMENT '触发 cron',
    active         TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用',
    last_notified  DATETIME    NULL COMMENT '上次通知时间',
    version        BIGINT      NOT NULL DEFAULT 0,
    deleted        TINYINT     NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_config_recipient_type (recipient_id, type),
    CONSTRAINT fk_notification_config_recipient FOREIGN KEY (recipient_id) REFERENCES recipient (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知配置';
