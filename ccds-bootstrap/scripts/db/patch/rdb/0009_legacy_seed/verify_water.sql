-- 水源检查表迁移核对（只读查询，不改数据、不改结构）。
--
-- 用法：在已执行 0001_init/0002_seed_org_account.sql、
--       0009_legacy_seed/0000_landing_tables.sql、0009_legacy_seed/0002_water_fields.sql、
--       0009_legacy_seed/0001_water_source.sql 的库上依次执行本文件。
--       每段都带「期望值」注释；出现差异时按段定位，不要直接 UPDATE 订正，先确认再改。
--
-- 期望值来源：migration 脚本抽取的 out/water.ndjson（旧版 APK 内置清单 2026-06-09，643 条）。
--             重新抽取后，第 1/2/3/4/7 段的期望条数需要同步；第 9 段「客户」列是客户验收数，不随抽取变化。
-- 口径对应：旧版 usable=好用 → status=active + usable=good；usable=不好用 → status=repair + usable=bad。
-- 备注语义：extra_json 只存人类备注原文（旧版 notes），绝不是 JSON；Java 侧 WaterViewTranslator 直接把它当备注返回。
--           本文件第 6 段专门检测 JSON 回流。
-- 范围说明：水源档案 PDF（151 条）与检查照片由 Java 侧登记附件，不在 ccds_water_source 内，本文件不核对。
-- 兼容：只查 ccds_water_source / ccds_station 两张表，不依赖迁移落地表（0000_landing_tables.sql）。

-- ============================================================================
-- 1 总量：期望 643 条，且不应有软删
-- ============================================================================
SELECT COUNT(*) AS `本库条数`,
       643 AS `期望条数`,
       COUNT(*) - 643 AS `差异`,
       SUM(deleted_at IS NOT NULL) AS `已软删条数`
FROM ccds_water_source;

-- ============================================================================
-- 2 按类型：期望 crane 288 / ground 341 / underground 14
-- ============================================================================
SELECT t.`类型`,
       IFNULL(a.`本库`, 0) AS `本库`,
       t.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - t.`期望` AS `差异`
FROM (
    SELECT 'crane' AS `类型`, 288 AS `期望`
    UNION ALL SELECT 'ground', 341
    UNION ALL SELECT 'underground', 14
) t
LEFT JOIN (
    SELECT type AS `类型`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY type
) a ON a.`类型` = t.`类型`
ORDER BY t.`期望` DESC;

-- ============================================================================
-- 3 按状态：期望 active 529 / repair 114
-- ============================================================================
SELECT s.`状态`,
       IFNULL(a.`本库`, 0) AS `本库`,
       s.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - s.`期望` AS `差异`
FROM (
    SELECT 'active' AS `状态`, 529 AS `期望`
    UNION ALL SELECT 'repair', 114
) s
LEFT JOIN (
    SELECT status AS `状态`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY status
) a ON a.`状态` = s.`状态`
ORDER BY s.`期望` DESC;

-- ============================================================================
-- 4 每站条数：与 out/water.ndjson 逐站对照（按 ccds_station.sort_no 排序，共 23 行）
--   输出「差异」列非 0 的站即位点偏差，需要回看 build-water-sql.mjs 的跳过明细。
-- ============================================================================
SELECT s.id AS `站id`,
       s.name AS `站名`,
       IFNULL(w.`本库`, 0) AS `本库`,
       e.`期望` AS `期望`,
       IFNULL(w.`本库`, 0) - e.`期望` AS `差异`
