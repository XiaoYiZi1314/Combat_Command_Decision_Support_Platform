-- 水源变更申请与出水视频（改法明细 1.5 / 1.7）。只建新表，不改已发布 DDL。
-- 审批模式对齐 ccds_vehicle_request，并补车辆模板缺的两个硬字段：申请原因、变更前后状态。
-- 车辆模板 reviewed_at 恒 NULL 的缺陷在 VehicleServiceImpl 已修，本表从建表起就要求落审批时间。

CREATE TABLE IF NOT EXISTS `ccds_water_request` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '申请站ID',
    `action` VARCHAR(20) NOT NULL COMMENT '动作：add/modify/delete',
    `water_id` BIGINT UNSIGNED NULL COMMENT '目标水源ID，新增为空',
    `water_no` VARCHAR(32) NULL COMMENT '目标水源编号',
    `item_json` TEXT NOT NULL COMMENT '申请内容快照JSON',
    `before_json` TEXT NULL COMMENT '变更前快照JSON',
    `from_status` VARCHAR(16) NULL COMMENT '变更前状态 active/repair',
    `to_status` VARCHAR(16) NULL COMMENT '变更后状态 active/repair',
    `reason` VARCHAR(255) NOT NULL COMMENT '申请原因，必填',
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
    UNIQUE KEY `uk_water_request_client` (`station_id`, `client_request_id`),
    KEY `idx_water_request_station` (`station_id`, `status`),
    KEY `idx_water_request_review` (`status`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水源变更申请';

-- 出水视频。时长与三帧由客户端上报（服务端无媒体库），check_status 仅作辅助判断，不作考核依据。
CREATE TABLE IF NOT EXISTS `ccds_water_video` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `station_id` BIGINT UNSIGNED NOT NULL COMMENT '消防站ID',
    `water_id` BIGINT UNSIGNED NOT NULL COMMENT '水源ID',
    `file_id` BIGINT UNSIGNED NOT NULL COMMENT '视频文件ID',
    `duration_ms` INT UNSIGNED NULL COMMENT '客户端上报时长毫秒',
    `frame_file_ids` VARCHAR(255) NULL COMMENT '开头/中间/结尾三帧文件ID，逗号分隔',
    `check_status` VARCHAR(16) NOT NULL DEFAULT 'failed' COMMENT '判定结果 passed/rejected/failed',
    `check_reason` VARCHAR(255) NULL COMMENT '不通过原因',
    `client_reported` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '数据由客户端上报 1是',
    `created_by` BIGINT UNSIGNED NULL COMMENT '上传人账号ID',
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_water_video_water` (`water_id`),
    KEY `idx_water_video_station` (`station_id`, `check_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水源出水视频';
