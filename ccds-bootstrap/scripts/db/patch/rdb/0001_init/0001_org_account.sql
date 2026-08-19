-- 一期组织与账号表。口令哈希由运维在执行种子前注入，禁止明文入库。
CREATE TABLE IF NOT EXISTS ccds_brigade (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(32) NOT NULL COMMENT '编制编码',
    name VARCHAR(64) NOT NULL COMMENT '大队名称',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_brigade_code (code),
    UNIQUE KEY uk_brigade_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大队编制';

CREATE TABLE IF NOT EXISTS ccds_station (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    brigade_id BIGINT UNSIGNED NULL COMMENT '所属大队，空为支队直属',
    name VARCHAR(64) NOT NULL COMMENT '站名',
    name_norm VARCHAR(64) NOT NULL COMMENT '归一站名',
    category VARCHAR(32) NOT NULL COMMENT '站类别',
    sort_no INT UNSIGNED NOT NULL COMMENT '展示顺序',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_name (name),
    UNIQUE KEY uk_station_name_norm (name_norm),
    KEY idx_station_brigade (brigade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消防站编制';

CREATE TABLE IF NOT EXISTS ccds_station_alias (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '目标站',
    alias VARCHAR(64) NOT NULL COMMENT '导入别名',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_alias (alias),
    KEY idx_station_alias_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站名别名';

CREATE TABLE IF NOT EXISTS ccds_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(32) NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 口令哈希',
    role VARCHAR(16) NOT NULL COMMENT 'station/brigade/hq/developer',
    station_id BIGINT UNSIGNED NULL COMMENT '站级所属站',
    brigade_id BIGINT UNSIGNED NULL COMMENT '大队所属大队',
    is_must_change_password TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 必须改密',
    locked_until DATETIME NULL COMMENT '锁定截止',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_username (username),
    KEY idx_account_station (station_id),
    KEY idx_account_brigade (brigade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录账号';