FROM ccds_station s
LEFT JOIN (
    SELECT station_id, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY station_id
) w ON w.station_id = s.id
LEFT JOIN (
    SELECT '安顺路消防站' AS `站名`, 17 AS `期望`
    UNION ALL SELECT '海河路特勤站', 14
    UNION ALL SELECT '永安街消防站', 14
    UNION ALL SELECT '海山路消防站', 3
    UNION ALL SELECT '南树园消防站', 22
    UNION ALL SELECT '红旗路消防站', 48
    UNION ALL SELECT '华溪消防站', 18
    UNION ALL SELECT '通北路消防站', 12
    UNION ALL SELECT '光明路消防站', 15
    UNION ALL SELECT '宝石消防站', 6
    UNION ALL SELECT '新兴消防站', 15
    UNION ALL SELECT '胜利消防站', 34
    UNION ALL SELECT '向华路消防站', 36
    UNION ALL SELECT '新明路消防站', 91
    UNION ALL SELECT '学府路消防站', 38
    UNION ALL SELECT '建设路消防站', 11
    UNION ALL SELECT '奋斗街消防站', 20
    UNION ALL SELECT '府右街消防站', 59
    UNION ALL SELECT '金鼎消防站', 35
    UNION ALL SELECT '新和街消防站', 29
    UNION ALL SELECT '惠民街消防站', 51
    UNION ALL SELECT '南城路消防站', 55
    UNION ALL SELECT '战勤保障分队', 0
) e ON e.`站名` = s.name
ORDER BY s.sort_no;

-- ============================================================================
-- 5 撞名冲突检测
-- ============================================================================
-- 5.1 同站同名（uk_water_station_name 应已挡住，出现即异常）：期望 0 行
SELECT station_id AS `站id`, name AS `名称`, COUNT(*) AS `条数`
FROM ccds_water_source
GROUP BY station_id, name
HAVING COUNT(*) > 1;

-- 5.2 迁移脚本为避撞而加的后缀名 -2/-3…：期望 0 行
--     （旧版 643 条实测同站同名冲突 0 条，故正常迁移不会产生后缀名）
SELECT station_id AS `站id`, name AS `名称`
FROM ccds_water_source
WHERE name REGEXP '-[0-9]+$'
ORDER BY station_id, name;

-- 5.3 名称超长（VARCHAR(64)，按字符计）：期望 0 条
SELECT COUNT(*) AS `超64字符名称条数`
FROM ccds_water_source
WHERE CHAR_LENGTH(name) > 64;

-- 5.4 跨站同名（不同站允许同名，仅提示）：列出涉及站数最多的前 50 组
SELECT name AS `名称`, COUNT(DISTINCT station_id) AS `涉及站数`
FROM ccds_water_source
WHERE deleted_at IS NULL
GROUP BY name
HAVING COUNT(DISTINCT station_id) > 1
ORDER BY COUNT(DISTINCT station_id) DESC, name
LIMIT 50;

-- ============================================================================
-- 6 extra_json（人类备注）检查 —— 重点防 JSON 回流
-- ============================================================================
-- 6.1 extra_json 含 '}' 或 '{'：期望 0 条（本列只能放纯文本备注，写 JSON 会让前端「备注」显示 JSON）
SELECT COUNT(*) AS `备注含花括号条数`
FROM ccds_water_source
WHERE extra_json IS NOT NULL
  AND (extra_json LIKE '%{%' OR extra_json LIKE '%}%');

-- 6.2 extra_json 长度分布：迁移来源只有旧版 notes（实测 1 条「新建」，最长 2 字符）
--     若最长值突然变成几百字符，说明又有人把结构化内容写进备注列。
SELECT MAX(CHAR_LENGTH(extra_json)) AS `最长字符数`,
       ROUND(AVG(CHAR_LENGTH(extra_json)), 1) AS `平均字符数`,
       SUM(extra_json IS NULL) AS `空备注条数`
FROM ccds_water_source;

-- 6.3 备注非空条数：期望 1（旧版 notes 仅「新建」一条）
SELECT COUNT(*) AS `备注非空条数`
FROM ccds_water_source
WHERE extra_json IS NOT NULL
  AND extra_json <> '';

