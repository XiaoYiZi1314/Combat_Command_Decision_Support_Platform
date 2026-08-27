# 阶段 10 · 迁移与收口

规划：[06-dev-breakdown.md](06-dev-breakdown.md) 阶段 10。基线提交 `c7fa013`（P9 之后）。本阶段产出是报告与走查，**未改业务代码、未 commit**。

| ID | 任务 | 文档 | 状态 |
|----|------|------|------|
| P10-1 | 花名册/编组/水源/预案/值班迁移报告 | [p10-migration-report.md](p10-migration-report.md) | 完成 |
| P10-2 | 放弃清单（R2 / 进行中内攻 / 车辆） | [p10-abandon-list.md](p10-abandon-list.md) | 完成 |
| P10-3 | 对照需求文档 18 张图走查 | [p10-ux-walkthrough.md](p10-ux-walkthrough.md) | 完成（14 对齐 / 4 缺口） |
| P10-4 | 安全走查：无前端密钥、无明文口令、共享页脱敏 | [p10-security-walkthrough.md](p10-security-walkthrough.md) | 通过 |

## 迁移摘要

- 已进库：16 大队、23 站、41 账号（哈希+强制改密）、**464** 花名册、**70** 编组。
- 未进库（通道已就绪）：水源 0、预案 0、值班 0。
- 明确不迁：进行中内攻、R2 对不上的对象、车辆与历史。

## 18 图缺口（建议下一刀）

H5 未挂载：`#/duty`、`#/key-units`、`#/share`、`#/scba`。对外 `/s/:token` 只有 JSON，无微信 HTML。后端 API 均已存在。

## 安全

源码侧三项通过。正式 APK 反编需出包后补验。
