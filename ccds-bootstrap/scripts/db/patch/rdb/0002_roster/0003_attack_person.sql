-- P2 删档守卫：有未撤出内攻卡片时禁止删档案。P3 复用本表，不改已发布列。
CREATE TABLE IF NOT EXISTS ccds_attack_person (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    profile_id BIGINT UNSIGNED NULL COMMENT '花名册档案，临时人员为空',
    display_name VARCHAR(32) NOT NULL COMMENT '展示姓名',
    group_name VARCHAR(32) NULL COMMENT '编组名',
    cyl_type VARCHAR(8) NOT NULL COMMENT '气瓶规格',
    init_pressure DECIMAL(4, 1) NULL COMMENT '入场压力 MPa',
    current_pressure DECIMAL(4, 1) NULL COMMENT '当前压力 MPa',
    entered_at DATETIME NULL COMMENT '入场时间',
    withdrawn_at DATETIME NULL COMMENT '撤出时间，空表示未撤出',
    work_level VARCHAR(16) NULL COMMENT '作业强度',
    scene VARCHAR(64) NULL COMMENT '场景',
    status VARCHAR(16) NOT NULL COMMENT 'pending/in/warn/danger/out',
    remain_sec INT UNSIGNED NULL COMMENT '剩余秒',
    client_event_id VARCHAR(64) NULL COMMENT '客户端事件幂等键',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_attack_person_event (client_event_id),
    KEY idx_attack_person_station (station_id),
    KEY idx_attack_person_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内攻人员卡片';
