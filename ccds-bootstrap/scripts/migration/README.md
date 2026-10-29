# 旧系统数据迁移工具（一次性）

本目录把客户旧版 APP（作战指挥辅助决策平台 7.84/8.x）留下的数据迁移到本仓库 CCDS 表结构。
数据来源只有两处，全部为只读：

1. **旧版 APK 内置资源**：`tmp_apk/extracted/assets/`（`water_inspection_manifest.js`、`water_inspection_photos.js`、`water_sources_manifest.js`、`index.html` 内嵌 `fireStationData` / `IMPORTED_STATION_DUTY_VEHICLE_DATA`）。
2. **Cloudflare R2**（`nxgk-files` 桶）：旧版云端同步的 `nxgk/cloud/nxgk_inner_attack_cloud_v1/station_<base64站名>/` 下的花名册、值班、调档、空呼标定、阈值、作业模式，以及 `nxgk/<站>/keyunit|water-photo|water-video|...`、`nxgk/water_sources_pdf/`、`nxgk/preplans/migrated/` 下的附件。

**凭证不落仓库。** `r2.mjs` 依次尝试：环境变量 `R2_AK`/`R2_SK` → 旧版 `assets/cloud_config.js`（该目录被 `.gitignore` 排除）。

## 1. 目录与产物

```
ccds-bootstrap/scripts/migration/
  r2.mjs                     S3 兼容只读客户端（ListObjectsV2 / GetObject，SigV4）
  extract-bundle-water.mjs   内置水源检查表 + 照片 + 水源档案 PDF 清单 → NDJSON
  build-water-sql.mjs        NDJSON → 水源 SQL 补丁
  extract-cloud.mjs          R2 云端 → 花名册/值班/编组/标定/阈值/模式 NDJSON
  build-file-manifest.mjs    R2 附件清单 → LegacyObjectImportService 可消费的 manifest
  extract-bundle-core.mjs    index.html 内置 站级种子（花名册/编组/值班车辆）→ JSON
  README.md

ccds-bootstrap/scripts/db/patch/rdb/0009_legacy_seed/
  0000_landing_tables.sql    迁移落地表（人员删除记录、站级阈值、作业模式）
  0001_water_source.sql      水源检查表 643 条（由 build-water-sql.mjs 生成）
  verify.sql                 迁移核对 SQL
```

所有中间产物写 `LEGACY_MIGRATION_OUT`（默认 `%TEMP%\ccds-migration`），不写入仓库。

## 2. 统一约定

- 运行：`node <脚本>.mjs`，Node ≥ 20。
- 编码：NDJSON 一律 UTF-8、每行一个 JSON 对象、不含 BOM。
- 站名：直接使用旧版站名（如「华溪消防站」「战勤保障分队」），由 Java 侧按 `ccds_station.name / name_norm / ccds_station_alias` 归一，脚本不做猜测。
- 时间：毫秒时间戳原样保留（字段名以 `At` 结尾），Java 侧转 `DATETIME`。
- 幂等：所有脚本可重复运行；SQL 补丁用 `INSERT ... ON DUPLICATE KEY UPDATE`/`WHERE NOT EXISTS`，Java 导入按业务唯一键 upsert。
- 敏感数据：电话只在 NDJSON 内存流转（`phone`），绝不写入 SQL；落库由 Java 侧 `FieldCipherUtil` 加密。

## 3. NDJSON 契约

### 3.1 `out/water.ndjson` — 一条水源检查记录一行

```json
{"legacyId":"municipal_001","station":"安顺路消防站","name":"市政水鹤 1","type":"消防水鹤",
 "setup":"地上","address":"…","pipeDiameter":"200","pressure":"0.35","flow":"40",
 "waterSupply":"环状","height":"3.8","status":"在用","usable":"好用","reason":"","repair":"",
 "notes":"","photoRef":"\"ID_BBA2…\",1","lng":"123.955079","lat":"47.307292",
 "updatedAt":0,"locationAccuracy":"precise","photoLocation":"…","source":"municipal-check-20260609"}
```

### 3.2 `out/water-docs.ndjson` — 水源档案 PDF（来自 `water_sources_manifest.js`）

```json
{"station":"安顺路消防站","waterName":"安顺路救援站地上消火栓（4个）","bizType":"water_doc",
 "r2Key":"nxgk/water_sources_pdf/…/….pdf","originalName":"….pdf","contentType":"application/pdf","sizeBytes":979872}
```

### 3.3 `out/water-photos/` — 内置照片解出为文件

`water_inspection_photos.js` 里 `ID_xxx → data:image/jpeg;base64,…`，按 `ID` 命名为 `<ID>.jpg`；
`out/water-photo-index.ndjson` 记录 `{"id":"ID_xxx","file":"water-photos/ID_xxx.jpg","bytes":…,"sha256":"…"}`。

