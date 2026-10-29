-- 取消首次登录强制改密（改法明细 5.1）。
-- 只做数据 patch，不改表结构。后端 MustChangePasswordInterceptor 与改密接口保留，
-- 用于管理员重置后的兜底；站点账号日常登录不再被强制改密。

UPDATE ccds_account
SET is_must_change_password = 0
WHERE is_must_change_password = 1;

-- 校验：不应再有强制改密账号
SELECT COUNT(*) AS `仍强制改密账号数` FROM ccds_account WHERE is_must_change_password = 1;
