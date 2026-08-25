-- 值班日历表
CREATE TABLE IF NOT EXISTS `ccds_duty_day` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `duty_date` DATE NOT NULL COMMENT '值班日期',
    `main_officer_id` BIGINT UNSIGNED NULL COMMENT '主官档案ID',
    `deputy_officer_id` BIGINT UNSIGNED NULL COMMENT '副职档案ID',
    `note` VARCHAR(500) NULL COMMENT '备注',
    `group_snapshot_json` TEXT NULL COMMENT '当日战斗编组快照JSON',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_station_date` (`station_id`, `duty_date`),
    KEY `idx_duty_date` (`duty_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='值班日历';

-- 重点单位表
CREATE TABLE IF NOT EXISTS `ccds_key_unit` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '所属消防站ID',
    `name` VARCHAR(200) NOT NULL COMMENT '单位名称',
    `address` VARCHAR(500) NULL COMMENT '地址',
    `category` VARCHAR(50) NOT NULL COMMENT '类别',
    `contact` VARCHAR(50) NULL COMMENT '联系人',
    `phone` VARCHAR(20) NULL COMMENT '联系电话',
    `notes` VARCHAR(1000) NULL COMMENT '备注',
    `plan_text` TEXT NULL COMMENT '预案正文',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted_at` DATETIME NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_station` (`station_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重点单位预案';

-- 文件对象表
CREATE TABLE IF NOT EXISTS `ccds_file_object` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type` VARCHAR(50) NOT NULL COMMENT '业务类型',
    `biz_id` BIGINT UNSIGNED NOT NULL COMMENT '业务ID',
    `cos_key` VARCHAR(500) NOT NULL COMMENT 'COS对象键',
    `content_type` VARCHAR(100) NULL COMMENT 'MIME类型',
    `size_bytes` BIGINT UNSIGNED NOT NULL COMMENT '文件大小(字节)',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_cos_key` (`cos_key`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件对象';

-- 共享令牌表
CREATE TABLE IF NOT EXISTS `ccds_share_token` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `token_hash` VARCHAR(64) NOT NULL COMMENT '令牌哈希(SHA256)',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `revoked_at` DATETIME NULL COMMENT '作废时间',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_station` (`station_id`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='共享令牌';
