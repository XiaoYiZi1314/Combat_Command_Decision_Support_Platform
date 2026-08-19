# 02 · 架构边界

适用：新建类、跨层调用、改包结构。

## 模块依赖（严格单向）

```
bootstrap → app → service → infrastructure → api
```

- 上层可依赖下层；下层禁止依赖上层。
- 禁止循环依赖。

| 模块 | 可依赖 | 禁止依赖 |
|------|--------|----------|
| **api** | 无 | service, app, bootstrap |
| **infrastructure** | api | service, app, bootstrap |
| **service** | api, infrastructure | app, bootstrap |
| **app** | api, service, infrastructure | bootstrap |
| **bootstrap** | 全部下层 | 无 |

## 职责

### api

允许：DTO、VO、接口声明、常量、枚举。

禁止：业务逻辑、数据库访问、HTTP 处理、实现类。

### app

允许：Controller、`@Valid` + JSR-303、请求/响应简单转换。

禁止：注入 Mapper/Repository、业务逻辑、`@Transactional`。

### service

允许：Service / Impl / Wrapper、业务、`@Transactional`、Mapper/Repository。

禁止：HTTP、`@Controller` / `@RestController`。

### infrastructure

允许：Util、缓存、MQ、加解密、外部客户端。

禁止：具体业务、依赖 app/service。

### bootstrap

允许：启动类、全局配置、MyBatis XML、脚本。

禁止：业务、Controller、Service 实现。

## 包结构

```
com.ccds.{模块}.{业务域}.{分层}
```

```
com.ccds.{模块}.{业务域}/
├── service/              # 接口
│   └── impl/             # 实现
├── entity/               # 持久化实体
├── model/                # 领域模型
├── dto/
├── vo/
├── controller/
├── constant/
├── enums/
└── wrapper/

com.ccds.{模块}.integration.{系统名}/
├── config/
├── constant/
├── request/
├── response/
├── service/
├── service.impl/
├── handler/
├── provider/
├── consumer/
└── util/
```

接口在 `service`，实现必须在 `service.impl`。handler、wrapper 同样接口与实现分离。

## 调用链

正确：`Controller → Service → Mapper/Repository → DB`，中间可用 Wrapper / Translator。

禁止：Controller → Mapper；Service → Controller；Entity 直接回前端。
