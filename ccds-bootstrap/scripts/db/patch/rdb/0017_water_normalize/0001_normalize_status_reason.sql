-- 存量水源数据按 1.3 口径补原因码与部件码（幂等，可重复执行）。
--
-- 设计取舍（重要）：
--   1.2 的「不好用」是读时谓词：bad := status='repair' OR usable='bad' OR is_repair=1。
--   界面筛选、卡片颜色、汇总与导出都调这个谓词，因此**不依赖也不改写 status 列**。
--   存量数据里存在「在用 + 可使用=不好用」这类组合，直接改写 status 会改变用户可见状态，
--   属于业务决策而非数据修正，故本补丁**只做加法**：
--     · 只补 reason_code / part_code 的空值；
--     · 不修改 status / usable / is_repair；
--     · 不清空任何原因原文，避免丢失现场录入内容。
--
-- 判定顺序与 WaterReasonEnum / WaterPartEnum 声明顺序一致，先命中先得，兜底毁坏 / 闸阀。

-- ---------------------------------------------------------------------------
-- 一、原因码补空（只针对谓词判定为「不好用」的卡）
-- ---------------------------------------------------------------------------
UPDATE ccds_water_source SET reason_code = 'no_water'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%无水%' OR reason_text LIKE '%没水%'
       OR reason_text LIKE '%管网内没%' OR reason_text LIKE '%管道无水%' OR reason_text LIKE '%管网无%');

UPDATE ccds_water_source SET reason_code = 'leak'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%漏%' OR reason_text LIKE '%锁不住水%');

UPDATE ccds_water_source SET reason_code = 'low_pressure'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND reason_text LIKE '%压力%';

UPDATE ccds_water_source SET reason_code = 'rust'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%锈%' OR reason_text LIKE '%绣%');

UPDATE ccds_water_source SET reason_code = 'buried'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%埋压%' OR reason_text LIKE '%被埋%');

UPDATE ccds_water_source SET reason_code = 'occupied'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%圈占%' OR reason_text LIKE '%围挡%');

UPDATE ccds_water_source SET reason_code = 'diverted'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND reason_text LIKE '%挪用%';

UPDATE ccds_water_source SET reason_code = 'collapsed_well'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1)
  AND (reason_text LIKE '%塌井%' OR reason_text LIKE '%井塌%');

-- 兜底：剩余不好用卡一律归毁坏
UPDATE ccds_water_source SET reason_code = 'damaged'
WHERE reason_code IS NULL AND (status = 'repair' OR usable = 'bad' OR is_repair = 1);

-- ---------------------------------------------------------------------------
-- 二、部件码补空（顺序与 WaterPartEnum 一致，兜底闸阀）
-- ---------------------------------------------------------------------------
UPDATE ccds_water_source SET part_code = 'cover'
WHERE part_code IS NULL AND reason_code IS NOT NULL AND reason_text LIKE '%井盖%';
UPDATE ccds_water_source SET part_code = 'blind_cover'
WHERE part_code IS NULL AND reason_code IS NOT NULL AND reason_text LIKE '%闷盖%';
UPDATE ccds_water_source SET part_code = 'drain_valve'
WHERE part_code IS NULL AND reason_code IS NOT NULL AND reason_text LIKE '%放水阀%';
UPDATE ccds_water_source SET part_code = 'inlet_valve'
WHERE part_code IS NULL AND reason_code IS NOT NULL
  AND (reason_text LIKE '%上水阀%' OR reason_text LIKE '%开水阀%');
UPDATE ccds_water_source SET part_code = 'sign'
WHERE part_code IS NULL AND reason_code IS NOT NULL AND reason_text LIKE '%标志牌%';
UPDATE ccds_water_source SET part_code = 'outlet'
WHERE part_code IS NULL AND reason_code IS NOT NULL AND reason_text LIKE '%出水口%';
UPDATE ccds_water_source SET part_code = 'valve'
WHERE part_code IS NULL AND reason_code IS NOT NULL
  AND (reason_text LIKE '%闸阀%' OR reason_text LIKE '%阀门%' OR reason_text LIKE '%锁芯%'
       OR reason_text LIKE '%扳手%' OR reason_text LIKE '%转换器%' OR reason_text LIKE '%开关%');
UPDATE ccds_water_source SET part_code = 'pipe'
WHERE part_code IS NULL AND reason_code IS NOT NULL
  AND (reason_text LIKE '%管网%' OR reason_text LIKE '%管道%' OR reason_text LIKE '%水管%');
UPDATE ccds_water_source SET part_code = 'valve'
WHERE part_code IS NULL AND reason_code IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 三、核对：不好用卡必须都有原因码；status 一列不得被本补丁改动
-- ---------------------------------------------------------------------------
SELECT
    COUNT(*) AS `水源总数`,
    SUM(status = 'active') AS `在用`,
    SUM(status = 'repair') AS `报修`,
    SUM(status = 'repair' OR usable = 'bad' OR is_repair = 1) AS `不好用数`,
    SUM((status = 'repair' OR usable = 'bad' OR is_repair = 1) AND reason_code IS NULL) AS `坏卡缺原因码_应为0`,
    SUM((status = 'repair' OR usable = 'bad' OR is_repair = 1) AND part_code IS NULL) AS `坏卡缺部件码_应为0`
FROM ccds_water_source
WHERE deleted_at IS NULL;
