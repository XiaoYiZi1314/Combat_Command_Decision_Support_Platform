-- 车辆档案补流量列（改法明细 1.9：供水计算改用车辆档案）。
-- 只 ADD COLUMN，不改任何已发布 DDL、不删列。
-- 语义：pump_flow 水泵流量 L/s；water_cannon_flow 水炮流量 L/s；foam_cannon_flow 泡沫炮流量 L/s。
-- 供水计算取流量的优先级：水泵流量 → 水炮流量 → 泡沫炮流量 → 按类型估算。

ALTER TABLE ccds_vehicle
    ADD COLUMN pump_flow DECIMAL(6, 2) NULL COMMENT '水泵流量 L/s',
    ADD COLUMN water_cannon_flow DECIMAL(6, 2) NULL COMMENT '水炮流量 L/s',
    ADD COLUMN foam_cannon_flow DECIMAL(6, 2) NULL COMMENT '泡沫炮流量 L/s';