-- 6.4 备注是否与旧版一致（抽查非空的几条）
SELECT id AS `主键`, station_id AS `站id`, name AS `名称`, extra_json AS `备注`
FROM ccds_water_source
WHERE extra_json IS NOT NULL
ORDER BY id
LIMIT 10;

-- ============================================================================
-- 7 B1 结构化列核对（0002_water_fields.sql）
-- ============================================================================
-- 7.1 setup：期望 above 629 / under 14
SELECT s.`值`,
       IFNULL(a.`本库`, 0) AS `本库`,
       s.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - s.`期望` AS `差异`
FROM (
    SELECT 'above' AS `值`, 629 AS `期望`
    UNION ALL SELECT 'under', 14
) s
LEFT JOIN (
    SELECT setup AS `值`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY setup
) a ON a.`值` = s.`值`
ORDER BY s.`期望` DESC;

-- 7.2 supply_form：期望 ring 581 / branch 62 / NULL 0
SELECT t.`值`,
       IFNULL(a.`本库`, 0) AS `本库`,
       t.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - t.`期望` AS `差异`
FROM (
    SELECT 'ring' AS `值`, 581 AS `期望`
    UNION ALL SELECT 'branch', 62
    UNION ALL SELECT '<NULL>', 0
) t
LEFT JOIN (
    SELECT IFNULL(supply_form, '<NULL>') AS `值`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY IFNULL(supply_form, '<NULL>')
) a ON a.`值` = t.`值`
ORDER BY t.`期望` DESC;

-- 7.3 usable：期望 good 529 / bad 114
SELECT u.`值`,
       IFNULL(a.`本库`, 0) AS `本库`,
       u.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - u.`期望` AS `差异`
FROM (
    SELECT 'good' AS `值`, 529 AS `期望`
    UNION ALL SELECT 'bad', 114
) u
LEFT JOIN (
    SELECT usable AS `值`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY usable
) a ON a.`值` = u.`值`
ORDER BY u.`期望` DESC;

-- 7.4 is_repair：期望 1→114 / 0→85 / NULL→444
SELECT t.`值`,
       IFNULL(a.`本库`, 0) AS `本库`,
       t.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - t.`期望` AS `差异`
FROM (
    SELECT '1' AS `值`, 114 AS `期望`
    UNION ALL SELECT '0', 85
    UNION ALL SELECT '<NULL>', 444
) t
LEFT JOIN (
    SELECT IFNULL(CAST(is_repair AS CHAR), '<NULL>') AS `值`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
    GROUP BY IFNULL(CAST(is_repair AS CHAR), '<NULL>')
) a ON a.`值` = t.`值`
ORDER BY t.`期望` DESC;

-- 7.5 数值列：NULL 期望 pipe_diameter 0 / pressure_mpa 0 / flow_lps 91 / add_height_m 67
SELECT SUM(pipe_diameter IS NULL) AS `管径NULL`,
       SUM(pressure_mpa IS NULL) AS `压力NULL`,
       SUM(flow_lps IS NULL) AS `流量NULL`,
       SUM(add_height_m IS NULL) AS `高度NULL`,
       MIN(pipe_diameter) AS `最小管径`,
       MAX(pipe_diameter) AS `最大管径`,
       MIN(pressure_mpa) AS `最小压力`,
       MAX(pressure_mpa) AS `最大压力`,
       MIN(flow_lps) AS `最小流量`,
       MAX(flow_lps) AS `最大流量`,
       MIN(add_height_m) AS `最小高度`,
       MAX(add_height_m) AS `最大高度`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.5.1 高度为负（旧版原文 -3 / -2，共 14 条）：属旧数据录入问题，迁移按原值保留
SELECT COUNT(*) AS `负高度条数`
FROM ccds_water_source
WHERE deleted_at IS NULL
  AND add_height_m < 0;

