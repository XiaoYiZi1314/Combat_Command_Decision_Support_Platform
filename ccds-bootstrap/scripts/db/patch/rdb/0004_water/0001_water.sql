-- P6 水源档案。只建新表，不改已发布 DDL。
CREATE TABLE IF NOT EXISTS ccds_water_source (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    station_id BIGINT UNSIGNED NOT NULL COMMENT '所属站',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    type VARCHAR(16) NOT NULL COMMENT 'crane/ground/underground',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/repair',
    address VARCHAR(128) NULL COMMENT '地址',
    lng DECIMAL(10, 6) NULL COMMENT '经度 WGS84',
    lat DECIMAL(10, 6) NULL COMMENT '纬度 WGS84',
    extra_json VARCHAR(2000) NULL COMMENT '备注与扩展',
    deleted_at DATETIME NULL COMMENT '软删时间',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_water_station_name (station_id, name),
    KEY idx_water_station (station_id),
    KEY idx_water_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水源档案';
