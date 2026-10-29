-- 旧数据迁移落地表。只新增表，不改动任何已发布 DDL。
-- 用途：旧系统（R2 云端）里的人员删除记录、站级空呼阈值、站级作业模式在本仓库尚无落点，
--       迁移时先建最小承载表，后续 p11 需求实现（4.2 / 3.3 / 2.2）直接复用，不再二次改表。

-- 人员删除记录（墓碑）。旧系统 persons/index.json 的 deletedProfileKeys 落在这里。
CREATE TABLE IF NOT EXISTS `ccds_profile_tombstone` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '所属消防站ID',
    `profile_id` BIGINT UNSIGNED NULL COMMENT '现有档案主键；迁移记录为空',
    `legacy_profile_id` VARCHAR(64) NULL COMMENT '旧系统档案号，如 p_1 / p_xfer_3_xxx',
    `archive_no` VARCHAR(32) NULL COMMENT '档案号；旧系统无此概念时为空',
    `name` VARCHAR(32) NULL COMMENT '姓名',
    `name_norm` VARCHAR(32) NULL COMMENT '姓名归一值',
    `nfc_tag` VARCHAR(64) NULL COMMENT 'NFC 原始编号',
    `nfc_norm` VARCHAR(64) NULL COMMENT 'NFC 归一编号（去分隔符并大写）',
    `source` VARCHAR(16) NOT NULL DEFAULT 'user_delete' COMMENT '来源：user_delete/transfer_out/legacy_import',
    `name_released_at` DATETIME NULL COMMENT '姓名解除时间；空表示仍生效',
    `nfc_released_at` DATETIME NULL COMMENT 'NFC 解除时间；空表示仍生效',
    `archive_released_at` DATETIME NULL COMMENT '档案号解除时间；空表示仍生效',
    `released_by` BIGINT UNSIGNED NULL COMMENT '解封人账号ID',
    `created_by` BIGINT UNSIGNED NULL COMMENT '创建人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_profile_tomb_station_legacy` (`station_id`, `legacy_profile_id`),
    UNIQUE KEY `uk_profile_tomb_station_nfc` (`station_id`, `nfc_norm`),
    KEY `idx_profile_tomb_station_name` (`station_id`, `name_norm`),
    KEY `idx_profile_tomb_station_profile` (`station_id`, `profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员删除记录';

-- 站级空呼阈值。旧系统 snapshot.settings 的四项阈值与默认强度、场景、语音开关落在这里。
CREATE TABLE IF NOT EXISTS `ccds_station_scba_setting` (
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `warn_pressure` DECIMAL(4, 1) NOT NULL DEFAULT 10.0 COMMENT '压力预警阈值 MPa',
    `danger_pressure` DECIMAL(4, 1) NOT NULL DEFAULT 4.0 COMMENT '压力危险阈值 MPa',
    `warn_time_min` INT UNSIGNED NOT NULL DEFAULT 12 COMMENT '时间预警阈值 分钟',
    `danger_time_min` INT UNSIGNED NOT NULL DEFAULT 5 COMMENT '时间危险阈值 分钟',
    `work_level` VARCHAR(16) NOT NULL DEFAULT 'moderate' COMMENT '默认作业强度 light/moderate/heavy',
    `scene` VARCHAR(16) NOT NULL DEFAULT 'flat' COMMENT '默认场景 flat/highrise/dark/large/search',
    `voice_enabled` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '语音提醒 1开0关',
    `updated_by` BIGINT UNSIGNED NULL COMMENT '最后修改人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站级空呼阈值';

-- 站级作业模式（测试/训练/出警）。旧系统 snapshot.operationMode 落在这里。
CREATE TABLE IF NOT EXISTS `ccds_station_work_mode` (
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `work_mode` VARCHAR(16) NOT NULL DEFAULT 'test' COMMENT '作业模式 test/training/dispatch',
    `updated_by` BIGINT UNSIGNED NULL COMMENT '最后修改人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站级作业模式';
