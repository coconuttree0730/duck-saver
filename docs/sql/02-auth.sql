-- duck_saver_auth：用户与 OAuth 表
USE duck_saver_auth;

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    version     BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户';

CREATE TABLE IF NOT EXISTS oauth_binding (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT 'sys_user.id',
    provider   VARCHAR(20)  NOT NULL COMMENT '第三方提供方：github/wechat',
    openid     VARCHAR(128) NOT NULL COMMENT '第三方唯一标识',
    version    BIGINT       NOT NULL DEFAULT 0,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_binding_provider_openid (provider, openid),
    KEY idx_oauth_binding_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='第三方登录绑定（Spec B 使用）';

CREATE TABLE IF NOT EXISTS oauth_client (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    client_id     VARCHAR(64)  NOT NULL COMMENT '客户端标识',
    client_secret VARCHAR(128) NOT NULL COMMENT '客户端密钥',
    name          VARCHAR(64)  NULL COMMENT '客户端名称',
    scopes        VARCHAR(255) NULL COMMENT '授权范围，逗号分隔',
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_client_client_id (client_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='服务间客户端凭证（Spec B 使用）';
