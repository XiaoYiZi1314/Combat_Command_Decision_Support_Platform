-- 人员编组守卫与跨站调动（改法明细 4.1 / 4.2 / 4.3）。只 ADD COLUMN / CREATE TABLE，不改已发布 DDL。
-- 前置：0002_roster/0001_roster.sql、0009_legacy_seed/0000_landing_tables.sql 已执行。

-- 档案号：与旧系统 p_12 对照的对外编号。本仓库主键是全局自增 id，档案号作为独立列维护。
ALTER TABLE ccds_profile
    ADD COLUMN archive_no VARCHAR(32) NULL COMMENT '档案号，站内唯一',
    ADD KEY idx_profile_station_archive (station_id, archive_no);

-- 编组槽位：固定编制卡占用 1/2/3（指挥员/安全员/通信员），作战组不得使用。
ALTER TABLE ccds_battle_group
    ADD COLUMN slot_no INT UNSIGNED NULL COMMENT '固定槽位 1指挥员 2安全员 3通信员，作战组为空',
    ADD COLUMN is_fixed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '固定编制卡 1是0否';

-- 编组删除记录：只记精确的组名与组编号，禁止通配键（删「灭火1组」不得连到「灭火救援1组」）。
CREATE TABLE IF NOT EXISTS `ccds_battle_group_tombstone` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '所属消防站ID',
    `group_name` VARCHAR(64) NOT NULL COMMENT '组名原文',
    `name_norm` VARCHAR(64) NOT NULL COMMENT '组名归一值',
    `group_id` BIGINT UNSIGNED NULL COMMENT '删除时组主键',
    `slot_no` INT UNSIGNED NULL COMMENT '槽位号；仅固定卡有值',
    `source` VARCHAR(16) NOT NULL DEFAULT 'user_delete' COMMENT '来源：user_delete/legacy_bug',
    `released_at` DATETIME NULL COMMENT '解除时间；空表示仍生效',
    `released_by` BIGINT UNSIGNED NULL COMMENT '解除人账号ID',
    `created_by` BIGINT UNSIGNED NULL COMMENT '创建人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_tomb_station_name` (`station_id`, `name_norm`),
    KEY `idx_group_tomb_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='战斗编组删除记录';

-- 跨站调动申请：通过后才落档，调入站一律新建档案号。
CREATE TABLE IF NOT EXISTS `ccds_profile_transfer` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `from_station_id` BIGINT UNSIGNED NOT NULL COMMENT '调出站ID',
    `to_station_id` BIGINT UNSIGNED NOT NULL COMMENT '调入站ID',
    `profile_id` BIGINT UNSIGNED NOT NULL COMMENT '调出站人员主键',
    `source_archive_no` VARCHAR(32) NULL COMMENT '调出站档案号',
    `target_profile_id` BIGINT UNSIGNED NULL COMMENT '并卡时目标人员主键',
    `new_archive_no` VARCHAR(32) NULL COMMENT '落档后分配的新档案号',
    `reason` VARCHAR(255) NOT NULL COMMENT '调动原因',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    `review_reason` VARCHAR(255) NULL COMMENT '审批意见',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '提交人账号ID',
    `reviewed_by` BIGINT UNSIGNED NULL COMMENT '审批人账号ID',
    `reviewer_name` VARCHAR(50) NULL COMMENT '审批人账号名',
    `reviewed_at` DATETIME NULL COMMENT '审批时间',
    `client_request_id` VARCHAR(64) NULL COMMENT '客户端请求号，提交幂等',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transfer_client` (`from_station_id`, `client_request_id`),
    KEY `idx_transfer_from` (`from_station_id`, `status`),
    KEY `idx_transfer_to` (`to_station_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员跨站调动申请';
