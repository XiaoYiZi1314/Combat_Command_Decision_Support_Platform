-- 值班表补车辆快照（改法明细 4.4）。只 ADD COLUMN，不改已发布 DDL。
-- 语义：vehicle_snapshot_json 记当天的车辆快照，与编组快照一起保存；
--       只有今天与明天允许改编组与车辆，历史日期保存时保持原快照不动。

ALTER TABLE ccds_duty_day
    ADD COLUMN vehicle_snapshot_json VARCHAR(2000) NULL COMMENT '当日车辆快照JSON';
