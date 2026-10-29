-- 旧数据迁移核对脚本（只读，不修改任何数据）。
-- 用法：迁移完成后在同一个库执行，逐段核对；每段都有「期望」注释，与迁移报告对照。

-- ── 1. 水源档案 ────────────────────────────────────────────────
-- 期望：总数 643（旧版 8.76 内置检查表条数）；跳过的站名见迁移日志。
SELECT 'water_total' AS metric, COUNT(*) AS value FROM ccds_water_source WHERE deleted_at IS NULL;
SELECT 'water_type' AS metric, type AS dimension, COUNT(*) AS value
FROM ccds_water_source WHERE deleted_at IS NULL GROUP BY type ORDER BY value DESC;
SELECT 'water_status' AS metric, status AS dimension, COUNT(*) AS value
FROM ccds_water_source WHERE deleted_at IS NULL GROUP BY status;
SELECT 'water_by_station' AS metric, s.name AS dimension, COUNT(*) AS value
FROM ccds_water_source w JOIN ccds_station s ON s.id = w.station_id
WHERE w.deleted_at IS NULL GROUP BY s.name ORDER BY value DESC;

-- 与客户口径对照（客户 8.98 明细：水鹤 259/好用 248/不好用 11；消火栓 333/地上 326/地下 7/好用 290/不好用 43）
SELECT 'crane_total' AS metric, COUNT(*) AS actual, 259 AS customer FROM ccds_water_source
WHERE deleted_at IS NULL AND type = 'crane'
UNION ALL SELECT 'crane_active', COUNT(*), 248 FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'crane' AND status = 'active'
UNION ALL SELECT 'crane_repair', COUNT(*), 11 FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'crane' AND status = 'repair'
UNION ALL SELECT 'hydrant_total', COUNT(*), 333 FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground','underground')
UNION ALL SELECT 'hydrant_ground', COUNT(*), 326 FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'ground'
UNION ALL SELECT 'hydrant_underground', COUNT(*), 7 FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'underground'
UNION ALL SELECT 'hydrant_active', COUNT(*), 290 FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground','underground') AND status = 'active'
UNION ALL SELECT 'hydrant_repair', COUNT(*), 43 FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground','underground') AND status = 'repair';

-- 数据质量：extra_json 超长、经纬度缺失、同站同名（应恒为 0）
SELECT 'water_extra_too_long' AS metric, COUNT(*) AS value FROM ccds_water_source
WHERE deleted_at IS NULL AND CHAR_LENGTH(extra_json) > 2000;
SELECT 'water_no_latlng' AS metric, COUNT(*) AS value FROM ccds_water_source
WHERE deleted_at IS NULL AND (lng IS NULL OR lat IS NULL);
SELECT 'water_dup_name' AS metric, COUNT(*) AS value FROM (
    SELECT station_id, name FROM ccds_water_source WHERE deleted_at IS NULL GROUP BY station_id, name HAVING COUNT(*) > 1
) t;

-- ── 2. 花名册 ──────────────────────────────────────────────────
-- 期望：与旧版云端 persons/index.json 的 count 之和一致（各站见 out/cloud-station-map.json）
SELECT 'profile_total' AS metric, COUNT(*) AS value FROM ccds_profile WHERE deleted_at IS NULL;
SELECT 'profile_by_station' AS metric, s.name AS dimension, COUNT(*) AS value
FROM ccds_profile p JOIN ccds_station s ON s.id = p.station_id
WHERE p.deleted_at IS NULL GROUP BY s.name ORDER BY value DESC;
SELECT 'profile_no_phone' AS metric, COUNT(*) AS value FROM ccds_profile
WHERE deleted_at IS NULL AND (phone_cipher IS NULL OR phone_cipher = '');
SELECT 'profile_phone_not_encrypted' AS metric, COUNT(*) AS value FROM ccds_profile
WHERE phone_cipher IS NOT NULL AND phone_cipher NOT LIKE 'v1:%';
SELECT 'profile_dup_nfc' AS metric, COUNT(*) AS value FROM (
    SELECT station_id, nfc_tag FROM ccds_profile WHERE deleted_at IS NULL AND nfc_tag IS NOT NULL
    GROUP BY station_id, nfc_tag HAVING COUNT(*) > 1
) t;

