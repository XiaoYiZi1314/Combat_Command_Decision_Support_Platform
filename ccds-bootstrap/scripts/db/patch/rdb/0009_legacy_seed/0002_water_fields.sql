-- 水源档案结构化字段（旧数据迁移 B1 批次）。只 ADD COLUMN / ADD KEY，不改任何已发布 DDL。
-- 背景：0004_water/0001_water.sql 的 ccds_water_source 只有 name/type/status/address/lng/lat/extra_json，
--       旧版检查表的 setup/管径/压力/流量/给水形式/加水高度/可用性/原因等结构化信息无处安放。
-- 说明：extra_json 保持「人类备注纯文本」语义（Java 侧 WaterViewTranslator.notes 直接读它），
--       本批新增列承载结构化字段，禁止再把 JSON 写入 extra_json。
-- 执行顺序：0000_landing_tables.sql → 本文件 → 0001_water_source.sql（本文件必须先于补丁数据执行）。
-- 幂等：MySQL 不支持 ADD COLUMN IF NOT EXISTS，本文件只执行一次；重复执行会报 Duplicate column，属预期。

ALTER TABLE ccds_water_source
    ADD COLUMN setup VARCHAR(8) NULL COMMENT '设置方式 above/under',
    ADD COLUMN pipe_diameter INT UNSIGNED NULL COMMENT '管网直径 mm',
    ADD COLUMN pressure_mpa DECIMAL(5, 3) NULL COMMENT '管网压力 MPa',
    ADD COLUMN flow_lps DECIMAL(6, 2) NULL COMMENT '管网流量 L/s',
    ADD COLUMN supply_form VARCHAR(8) NULL COMMENT '给水形式 ring/branch',
    ADD COLUMN add_height_m DECIMAL(5, 2) NULL COMMENT '加水高度 m',
    ADD COLUMN usable VARCHAR(8) NULL COMMENT '可使用 good/bad',
    ADD COLUMN reason_text VARCHAR(255) NULL COMMENT '无法出水原因原文',
    ADD COLUMN reason_code VARCHAR(16) NULL COMMENT '九原因码',
    ADD COLUMN part_code VARCHAR(16) NULL COMMENT '八部件码',
    ADD COLUMN is_repair TINYINT(1) NULL COMMENT '是否报修 1是0否',
    ADD KEY idx_water_bad (station_id, status, usable);
