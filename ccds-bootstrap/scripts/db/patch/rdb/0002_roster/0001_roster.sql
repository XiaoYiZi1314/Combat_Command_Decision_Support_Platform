-- P2 花名册、战斗编组与空呼标定。电话入库为密文，禁止明文。
CREATE TABLE IF NOT EXISTS ccds_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    name VARCHAR(32) NOT NULL COMMENT '姓名',
    title VARCHAR(32) NULL COMMENT '职务',
    rank_name VARCHAR(32) NULL COMMENT '衔级',
    phone_cipher VARCHAR(256) NULL COMMENT '电话密文',
    cyl_type VARCHAR(8) NOT NULL COMMENT '气瓶规格 6.8/9',
    nfc_tag VARCHAR(64) NULL COMMENT 'NFC 编号，空为 NULL',
    height_cm INT UNSIGNED NULL COMMENT '身高厘米',
    weight_kg INT UNSIGNED NULL COMMENT '体重公斤',
    age INT UNSIGNED NULL COMMENT '年龄',
    morning_pressure DECIMAL(4, 1) NULL COMMENT '早检压力 MPa',
    extra_json VARCHAR(2000) NULL COMMENT '扩展 JSON',
    alive_name VARCHAR(32) GENERATED ALWAYS AS (IF(deleted_at IS NULL, name, NULL)) STORED COMMENT '在册姓名，软删为空',
    deleted_at DATETIME NULL COMMENT '软删时间',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_station_name (station_id, alive_name),
    UNIQUE KEY uk_profile_station_nfc (station_id, nfc_tag),
    KEY idx_profile_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站级人员档案';

CREATE TABLE IF NOT EXISTS ccds_battle_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    name VARCHAR(32) NOT NULL COMMENT '组名',
    sort_no INT UNSIGNED NOT NULL COMMENT '展示顺序',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_battle_group_station_name (station_id, name),
    KEY idx_battle_group_station (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战斗编组';

CREATE TABLE IF NOT EXISTS ccds_battle_group_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '编组',
    profile_id BIGINT UNSIGNED NOT NULL COMMENT '档案',
    role_in_group VARCHAR(32) NULL COMMENT '组内角色',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_battle_group_member (group_id, profile_id),
    KEY idx_battle_group_member_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战斗编组成员';

CREATE TABLE IF NOT EXISTS ccds_scba_calibration (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    profile_id BIGINT UNSIGNED NOT NULL COMMENT '档案',
    pressure DECIMAL(4, 1) NOT NULL COMMENT '标定压力 MPa',
    full_time_sec INT UNSIGNED NOT NULL COMMENT '完全用尽秒数',
    cyl_type VARCHAR(8) NOT NULL COMMENT '气瓶规格',
    source VARCHAR(32) NOT NULL COMMENT '来源',
    measured_at DATETIME NOT NULL COMMENT '测定时间',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_scba_calibration_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空呼标定记录';
