-- duck_saver_account：账户 · 交易记录（Transaction）· 储蓄概况
USE duck_saver_account;

CREATE TABLE IF NOT EXISTS account (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(50) NOT NULL COMMENT '账户名（即登录用户名）',
    currency   VARCHAR(8)  NOT NULL DEFAULT 'CNY' COMMENT '币种',
    version    BIGINT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='账户';

CREATE TABLE IF NOT EXISTS `transaction` (
    id         CHAR(32)      NOT NULL COMMENT '主键（应用侧生成 UUID，无连字符，对外字符串呈现）',
    account_id BIGINT        NOT NULL COMMENT '所属账户',
    title      VARCHAR(100)  NOT NULL COMMENT '标题/备注',
    amount     DECIMAL(12,2) NOT NULL COMMENT '金额',
    currency   VARCHAR(8)    NOT NULL DEFAULT 'CNY' COMMENT '币种',
    category   VARCHAR(16)   NOT NULL COMMENT '分类：餐饮/交通/购物/娱乐/居住/通讯/医疗/教育/其他',
    type       VARCHAR(8)    NOT NULL COMMENT '收支类型：INCOME/EXPENSE',
    date       DATE          NOT NULL COMMENT '发生日期',
    version    BIGINT        NOT NULL DEFAULT 0,
    deleted    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_transaction_account (account_id, date),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='交易记录';

CREATE TABLE IF NOT EXISTS saving (
    id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    account_id BIGINT        NOT NULL COMMENT '所属账户',
    amount     DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '储蓄总额',
    interest   DECIMAL(8,4)  NOT NULL DEFAULT 0 COMMENT '年利率（如 0.0150）',
    deposit    TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否存款',
    currency   VARCHAR(8)    NOT NULL DEFAULT 'CNY',
    version    BIGINT        NOT NULL DEFAULT 0,
    deleted    TINYINT       NOT NULL DEFAULT 0,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saving_account (account_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='储蓄概况';
