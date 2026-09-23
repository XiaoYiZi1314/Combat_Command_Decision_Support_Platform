-- P7 空呼运行态：压力锚点、暂停累计与状态迁移。保留旧列，避免改写已发布 DDL。
ALTER TABLE ccds_attack_person
    ADD COLUMN manual_pressure_at DATETIME NULL COMMENT '最近确认压力锚点时间' AFTER current_pressure,
    ADD COLUMN timer_paused_at DATETIME NULL COMMENT '暂停开始时间' AFTER withdrawn_at,
    ADD COLUMN paused_seconds INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计暂停秒数' AFTER timer_paused_at,
    ADD COLUMN pressure_paused_seconds INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最近压力锚点后的暂停秒数' AFTER paused_seconds,
    ADD COLUMN remain_anchor_sec INT UNSIGNED NULL COMMENT '运行时剩余时间锚点秒数' AFTER remain_sec;

-- warn/danger 是展示风险态，不再作为持久化状态；历史行归并为 in。
UPDATE ccds_attack_person
SET status = 'in'
WHERE status IN ('warn', 'danger');
