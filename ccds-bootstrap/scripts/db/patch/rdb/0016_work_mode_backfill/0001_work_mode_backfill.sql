-- 作业模式上线迁移（改法明细 2.2，对应验收「测试模式入场不上首页」）。
-- 目的：上线瞬间不能让正在内攻的站从指挥端消失。
-- 做法：把当前确有内攻中人员（in/warn/danger，不含预录入）的站一次性置为出警模式；其余站保持默认测试模式。
-- 幂等：重复执行结果一致，已有出警模式的站不会被改回。

INSERT INTO ccds_station_work_mode (station_id, work_mode)
SELECT DISTINCT p.station_id, 'dispatch'
FROM ccds_attack_person p
WHERE p.withdrawn_at IS NULL
  AND p.status IN ('in', 'warn', 'danger')
ON DUPLICATE KEY UPDATE
    work_mode = 'dispatch',
    gmt_modified = CURRENT_TIMESTAMP;

-- 校验：出警模式站数应与「有内攻中人员的站数」一致
SELECT
    (SELECT COUNT(*) FROM ccds_station_work_mode WHERE work_mode = 'dispatch') AS `出警模式站数`,
    (SELECT COUNT(DISTINCT station_id) FROM ccds_attack_person
      WHERE withdrawn_at IS NULL AND status IN ('in', 'warn', 'danger')) AS `有内攻人员站数`;
