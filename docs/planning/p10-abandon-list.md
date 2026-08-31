# P10-2 放弃清单

依据：[01-scope.md](01-scope.md) 第 3、6 节，[06-dev-breakdown.md](06-dev-breakdown.md) P10-2。对象：现网 R2 / 本机 `localStorage` / 源码包缺失文件。本期**不迁、不恢复、不在 APP 暴露入口**。

与 [p6-water-migration-waiver.md](p6-water-migration-waiver.md) 重叠的水源项在此汇总，不重复开新口径。

## 1. 必须放弃（规格已锁）

| ID | 对象 | 现网位置 | 放弃原因 | 一期替代 |
|----|------|----------|----------|----------|
| A-01 | 进行中的内攻卡片 / 压力 / 剩余时间 | 设备 `localStorage`、R2 快照 | 现场态不可信、不可幂等对账；规格明确不迁 | 站级重新预录入 / NFC / 快速录入 |
| A-02 | 内攻历史记录 | 现网历史页 | 一期隐藏 | 无 |
| A-03 | 车辆档案 | 现网车辆模块 | 一期隐藏 | 供水计算按类型默认泵量，界面写明 |
| A-04 | 车辆审批 / 水源审批流 | 现网审批 | 一期不做 | 无 |
| A-05 | 文书生成 | 现网文书 | 一期隐藏 | 无 |
| A-06 | 批量 NFC 登记 | 现网批量写标签 | 一期隐藏；入场仍做单次 NFC | 花名册手填 NFC |
| A-07 | 测试 / 训练 / 出警作业模式 | 现网模式开关 | ADR 0006；指挥端看「有未撤出卡片」的站 | `#/hq` 过滤未撤出 |
| A-08 | 融合通信自动拉值班 | 现网值班拉取 | 一期不做 | 值班手填 |
| A-09 | PC 指挥台 | 现网 PC | 一期不做 | 无 |
| A-10 | APP 直连 R2 | 现网前端 SDK + 密钥 | 禁止 | 本后端 + COS，密钥只在服务端 |
| A-11 | APP 直连 Agnes | 现网 HTML 内 Key | 禁止 | `/assist/*` 模型网关 |
| A-12 | 旧文件服务 `http://119.45.196.24:8080/nxgk-file` | 现网硬编码 | 禁止 | COS 预签名 |
| A-13 | Cloudflare Worker 分享页 | 现网 Worker | 改本后端 | `GET /api/v1/s/{token}` |

## 2. 对不上的 R2 / 对象存储

现网对象键依赖 Cloudflare R2 与本机缓存，本期无 R2 凭证、无桶清单、不对账。以下一律放弃，**不尝试猜测 key、不写占位行**。

| ID | 对象 | 说明 |
|----|------|------|
| R-01 | R2 内攻快照 JSON | 与 A-01 同类，现场态 |
| R-02 | R2 水源照片及水印经纬度 | 水源表本期无照片字段；识别属后续 |
| R-03 | `PROTECTED_RESTORED_WATER_IDS`（海河路 `municipal_001`，安顺路 `municipal_027/028/029`） | 依赖 R2 快照，本库无记录 |
| R-04 | R2 重点单位预案 doc/docx/pdf/txt | 无对象清单 |
| R-05 | R2 重点单位平面图 | 无对象清单 |
| R-06 | R2 水源档案材料 PDF | 无对象清单 |
| R-07 | 现网分享页静态资源 | Worker / R2 托管，改 COS + 本后端 |
| R-08 | 无法从现网 key 映射到 COS key 的任何文件 | 对不上即放弃 |

补救：现场重新上传到 COS（`POST /api/v1/files/presign`）。旧 R2 URL 不在 APP 打开。

## 3. 源码包缺失、无法解析

| ID | 对象 | 核查结果 |
|----|------|----------|
| M-01 | `water_sources_manifest.js` / `WATER_SOURCE_MANIFEST` | 源码包 assets 仅有 `index.html`、`brigade_organization.js`、`cloud_config.js`；内置水源 **0 条** |
| M-02 | 现网值班导出 | 无文件 |
| M-03 | 现网重点单位清单 | 无文件 |
| M-04 | 花名册电话 / NFC 完整表 | `fireStationData` 种子已剥离；不从 HTML 抠明文 |

## 4. 一期不做的报表与扩展

| ID | 对象 | 说明 |
|----|------|------|
| X-01 | 水源检查「汇总表」（北三区 / 南四区 / 外县目标数） | 现网总部报表，需求一期未列 |
| X-02 | 出水视频 AI 审核 | 规格明确不做 |
| X-03 | 商店化版本推送 | `#/version` 只做精简版本信息 |
| X-04 | 设置里的「云端地址 / 账号 / 密码」 | 删除；连本后端，环境注入 |
| X-05 | 鸿蒙 NFC / 定位 / 罗盘真机能力 | P9-3 一期降级为 `UNSUPPORTED`，H5 手选花名册；不作为迁移对象 |

## 5. 处理规则

1. 放弃对象不进 `ccds_*` 业务表，不在 H5 建路由（车辆、文书、内攻历史、批量 NFC、作业模式已按 [02-routes-and-ux.md](02-routes-and-ux.md) 隐藏）。二期启用车辆/文书/内攻历史/批量 NFC 的决策变更见 [ADR-0012](../adr/0012-phase2-vehicle-archive-doc-nfc.md)——启用的模块走本库 `ccds_*` 表与本仓 H5 路由，不翻 R2 旧数据。
2. 发现残留现网密钥、R2 SDK、旧 IP 视为缺陷，记入 [p10-security-walkthrough.md](p10-security-walkthrough.md)，不「兼容旧通道」。
3. 后续若客户提供 Excel / `water_sources_manifest.js` / 预案文件包，走既有导入 API 或一次性脚本，不翻 R2。
