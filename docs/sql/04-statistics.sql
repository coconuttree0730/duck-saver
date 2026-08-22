-- duck_saver_statistics：每日数据点（时间序列）
USE duck_saver_statistics;

CREATE TABLE IF NOT EXISTS data_point (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    account_name VARCHAR(50) NOT NULL COMMENT '账户名',
    date         DATE        NOT NULL COMMENT '数据点日期',
    incomes      JSON        NULL COMMENT '收入明细集合 [{title, amount}]',
    expenses     JSON        NULL COMMENT '支出明细集合 [{title, amount}]',
    statistics   JSON        NULL COMMENT '统计量 {INCOMES_AMOUNT, EXPENSES_AMOUNT, SAVING_AMOUNT}',
    version      BIGINT      NOT NULL DEFAULT 0,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_data_point_account_date (account_name, date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='每日数据点（同日覆盖写）';
