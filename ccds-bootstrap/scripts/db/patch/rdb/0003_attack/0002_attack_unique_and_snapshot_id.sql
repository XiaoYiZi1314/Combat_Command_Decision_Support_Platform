-- P3 审查修复：进行中唯一约束；快照补独立主键。不改已发布列语义。
-- 已撤出行 active_profile_id / active_temp_name 为 NULL，不占唯一槽。
ALTER TABLE ccds_attack_person
    ADD COLUMN active_profile_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (IF(withdrawn_at IS NULL, profile_id, NULL)) VIRTUAL,
    ADD COLUMN active_temp_name VARCHAR(32)
        GENERATED ALWAYS AS (IF(withdrawn_at IS NULL AND profile_id IS NULL, display_name, NULL)) VIRTUAL,
    ADD UNIQUE KEY uk_attack_person_active_profile (station_id, active_profile_id),
    ADD UNIQUE KEY uk_attack_person_active_temp (station_id, active_temp_name);

ALTER TABLE ccds_station_snapshot
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD UNIQUE KEY uk_station_snapshot_station (station_id);