-- ── 3. 编组 ────────────────────────────────────────────────────
-- 期望：与旧版云端 localGroupTasks 组名数一致；成员数 ≤ 花名册人数
SELECT 'group_total' AS metric, COUNT(*) AS value FROM ccds_battle_group;
SELECT 'group_member_total' AS metric, COUNT(*) AS value FROM ccds_battle_group_member;
SELECT 'group_by_station' AS metric, s.name AS dimension, COUNT(*) AS value
FROM ccds_battle_group g JOIN ccds_station s ON s.id = g.station_id GROUP BY s.name ORDER BY value DESC;

-- ── 4. 值班 ────────────────────────────────────────────────────
SELECT 'duty_total' AS metric, COUNT(*) AS value FROM ccds_duty_day;
SELECT 'duty_range' AS metric, MIN(duty_date) AS from_date, MAX(duty_date) AS to_date FROM ccds_duty_day;

-- ── 5. 空呼标定 ────────────────────────────────────────────────
SELECT 'calibration_total' AS metric, COUNT(*) AS value FROM ccds_scba_calibration;
SELECT 'calibration_by_station' AS metric, s.name AS dimension, COUNT(*) AS value
FROM ccds_scba_calibration c JOIN ccds_profile p ON p.id = c.profile_id
JOIN ccds_station s ON s.id = p.station_id GROUP BY s.name ORDER BY value DESC;

-- ── 6. 删除记录 / 阈值 / 作业模式 ──────────────────────────────
-- 期望：墓碑数 = 各站 deletedProfileKeys 去重后之和；阈值 23 站；作业模式 23 站
SELECT 'tombstone_total' AS metric, COUNT(*) AS value FROM ccds_profile_tombstone;
SELECT 'tombstone_by_source' AS metric, source AS dimension, COUNT(*) AS value
FROM ccds_profile_tombstone GROUP BY source;
SELECT 'tombstone_station_count' AS metric, COUNT(DISTINCT station_id) AS value FROM ccds_profile_tombstone;
SELECT 'scba_setting_station_count' AS metric, COUNT(*) AS value FROM ccds_station_scba_setting;
SELECT 'scba_setting_invalid' AS metric, COUNT(*) AS value FROM ccds_station_scba_setting
WHERE danger_pressure >= warn_pressure OR danger_time_min >= warn_time_min;
SELECT 'work_mode_station_count' AS metric, COUNT(*) AS value FROM ccds_station_work_mode;
SELECT 'work_mode_distribution' AS metric, work_mode AS dimension, COUNT(*) AS value
FROM ccds_station_work_mode GROUP BY work_mode;

-- ── 7. 附件（在 LegacyObjectImportService 执行后核对）────────────
SELECT 'file_total' AS metric, COUNT(*) AS value FROM ccds_file_object;
SELECT 'file_by_biz_type' AS metric, biz_type AS dimension, COUNT(*) AS value
FROM ccds_file_object GROUP BY biz_type ORDER BY value DESC;
SELECT 'keyunit_total' AS metric, COUNT(*) AS value FROM ccds_key_unit WHERE deleted_at IS NULL;

-- ── 8. 无法自动归位的旧资产（外部核对，不查库）─────────────────
-- 出水视频 1072 条 / 水源照片 376 条在旧版文件名中没有水源编号，无法自动挂到具体水源。
-- 盘点与按站分布见 `out/legacy-assets-summary.json` 与 `out/legacy-assets.ndjson`；
-- 1.6 的「上传出水视频数」在旧数据上只能按站给出近似值，需现场复核后回填。
SELECT 'legacy_unmapped_note' AS metric, 'video=1072 photo=376 未归位，见 legacy-assets-summary.json' AS value;
