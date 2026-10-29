-- 水源台账补列：水源编号、保留标记、市政底表关联、泄水阀与保暖措施。
-- 约束：只 ADD COLUMN / ADD KEY / CREATE TABLE，不改任何已发布 DDL、不删列。
-- 前置：0004_water/0001_water.sql（建表）、0009_legacy_seed/0002_water_fields.sql（11 个结构化列）已执行。
-- 幂等：MySQL 不支持 ADD COLUMN IF NOT EXISTS，本文件只执行一次；重复执行会报 Duplicate column，属预期。
-- 语义：
--   water_no     水源编号，站内唯一。用于「删过的水源不再回流」按编号拦截，也是审批通过新增时分配的正式编号。
--   retained     明确要求保留的卡（1=保留）。被删除后打开列表要按原编号补回，不能换新编号。
--   base_no      来源市政底表编号，空表示非底表来源。
--   drain_*      泄水阀四列（是否可查看、好用情况、无法查看原因、备注）。
--   warmth_*     保暖措施两列（是否有措施、无措施原因）。
--   repair_reason 是报修时的原因说明（人类文本，与 reason_text 的机器归类原文分离）。

ALTER TABLE ccds_water_source
    ADD COLUMN water_no VARCHAR(32) NULL COMMENT '水源编号，站内唯一',
    ADD COLUMN retained TINYINT(1) NOT NULL DEFAULT 0 COMMENT '明确要求保留 1是0否',
    ADD COLUMN base_no VARCHAR(32) NULL COMMENT '来源市政底表编号',
    ADD COLUMN drain_viewable VARCHAR(8) NULL COMMENT '泄水阀是否可查看 yes/no',
    ADD COLUMN drain_usable VARCHAR(8) NULL COMMENT '泄水阀好用情况 good/bad',
    ADD COLUMN drain_reason VARCHAR(255) NULL COMMENT '无法查看泄水阀原因',
    ADD COLUMN drain_notes VARCHAR(255) NULL COMMENT '泄水阀备注',
    ADD COLUMN warmth_measure TINYINT(1) NULL COMMENT '是否有保暖措施 1是0否',
    ADD COLUMN warmth_reason VARCHAR(255) NULL COMMENT '无保暖措施原因',
    ADD COLUMN repair_reason VARCHAR(255) NULL COMMENT '报修原因说明',
    ADD KEY idx_water_station_no (station_id, water_no),
    ADD KEY idx_water_base_no (base_no);

-- 已删除水源名单。删除时先写本表再软删，任何同步 / 底表合并 / 导入路径都必须按编号拦截回流。
CREATE TABLE IF NOT EXISTS `ccds_water_deleted` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '所属消防站ID',
    `water_no` VARCHAR(32) NOT NULL COMMENT '已删除水源编号',
    `name` VARCHAR(64) NULL COMMENT '删除时名称',
    `type` VARCHAR(16) NULL COMMENT '删除时类型 crane/ground/underground',
    `source` VARCHAR(16) NOT NULL DEFAULT 'user_delete' COMMENT '来源：user_delete/approval/legacy_import',
    `released_at` DATETIME NULL COMMENT '解除时间；空表示仍生效',
    `released_by` BIGINT UNSIGNED NULL COMMENT '解除人账号ID',
    `created_by` BIGINT UNSIGNED NULL COMMENT '创建人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_water_deleted_station_no` (`station_id`, `water_no`),
    KEY `idx_water_deleted_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水源删除记录';

-- 全市市政水源底表。按批次导入，confirmed=1 的批次可替换该站旧目录。
CREATE TABLE IF NOT EXISTS `ccds_water_base` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '归属消防站ID',
    `base_no` VARCHAR(32) NOT NULL COMMENT '底表编号',
    `name` VARCHAR(64) NOT NULL COMMENT '名称',
    `type` VARCHAR(16) NOT NULL COMMENT 'crane/ground/underground',
    `address` VARCHAR(128) NULL COMMENT '地址',
    `lng` DECIMAL(10, 6) NULL COMMENT '经度 WGS84',
    `lat` DECIMAL(10, 6) NULL COMMENT '纬度 WGS84',
    `batch_no` VARCHAR(32) NOT NULL COMMENT '导入批次号',
    `confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已确认批次 1是0否',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_water_base_no` (`station_id`, `base_no`),
    KEY `idx_water_base_batch` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全市市政水源底表';
