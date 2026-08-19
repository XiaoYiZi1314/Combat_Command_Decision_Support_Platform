# 05 · 外部系统集成

适用：`**/integration/**` 或对接外部系统。新建集成模块必须先问用户。

## 目录

```
com.ccds.{模块}.integration.{系统名}/
├── config/
├── constant/
├── request/
├── response/
├── service/
├── service.impl/
├── handler/
├── handler.registry/
├── provider/
├── consumer/
├── context/
│   └── spi/
├── util/
└── exception/
```

系统名用小写英文，例如 `gis`、`intel`、`force`。禁止把外部系统数据结构散落到业务包。

## 接口优先

能力以接口定义，实现放 `impl`。业务只依赖接口，禁止依赖 `*Impl`。

## 转换

外部 DTO ↔ 本系统 DTO 必须经 `Translator`（`DataTranslator<S, T>`）。禁止在业务代码里直接扒外部字段。

## 错误与超时

- HTTP 客户端必须设连接 / 读取超时（建议 5s / 30s）。
- 可重试调用：最多 3 次，指数退避；注意幂等。
- catch 必须带请求上下文（已脱敏）和完整堆栈，再转为业务异常；禁止空 catch、禁止丢掉 cause。

## 日志与监控

| 时机 | 级别 | 内容 |
|------|------|------|
| 开始 | INFO | 业务标识、脱敏后的请求摘要 |
| 成功 | INFO | 业务标识、状态、耗时 |
| 失败 | ERROR | 业务标识、完整堆栈 |
| 对方业务错误 | WARN | 业务标识、错误码与摘要 |

必须记录调用轨迹（请求时间、响应时间、耗时）。禁止把完整大报文打进 INFO。

## 配置

```yaml
external:
  {系统名}:
    base-url: https://example.invalid
    app-id: ${EXT_APP_ID}
    app-secret: ${EXT_APP_SECRET}
    timeout: 30000
```

用 `@ConfigurationProperties` 绑定。密钥只来自环境变量 / 配置中心。

## 测试

外部客户端必须可 Mock。单测不打真实外网。