-- 7.6 reason_text：期望非空 124 条（其中 usable=bad 114 条 + 好用但旧版填了原因 10 条）
SELECT SUM(reason_text IS NOT NULL) AS `原因原文非空条数`,
       SUM(reason_text IS NOT NULL AND usable = 'bad') AS `其中bad条数`,
       MAX(CHAR_LENGTH(reason_text)) AS `最长字符数`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.7 reason_code 分布：期望 no_water 34 / leak 1 / low_pressure 0 / rust 5 /
--     buried 0 / occupied 0 / diverted 0 / collapsed_well 0 / damaged 74 / NULL 529
SELECT t.`原因码`,
       IFNULL(a.`本库`, 0) AS `本库`,
       t.`期望` AS `期望`,
       IFNULL(a.`本库`, 0) - t.`期望` AS `差异`
FROM (
    SELECT 'no_water' AS `原因码`, 34 AS `期望`
    UNION ALL SELECT 'leak', 1
    UNION ALL SELECT 'low_pressure', 0
    UNION ALL SELECT 'rust', 5
    UNION ALL SELECT 'buried', 0
    UNION ALL SELECT 'occupied', 0
    UNION ALL SELECT 'diverted', 0
    UNION ALL SELECT 'collapsed_well', 0
    UNION ALL SELECT 'damaged', 74
) t
LEFT JOIN (
    SELECT reason_code AS `原因码`, COUNT(*) AS `本库`
    FROM ccds_water_source
    WHERE deleted_at IS NULL
      AND reason_code IS NOT NULL
    GROUP BY reason_code
) a ON a.`原因码` = t.`原因码`
ORDER BY t.`期望` DESC;

-- 7.7.1 reason_code 合计：期望非空 114（= usable=bad 条数）、NULL 529
SELECT SUM(reason_code IS NOT NULL) AS `原因码非空条数`,
       SUM(reason_code IS NULL) AS `原因码空条数`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.8 原因码与可用性一致性：期望三项均为 0
SELECT SUM(usable = 'good' AND reason_code IS NOT NULL) AS `好用却有原因码`,
       SUM(usable = 'bad' AND reason_code IS NULL) AS `不好用却无原因码`,
       SUM(reason_code IS NOT NULL AND usable IS NULL) AS `缺usable却有原因码`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.9 part_code：旧数据没有部件信息，期望全 NULL
SELECT SUM(part_code IS NOT NULL) AS `非空部件码条数`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.10 good/bad 与 active/repair 交叉验证：期望四项均为 0
SELECT SUM(usable = 'good' AND status <> 'active') AS `good但非active`,
       SUM(usable = 'bad' AND status <> 'repair') AS `bad但非repair`,
       SUM(type = 'underground' AND setup <> 'under') AS `地下类型但setup非under`,
       SUM(type <> 'underground' AND setup = 'under') AS `setup为under但类型非地下`
FROM ccds_water_source
WHERE deleted_at IS NULL;

-- 7.11 原因码抽查（每个原因码各取 2 条，便于人工确认归类是否符合客户口径）
SELECT reason_code AS `原因码`, reason_text AS `原因原文`, name AS `名称`
FROM ccds_water_source
WHERE deleted_at IS NULL
  AND reason_code IS NOT NULL
ORDER BY reason_code, id
LIMIT 40;

-- ============================================================================
-- 8 坐标缺失与范围检查
-- ============================================================================
-- 8.1 缺坐标：期望 109 条（旧版 108 条空串 + 1 条 "123.10 30" 非数值）
SELECT COUNT(*) AS `缺坐标条数`,
       SUM(lng IS NULL AND lat IS NOT NULL) AS `仅缺经度`,
       SUM(lat IS NULL AND lng IS NOT NULL) AS `仅缺纬度`
FROM ccds_water_source
WHERE deleted_at IS NULL
  AND (lng IS NULL OR lat IS NULL);

-- 8.2 缺坐标按站分布（便于让客户按站补录）
SELECT s.name AS `站名`, COUNT(*) AS `缺坐标条数`
FROM ccds_water_source w
JOIN ccds_station s ON s.id = w.station_id
WHERE w.deleted_at IS NULL
  AND (w.lng IS NULL OR w.lat IS NULL)
