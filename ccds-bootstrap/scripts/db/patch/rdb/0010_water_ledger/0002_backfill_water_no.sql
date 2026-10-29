-- 历史水源补编号（幂等，可重复执行）。
-- 规则：已有 water_no 的行不动；其余按站内 id 升序编 S{stationId}-{四位序号}。
-- 编号一旦生成不得复用：删卡后编号进入 ccds_water_deleted，不参与重新编号。
-- 执行前置：0010_water_ledger/0001_water_ledger.sql 已加列。

UPDATE ccds_water_source w
JOIN (
    SELECT id,
           CONCAT('S', station_id, '-', LPAD(
               ROW_NUMBER() OVER (PARTITION BY station_id ORDER BY id), 4, '0')) AS new_no
    FROM ccds_water_source
    WHERE water_no IS NULL
) t ON t.id = w.id
SET w.water_no = t.new_no;

-- 校验：不应再有空编号
SELECT COUNT(*) AS `缺编号条数` FROM ccds_water_source WHERE water_no IS NULL;
