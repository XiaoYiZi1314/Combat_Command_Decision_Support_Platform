-- 历史站点补固定编制卡（幂等，可重复执行）。
-- 背景：4.3 要求每站保留指挥员、安全员、通信员三张固定编制卡，作战组不得占用这三张卡的固定编号。
-- 注意：站内可能已存在同名的普通编组（旧数据导入产生），直接 INSERT 会撞
--       uk_battle_group_station_name。因此先「认领同名组」再补缺失的。
-- 幂等：UPDATE 只改未标记的行；INSERT 用 NOT EXISTS 判重。

-- 第一步：已有同名组的，直接标成固定卡并落槽位
UPDATE ccds_battle_group SET slot_no = 1, is_fixed = 1
WHERE slot_no IS NULL AND name = '指挥员';
UPDATE ccds_battle_group SET slot_no = 2, is_fixed = 1
WHERE slot_no IS NULL AND name = '安全员';
UPDATE ccds_battle_group SET slot_no = 3, is_fixed = 1
WHERE slot_no IS NULL AND name = '通信员';

-- 第二步：按槽位补齐缺失的固定卡（槽位已占用则跳过，避免重复）
INSERT INTO ccds_battle_group (station_id, name, sort_no, slot_no, is_fixed)
SELECT s.id, '指挥员',
       COALESCE((SELECT MAX(g2.sort_no) FROM ccds_battle_group g2 WHERE g2.station_id = s.id), 0) + 1,
       1, 1
FROM ccds_station s
WHERE NOT EXISTS (
    SELECT 1 FROM ccds_battle_group g WHERE g.station_id = s.id AND g.slot_no = 1
);

INSERT INTO ccds_battle_group (station_id, name, sort_no, slot_no, is_fixed)
SELECT s.id, '安全员',
       COALESCE((SELECT MAX(g2.sort_no) FROM ccds_battle_group g2 WHERE g2.station_id = s.id), 0) + 1,
       2, 1
FROM ccds_station s
WHERE NOT EXISTS (
    SELECT 1 FROM ccds_battle_group g WHERE g.station_id = s.id AND g.slot_no = 2
);

INSERT INTO ccds_battle_group (station_id, name, sort_no, slot_no, is_fixed)
SELECT s.id, '通信员',
       COALESCE((SELECT MAX(g2.sort_no) FROM ccds_battle_group g2 WHERE g2.station_id = s.id), 0) + 1,
       3, 1
FROM ccds_station s
WHERE NOT EXISTS (
    SELECT 1 FROM ccds_battle_group g WHERE g.station_id = s.id AND g.slot_no = 3
);

-- 校验：固定卡应为「站点数 × 3」
SELECT (SELECT COUNT(*) FROM ccds_station) AS `站点数`,
       (SELECT COUNT(*) FROM ccds_battle_group WHERE is_fixed = 1) AS `固定卡数`;
