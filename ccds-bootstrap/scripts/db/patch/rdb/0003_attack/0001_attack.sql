-- P3 内攻事件与站快照。人员卡片表已在 0002_roster/0003_attack_person.sql，本脚本不改已发布列。
CREATE TABLE IF NOT EXISTS ccds_attack_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    person_id BIGINT UNSIGNED NULL COMMENT '人员卡片',
    event_type VARCHAR(16) NOT NULL COMMENT 'pre_add/enter/remeasure/withdraw/delete',
    payload_json VARCHAR(2000) NULL COMMENT '事件载荷',
    client_event_id VARCHAR(64) NOT NULL COMMENT '客户端幂等键',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attack_event_client (client_event_id),
    KEY idx_attack_event_station (station_id),
    KEY idx_attack_event_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内攻事件';

CREATE TABLE IF NOT EXISTS ccds_station_snapshot (
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    in_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '安全人数',
    warn_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预警人数',
    danger_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '危险人数',
    out_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已撤出人数',
    pending_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预录入人数',
    group_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '有未撤出人员的编组数',
    last_event_at DATETIME NULL COMMENT '最近事件时间',
    payload_json VARCHAR(2000) NULL COMMENT '快照摘要',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (station_id),
    KEY idx_station_snapshot_active (in_count, warn_count, danger_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内攻快照';
