-- P1 会话与登录锁定计数。已执行 0001 的库用本脚本补齐。
SET @ccds_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ccds_account'
      AND COLUMN_NAME = 'failed_login_count'
);
SET @ccds_add_failed_count = IF(
    @ccds_col_exists = 0,
    'ALTER TABLE ccds_account ADD COLUMN failed_login_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''连续失败次数'' AFTER is_must_change_password',
    'SELECT 1'
);
PREPARE ccds_stmt FROM @ccds_add_failed_count;
EXECUTE ccds_stmt;
DEALLOCATE PREPARE ccds_stmt;

CREATE TABLE IF NOT EXISTS ccds_account_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '账号',
    token_hash VARCHAR(64) NOT NULL COMMENT 'refresh 令牌 SHA-256',
    expire_at DATETIME NOT NULL COMMENT 'refresh 过期时间',
    device_hint VARCHAR(64) NULL COMMENT '设备提示，不含敏感标识',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_session_token (token_hash),
    KEY idx_account_session_account (account_id),
    KEY idx_account_session_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号 refresh 会话';
