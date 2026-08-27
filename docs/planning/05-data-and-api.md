# 表与 API 草案

表前缀 `ccds_`，字段 `snake_case`。下列是规划草案，建表脚本等开工后按 harness/06 落在 bootstrap，不在本期改已发布 DDL（当前仓库还没有业务表）。

## 1. 表

### 组织与账号

**ccds_brigade**  
`id, code, name, created_at`  
种子 16 行。禁止业务删除。

**ccds_station**  
`id, brigade_id null, name, name_norm, category, sort_no, created_at`  
`brigade_id` 空 = 支队直属（战勤保障分队）。

**ccds_account**  
`id, username, password_hash, role, station_id, brigade_id, must_change_password, locked_until, created_at`  
`role`: `station | brigade | hq | developer`

**ccds_account_session**  
`id, account_id, token_hash, expire_at, device_hint`

### 花名册

**ccds_profile**  
`id, station_id, name, title, rank_name, phone, cyl_type, nfc_tag, height_cm, weight_kg, age, morning_pressure, extra_json, updated_at, deleted_at`  
唯一：`(station_id, name)` 未删除；`(station_id, nfc_tag)` 在 tag 非空时唯一。

**ccds_battle_group**  
`id, station_id, name, sort_no, updated_at`

**ccds_battle_group_member**  
`group_id, profile_id, role_in_group`

**ccds_scba_calibration**  
`id, profile_id, pressure, full_time_sec, cyl_type, source, measured_at`

### 内攻

**ccds_attack_person**  
`id, station_id, profile_id null, display_name, group_name, cyl_type, init_pressure, current_pressure, entered_at, withdrawn_at, work_level, scene, status, remain_sec, client_event_id, updated_at`  
`status`: `pending | in | warn | danger | out`  
进行中唯一：同一 `station_id + profile_id`（或临时人名）且 `withdrawn_at` 空。

**ccds_attack_event**  
`id, station_id, person_id, type, payload_json, client_event_id, created_at`  
`type`: `pre_add | enter | remeasure | withdraw | delete`  
`client_event_id` 唯一。

**ccds_station_snapshot**  
`station_id, in_count, warn_count, danger_count, out_count, group_count, last_event_at, payload_json`  
指挥首页读这张，避免每次聚合卡片。

### 值班 / 水源 / 预案 / 共享

**ccds_duty_day**  
`id, station_id, duty_date, main_officer_id, deputy_officer_id, note, group_snapshot_json, updated_at`  
唯一 `(station_id, duty_date)`。

**ccds_water_source**  
`id, station_id, name, type, status, address, lng, lat, extra_json, updated_at, deleted_at`  
`type`: `crane | ground | underground`  
`status`: `active | repair`

**ccds_file_object**  
`id, biz_type, biz_id, cos_key, content_type, size_bytes, created_by, created_at`  
`biz_type`: `water_photo | water_doc | keyunit_plan | keyunit_floor | hazmat_image`

**ccds_key_unit**  
`id, station_id, name, address, category, contact, phone, notes, plan_text, updated_at, deleted_at`

**ccds_share_token**  
`id, station_id, token_hash, expire_at, revoked_at, created_by`

**ccds_offline_inbox**（可选，若只用 Redis 流可不上表）  
补传去重以 `client_event_id` 为准。

## 2. API（均需登录，除标注）

前缀 `/api/v1`。写接口带幂等键（内攻用 `eventId`）。

### IAM / 组织

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 账号口令 → token + 是否改密 |
| POST | `/auth/password` | 改密 |
| POST | `/auth/logout` | |
| GET | `/me` | 角色、可见站 |
| GET | `/org/stations` | 可见编制树 |

### 花名册

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/stations/{id}/profiles` | |
| PUT | `/stations/{id}/profiles/{pid}` | 站级 |
| POST | `/stations/{id}/profiles` | |
| DELETE | `/stations/{id}/profiles/{pid}` | 软删 |
| GET/PUT | `/stations/{id}/groups` | 编组整存或分项，开工时选一种 |
| POST | `/stations/{id}/profiles/import` | Excel |

### 内攻

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/stations/{id}/attack` | 本站卡片 + 快照 |
| POST | `/stations/{id}/attack/events` | 预录入/入场/复测/撤出/删除 |
| GET | `/attack/snapshots` | 指挥端；仅可见且 `in+warn+danger>0` 的站 |
| WS | `/ws/command` | 快照推送 |

### 辅助

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/PUT | `/stations/{id}/duty/{date}` | |
| GET/POST/PUT | `/stations/{id}/waters` | |
| GET | `/waters/nearby?lng&lat` | 全市在用；默认半径 3 km、最多 30 条 |
| GET/POST/PUT | `/stations/{id}/key-units` | |
| POST | `/files/presign` | 上传前拿 COS PUT |
| POST | `/share` | 开令牌，回绝对 URL |
| DELETE | `/share/{id}` | 作废 |
| GET | `/s/{token}` | **匿名** 只读页/JSON |
| GET | `/weather?lng&lat` | 后端代理 |
| GET | `/hazmat/search?q=` | |
| POST | `/assist/hazmat-vision` | 图+文 |
| POST | `/assist/attack-advice` | 研判 |
| POST | `/assist/supply-calc` | 供水计算（也可纯前端算，AI 走此） |
| GET | `/maps/baidu-token` | 浏览器端临时凭证 |

## 3. 权限要点

- 所有 `{id}` 先鉴权再查：站级 `id` 必须等于本站。
- 大队：`station.brigade_id` 必须等于本账号大队。
- 匿名 `/s/{token}` 只读投影 DTO，不含 phone/nfc/account。
- 禁止 `findAll` 无分页拉全市水源详情；附近查询带半径与 limit。

## 4. 种子账号策略

初始化脚本创建：

- 支队 `qqhrzd`（沿用现网用户名，口令不沿用明文规则）
- 开发者 `kaifazhe`
- 16 个大队账号（现网 `lsdd` 等）
- 每站一个站级账号

全部 `must_change_password=1`。口令用一次性清单交给你，不进仓库、不进 APK。
