# 07 · 日志与异常

适用：异常处理、日志、脱敏。基于《阿里巴巴 Java 开发手册》异常日志规约。

## 异常（强制）

- NPE、越界等可通过预检查避免的，不要靠 catch。
- 异常不做流程控制。
- 只包非稳定代码；按类型分别处理。
- 捕获必须处理或继续抛；最外层转成用户可理解的结果。
- 事务里 catch 后若需回滚，必须手动回滚。
- 资源用 try-with-resources；`finally` 禁止 `return`。
- 捕获类型必须与抛出匹配或其父类。
- 禁止 `new RuntimeException()` / 直接抛 `Exception`；用业务异常。
- 对外 HTTP：错误码；应用内部：抛异常；跨应用 RPC：`Result`（`isSuccess` + 码 + 摘要）。
- 方法可以返回 null，但必须在 Javadoc 写明；调用方判空。远程结果、集合元素、级联调用都要防 NPE；推荐 `Optional`。

## 框架

SLF4J + Logback + `@Slf4j`。禁止 `System.out` / `System.err`。

## 日志（强制）

- 日志至少保留 15 天（运维配置，代码侧不改 logback 除非用户要求）。
- 扩展日志名：`appName_logType_logName.log`。
- `trace` / `debug` / `info` 必须用占位符或 `isDebugEnabled()`，禁止先拼接再打。
- 避免重复打印（appender `additivity="false"`）。
- ERROR 必须带现场 + 完整堆栈：`log.error("... {}", id, e)`。
- 生产禁止 debug；用户参数错误用 WARN，系统故障用 ERROR。

## 级别

| 级别 | 用途 |
|------|------|
| ERROR | 程序异常、DB 失败、外部调用失败、配置错误 |
| WARN | 业务校验失败、资源告警、可恢复异常 |
| INFO | 启停、定时任务、关键业务节点、外部调用成功 |
| DEBUG | 仅开发排查；入参 / SQL / 循环细节 |

每条业务日志必须带业务标识（单号、任务 ID 等）。禁止把超大结果集整包打进日志，只打数量或摘要。

## 脱敏

禁止打印口令、证件、通信标识、密钥、银行卡、精确敏感坐标。需要关联时打业务 ID 或脱敏值。脱敏实现放 infrastructure 工具类，禁止在业务里手写截串。
