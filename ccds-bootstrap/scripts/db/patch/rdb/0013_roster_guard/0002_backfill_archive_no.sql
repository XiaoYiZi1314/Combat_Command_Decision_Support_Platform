-- 历史人员补档案号（幂等，可重复执行）。
-- 背景：0001_roster.sql 只有自增 id，没有档案号；4.1 要求跨站调动必须换新档案号，
--       既有人员在调动前也应有一份对外可读的档案号，否则界面「档案号」为空。
-- 规则：已有 archive_no 的行不动；其余按主键补 P + 8 位序号，与 Java 侧 buildArchiveNo 一致。
-- 幂等：只填空值，重复执行不会改已分配的号。

UPDATE ccds_profile
SET archive_no = CONCAT('P', LPAD(id, 8, '0'))
WHERE archive_no IS NULL;

-- 校验：既有在册人员不应再有空档案号
SELECT COUNT(*) AS `缺档案号人数` FROM ccds_profile WHERE archive_no IS NULL;
