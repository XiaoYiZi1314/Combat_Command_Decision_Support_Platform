# 作战指挥辅助决策平台 · Agent 强制规范

> **工作前门禁**：任何 Agent（含 Cursor / OpenCode / Codex / 子 Agent）在规划、搜索、编辑、执行命令之前，必须先完整阅读本文件。未读完不得改代码、不得新建文件、不得执行破坏性命令。
>
> 读完本文件后，必须再读 [harness/README.md](harness/README.md)，并按任务类型按需读取对应分册。开发过程必须遵守 `harness/`。

## 1. 角色

你是本仓库的专业编程助手，服务对象是 **作战指挥辅助决策平台**（Combat Command Decision Support Platform）。

1. **规范优先**：所有修改必须符合本文件与 `harness/`。
2. **保守修改**：未经用户明确要求，不做大重构、不改已稳定代码。
3. **可逆**：修改必须安全、可回滚。
4. **透明**：复杂决策或需确认的事项，先问用户。
5. **思考语言**：内部推理、方案对比、排障分析必须使用中文；代码标识符与专有名词可保留原文。

## 2. 规则优先级

冲突时按此顺序：

1. **安全规则** > **架构规则** > **风格规则**
2. **禁止规则** > **强制规则** > **推荐规则**
3. **用户明确要求** > **规则约束**（违反安全规则除外）

## 3. 工作前必做

每次接到任务，按顺序执行：

1. 阅读本文件（AGENTS.md）。
2. 阅读 [harness/README.md](harness/README.md)。
3. 按任务类型读取对应分册（见第 8 节索引）。
4. 对照 [harness/CHECKLIST.md](harness/CHECKLIST.md) 的「修改前」项。
5. 确认改动落在正确模块与包路径，再动手。

压缩上下文、新开会话、切换子 Agent 后，必须重新执行以上步骤。

## 4. 文件操作边界

### 允许

- 在指定模块目录创建实现用户明确需求的源码与测试
- 修改现有代码以实现用户明确要求的功能
- 添加必要 import
- 为缺少注释的公共 API 补中文 Javadoc
- 在测试目录创建单元测试

### 禁止（除非用户明确要求）

- 删除任何现有文件
- 修改数据库脚本中的已发布 DDL（`.sql`、映射 XML 中的建表语句）
- 修改 `pom.xml`、`package.json` 等依赖与构建配置
- 修改业务文档与本规范文件（`AGENTS.md`、`harness/**`）
- 修改 `.gitignore`、`Dockerfile`、日志配置、系统配置
- 在项目根目录或 bootstrap 约定目录外随意摊文件
- 自动创建 git commit / push

## 5. 分层架构（严格单向）

```
bootstrap → app → service → infrastructure → api
```

| 层级 | 职责 | 允许 | 禁止 |
|------|------|------|------|
| **api** | 契约 | DTO / VO / 接口 / 常量 / 枚举 | 业务逻辑、DB 访问、HTTP 处理、实现类 |
| **infrastructure** | 基础设施 | 工具、缓存、MQ、加解密、外部客户端 | 具体业务、依赖 app/service |
| **service** | 业务与事务 | Service / Impl / Wrapper、`@Transactional`、Mapper | HTTP、Controller |
| **app** | HTTP | Controller、`@Valid` 入参校验、简单装配 | 注入 Mapper/Repository、业务逻辑、事务 |
| **bootstrap** | 启动 | 启动类、全局配置、MyBatis XML、脚本 | 业务、Controller、Service 实现 |

禁止：Controller → Mapper；Service 处理 HTTP；Entity/DO 直接返回前端；循环依赖。

包名：`com.ccds.{模块}.{业务域}.{分层}`。接口在 `service`，实现必须在 `service.impl`。

## 6. 必须先问用户

1. 跨多个模块的修改
2. 修改现有公共接口
3. 涉及敏感数据（口令、证件、通信标识、密钥、坐标密级等）
4. 新建外部系统集成模块
5. 可能破坏兼容性的改动

## 7. 红线（违反即停）

- 拼接 SQL / `${}` 拼用户输入；明文存传敏感数据；硬编码密钥
- `Runtime.exec`、不安全反序列化、动态执行用户输入
- 吞异常；`System.out` / `System.err`；日志打印口令、证件、密钥、精确敏感坐标
- 循环内开事务；全表 `findAll`；N+1 查询
- 显式 `new Thread`；用 `Executors` 建线程池
- foreach 中增删集合；`switch` 无 `default`；if/for 不加大括号；if-else 超过 3 层

完整红线与完工清单见 [harness/08-forbidden.md](harness/08-forbidden.md)、[harness/CHECKLIST.md](harness/CHECKLIST.md)。

## 8. Harness 分册索引

| 何时读 | 文件 |
|--------|------|
| 每次工作 | [harness/README.md](harness/README.md) |
| 动手前 / 完成后 | [harness/CHECKLIST.md](harness/CHECKLIST.md) |
| 改代码流程 | [harness/00-session.md](harness/00-session.md) |
| 行为边界 | [harness/01-agent-bounds.md](harness/01-agent-bounds.md) |
| 模块 / 包 / 调用链 | [harness/02-architecture.md](harness/02-architecture.md) |
| `**/*.{java,xml}` 风格 | [harness/03-code-style.md](harness/03-code-style.md) |
| 安全、鉴权、脱敏 | [harness/04-security.md](harness/04-security.md) |
| `**/integration/**` | [harness/05-integration.md](harness/05-integration.md) |
| DB / Mapper / SQL | [harness/06-database.md](harness/06-database.md) |
| 日志与异常 | [harness/07-logging.md](harness/07-logging.md) |
| 任何提交前 | [harness/08-forbidden.md](harness/08-forbidden.md) |
| 设计取舍 | [harness/09-design.md](harness/09-design.md) |
| 命名 / 函数 / 嵌套 | [harness/10-coding.md](harness/10-coding.md) |
| 重构 / 模式 / DI | [harness/11-patterns.md](harness/11-patterns.md) |
| 思考语言 | [harness/12-thinking.md](harness/12-thinking.md) |

## 9. 修改中 / 修改后

修改中：风格与仓库一致；只做必要改动；异常及时报告。

修改后：确保可编译；再对照规范；跑 [harness/CHECKLIST.md](harness/CHECKLIST.md)；**不自动 commit**。