GROUP BY s.name
ORDER BY COUNT(*) DESC;

-- 8.3 坐标超出齐齐哈尔合理范围（约 lng 121.5–126.5 / lat 45.5–49.0）：期望 3 条已知异常
--     municipal_xinming_crane_024 / ground_056 / ground_070 属旧版录入问题，迁移按原值保留。
SELECT id AS `主键`,
       station_id AS `站id`,
       name AS `名称`,
       lng AS `经度`,
       lat AS `纬度`,
       address AS `地址`
FROM ccds_water_source
WHERE deleted_at IS NULL
  AND lng IS NOT NULL
  AND (lng < 121.5 OR lng > 126.5 OR lat < 45.5 OR lat > 49.0)
ORDER BY id;

-- 8.4 地址为空：期望 0 条
SELECT COUNT(*) AS `地址为空条数`
FROM ccds_water_source
WHERE deleted_at IS NULL
  AND address IS NULL;

-- 8.5 地址超长（VARCHAR(128)）：期望 0 条
SELECT COUNT(*) AS `超128字符地址条数`
FROM ccds_water_source
WHERE address IS NOT NULL
  AND CHAR_LENGTH(address) > 128;

-- ============================================================================
-- 9 客户验收数对照（一个 SELECT 同时输出「本库 vs 客户」与差异）
--    客户口径：水鹤 259（好用 248 / 不好用 11）；消火栓 333（地上 326 / 地下 7 / 好用 290 / 不好用 43）
--    本库口径：好用 = usable 'good'（等价 status 'active'），消火栓 = ground + underground
--    客户合计按 259 + 333 = 592 计，用于暴露两份数据集的整体差量。
-- ============================================================================
SELECT t.`序号`,
       t.`指标`,
       t.`本库`,
       t.`客户`,
       t.`本库` - t.`客户` AS `差异`
FROM (
    SELECT 1 AS `序号`, '消防水鹤·总' AS `指标`,
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'crane') AS `本库`,
           259 AS `客户`
    UNION ALL
    SELECT 2, '消防水鹤·好用',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'crane' AND usable = 'good'),
           248
    UNION ALL
    SELECT 3, '消防水鹤·不好用',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'crane' AND usable = 'bad'),
           11
    UNION ALL
    SELECT 4, '消火栓·总（地上+地下）',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground', 'underground')),
           333
    UNION ALL
    SELECT 5, '消火栓·地上',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'ground'),
           326
    UNION ALL
    SELECT 6, '消火栓·地下',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type = 'underground'),
           7
    UNION ALL
    SELECT 7, '消火栓·好用',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground', 'underground') AND usable = 'good'),
           290
    UNION ALL
    SELECT 8, '消火栓·不好用',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('ground', 'underground') AND usable = 'bad'),
           43
    UNION ALL
    SELECT 9, '合计（水鹤+消火栓）',
           (SELECT COUNT(*) FROM ccds_water_source WHERE deleted_at IS NULL AND type IN ('crane', 'ground', 'underground')),
           592
) t
ORDER BY t.`序号`;

-- ============================================================================
-- 10 迁移批次健康度（按导入日期看是否单批完成）
-- ============================================================================
SELECT DATE(gmt_create) AS `导入日期`,
       COUNT(*) AS `条数`,
       MIN(gmt_create) AS `最早`,
       MAX(gmt_create) AS `最晚`
FROM ccds_water_source
GROUP BY DATE(gmt_create)
ORDER BY `导入日期`;

-- 跳过的记录不会入库（生成脚本已跳过并打印明细），这里反向确认缺口：
-- 若本值 > 0，回看 build-water-sql.mjs 控制台的「跳过明细」段。
SELECT 643 - COUNT(*) AS `相对NDJSON缺口`
FROM ccds_water_source;