### 3.4 `out/roster.ndjson` — 一个消防站一行

```json
{"station":"红旗路消防站","stationKey":"station_57qi…","source":"cloud","indexUpdatedAt":1792452482930,
 "profiles":[{"legacyId":"p_1","name":"王永涛","role":"站长","rank":"一级指挥员","phone":"",
   "cylType":"6.8","height":170,"weight":64,"age":39,"nfcTag":"53CEAE02920001","morningPressure":28}],
 "deletedKeys":["id:p_3","nfc:530B2455300001"],
 "groups":[{"name":"灭火救援1组","sortNo":2,"members":[{"legacyId":"p_1","roleInGroup":"组长"}]}],
 "calibrations":[{"legacyId":"p_1","pressure":28,"fullTimeSec":1216,"cylType":"6.8",
   "source":"高强度实测","measuredAt":1783494340918}],
 "duty":[{"date":"2026-07-01","mainOfficerName":"李根","deputyOfficerName":"宋杰","note":""}],
 "transfers":[{"fromStation":"红旗路消防站","toStation":"光明路消防站","sourceProfileId":"p_3",
   "name":"宋杰","nfcTag":"530B2455300001","status":"approved","createdAt":1786973336216,
   "reviewedAt":1786973356255,"reviewer":"支队","newProfileId":"p_xfer_3_msx9ssdl"}],
 "settings":{"warnPressure":10,"dangerPressure":4,"warnTimeMin":12,"dangerTimeMin":5,
   "workLevel":"moderate","scene":"flat","voiceEnabled":true},
 "operationMode":"dispatch",
 "groupTasks":[{"name":"灭火救援1组","task":"","location":""}]}
```

规则：
- `profiles` 只放活档（`persons/index.json` 的 `people`）；`deletedKeys` 原样搬 `deletedProfileKeys`。
- `groups.members` 成员归属来自内置 `fireStationData[站].groups`；云端的 `settings.localGroupTasks` 只提供组名与任务/位置。
- `duty` 取 `duty_v2.json`（缺则 `duty.json`）的 `data.dutySchedule.entries`。
- `calibrations` 取 `scba_samples.json` 的 `data.profiles[].scbaCalibrations`。
- `settings`/`operationMode` 取 `snapshot_v2.json` 的 `data.settings` 与顶层 `operationMode`。
- 无云端数据的站，用内置种子（`out/bundle-core.json`）兜底并在 `source` 标 `bundle`。

## 4. 执行顺序

```
1) node extract-bundle-core.mjs      # 内置站级种子（兜底）
2) node extract-bundle-water.mjs     # 水源 NDJSON + 照片
3) node build-water-sql.mjs          # 生成 0001_water_source.sql
4) node extract-cloud.mjs            # 云端花名册/值班/编组/标定/阈值/模式
5) node build-file-manifest.mjs      # 附件清单（默认只下小文件）
   数据库执行 0000_landing_tables.sql → 0001_water_source.sql
6) 启动后端并设置 CCDS_LEGACY_DATA_DIR=<out> 走 Java 导入（人员/编组/值班/标定/阈值/模式/删除记录）
7) 设置 CCDS_LEGACY_IMPORT_MANIFEST=<out>/legacy-file-manifest.json 走附件登记（需 COS 凭证）
8) 执行 verify.sql 核对
```

## 5. 不做与边界

- 不迁移进行中的内攻实时态（`snapshot.json`/`runtime.json` 的 `persons`）——按一期口径（现场态不可信）。
- 不迁移内攻历史归档（旧版无归档表，只有运行快照）。
- 默认不下载 `water-video`（约 1.6 GB）与 `keyunit` 大 PDF（约 3 GB）：清单先生成，按需 `--download` 分批；视频只登记「是否存在」供 1.6 的「上传出水视频数」统计。
- 不迁移 R2 的 `app_update`（旧安装包）、`web_desktop`（旧 PC 端）、`share`（旧分享页静态资源）。
- 不迁移 `nxgk-files/cloud/...` 的 `watch` 表（旧 Watch 端数据，客户已停用）。
- 附件默认零下载：`build-file-manifest.mjs` 不带 `--download` 时只做盘点；`--download=water_doc,drain,ins` 下小文件（约 470 MB），`--download=keyunit`/`all` 才是 GB 级；`--limit=N` 可抽测。内置水源照片（637 张）本地已解出，不占下载。
- `out/water.ndjson` 由 `extract-bundle-water.mjs` 写到 `LEGACY_MIGRATION_OUT/out/`，`build-file-manifest.mjs` 两处都找（`$OUT` 与 `$OUT/out`）。
