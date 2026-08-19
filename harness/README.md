# Harness · 开发规范总册

> Agent 读完根目录 [AGENTS.md](../AGENTS.md) 后必须读本文件。开发过程遵守本目录全部生效分册。
>
> 本目录是工作手册，不是业务文档。改代码前按任务类型按需深入，禁止只读索引就动手。

## 1. 这是什么

`harness/` 规定本仓库里 Agent 如何规划、改代码、查库、写日志、做集成。目标：

- 分层单向、模块边界清晰
- 安全与敏感数据不越线
- 风格与阿里巴巴 Java 手册一致
- 改动可逆、可审查、不擅自提交

## 2. 每次任务怎么用

```
接到任务
  → 读 AGENTS.md（门禁）
  → 读本文件
  → 按任务类型读对应分册
  → 勾 CHECKLIST「修改前」
  → 实现（遵守 00-session）
  → 勾 CHECKLIST「修改后」+ 08-forbidden
  → 不自动 commit
```

压缩上下文、新会话、子 Agent 启动后，必须重新走一遍。

## 3. 分册索引

| 编号 | 文件 | 何时必读 | 优先级 |
|------|------|----------|--------|
| — | [CHECKLIST.md](CHECKLIST.md) | 动手前、完成后 | critical |
| 00 | [00-session.md](00-session.md) | 任何改代码任务 | critical |
| 01 | [01-agent-bounds.md](01-agent-bounds.md) | 每次工作 | critical |
| 02 | [02-architecture.md](02-architecture.md) | 新建类 / 跨层 / 改包结构 | critical |
| 03 | [03-code-style.md](03-code-style.md) | `**/*.{java,xml}` | high |
| 04 | [04-security.md](04-security.md) | 入参、鉴权、敏感字段、SQL | critical |
| 05 | [05-integration.md](05-integration.md) | `**/integration/**` 或外部系统 | high |
| 06 | [06-database.md](06-database.md) | Mapper / SQL / 实体 / 脚本 | high |
| 07 | [07-logging.md](07-logging.md) | 异常、日志、脱敏 | medium |
| 08 | [08-forbidden.md](08-forbidden.md) | 任何提交前 | critical |
| 09 | [09-design.md](09-design.md) | 新模块、接口设计、取舍 | high |
| 10 | [10-coding.md](10-coding.md) | 函数拆分、命名、嵌套 | high |
| 11 | [11-patterns.md](11-patterns.md) | 重构、模式、DI | high |
| 12 | [12-thinking.md](12-thinking.md) | 始终生效 | critical |

## 4. 任务类型 → 必读

| 任务 | 必读 |
|------|------|
| 修 Bug / 小改动 | 00, 01, 03, 07, 08 + CHECKLIST |
| 新接口 / 新业务 | 00, 01, 02, 03, 04, 07, 08, 09, 10 |
| 改表 / Mapper / SQL | 00, 02, 04, 06, 08 |
| 外部系统对接 | 00, 02, 04, 05, 07, 08 |
| 重构 | 00, 01, 02, 09, 10, 11, 08 |
| 仅问答 / 读代码 | 01, 12；涉及架构再读 02 |

## 5. 项目约定（摘要）

- 产品：作战指挥辅助决策平台（CCDS）
- 分层：`bootstrap → app → service → infrastructure → api`
- 包根：`com.ccds.{模块}.{业务域}.{分层}`
- 表前缀：`ccds_`；字段 `snake_case`；Java `camelCase`
- 接口在 `service`，实现在 `service.impl`
- 思考过程用中文

## 6. 维护

未经用户明确要求，Agent 不得修改 `AGENTS.md` 与 `harness/**`。
