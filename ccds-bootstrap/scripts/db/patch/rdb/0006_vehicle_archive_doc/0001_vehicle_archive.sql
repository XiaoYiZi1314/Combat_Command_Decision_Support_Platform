-- 车辆档案表
CREATE TABLE IF NOT EXISTS `ccds_vehicle` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '所属消防站ID',
    `type` VARCHAR(50) NOT NULL DEFAULT 'other' COMMENT '车辆类型码',
    `vehicle_type` VARCHAR(50) NULL COMMENT '具体类型名称',
    `plate` VARCHAR(30) NULL COMMENT '转改后号牌/地方号牌',
    `old_plate` VARCHAR(30) NULL COMMENT '原号牌',
    `status` VARCHAR(20) NOT NULL DEFAULT '执勤' COMMENT '状态：执勤/报修',
    `model` VARCHAR(100) NULL COMMENT '厂牌型号',
    `maker` VARCHAR(100) NULL COMMENT '生产厂家',
    `water_cap` VARCHAR(20) NULL COMMENT '载水量(t)',
    `foam_cap` VARCHAR(20) NULL COMMENT '载泡沫量(t)',
    `powder_cap` VARCHAR(20) NULL COMMENT '载干粉量(t)',
    `work_height` VARCHAR(20) NULL COMMENT '举高高度(m)',
    `engine_no` VARCHAR(50) NULL COMMENT '发动机号',
    `vin` VARCHAR(50) NULL COMMENT '车辆识别代号',
    `color` VARCHAR(20) NULL COMMENT '车体颜色',
    `made_date` VARCHAR(50) NULL COMMENT '出厂日期(原样文本)',
    `equip_date` VARCHAR(50) NULL COMMENT '装备时间(原样文本)',
    `notes` VARCHAR(500) NULL COMMENT '备注',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_station` (`station_id`),
    KEY `idx_vehicle_type` (`type`),
    KEY `idx_vehicle_plate` (`plate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆档案';

-- 车辆变更申请表
CREATE TABLE IF NOT EXISTS `ccds_vehicle_request` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '申请站ID',
    `action` VARCHAR(20) NOT NULL COMMENT '动作：add/modify/delete',
    `item_json` TEXT NOT NULL COMMENT '目标车辆快照JSON',
    `before_json` TEXT NULL COMMENT '变更前快照JSON',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '提交人账号ID',
    `reviewed_by` BIGINT UNSIGNED NULL COMMENT '审批人账号ID',
    `reviewer_name` VARCHAR(50) NULL COMMENT '审批人账号名',
    `reviewed_at` DATETIME NULL COMMENT '审批时间',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_request_station` (`station_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆变更申请';

-- 内攻历史归档表
CREATE TABLE IF NOT EXISTS `ccds_attack_archive` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `event_kind` VARCHAR(20) NOT NULL DEFAULT 'drill' COMMENT '类型：drill演练/dispatch出警',
    `event_name` VARCHAR(100) NULL COMMENT '事件名称',
    `location` VARCHAR(200) NULL COMMENT '地点',
    `mode` VARCHAR(20) NULL COMMENT '归档时操作模式',
    `started_at` DATETIME NULL COMMENT '本次内攻开始时间',
    `finished_at` DATETIME NULL COMMENT '归档完成时间',
    `person_count` INT NOT NULL DEFAULT 0 COMMENT '记录人数',
    `person_snapshot_json` MEDIUMTEXT NULL COMMENT '人员快照JSON',
    `created_by` BIGINT UNSIGNED NULL COMMENT '归档人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_archive_station` (`station_id`),
    KEY `idx_archive_finished` (`finished_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内攻历史归档';

-- 支队本级车辆挂靠站（客户 APK 的「支队本级」条目）
INSERT INTO ccds_station (id, brigade_id, name, name_norm, category, sort_no)
SELECT 24, NULL, '支队本级', '支队本级', '支队本级', 24
WHERE NOT EXISTS (SELECT 1 FROM ccds_station WHERE id = 24);
