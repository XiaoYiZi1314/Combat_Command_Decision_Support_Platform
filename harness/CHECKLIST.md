# 完工清单

动手前与完成后各勾一遍。未勾完不得声称任务完成。

## 修改前

- [ ] 已读 [AGENTS.md](../AGENTS.md)
- [ ] 已读 [README.md](README.md)
- [ ] 已按任务类型读对应分册
- [ ] 改动落在正确模块与包路径
- [ ] 已理解现有代码与业务，不是凭空写
- [ ] 跨模块 / 改公共接口 / 敏感数据 / 新集成 / 兼容性风险：已先问用户

## 修改后 · 安全

- [ ] 无字符串拼接 SQL，无 `${}` 拼用户输入
- [ ] 无明文存传口令、证件、通信标识、密钥
- [ ] 无硬编码密钥 / Token / 口令
- [ ] 日志无敏感原文、无精确敏感坐标
- [ ] 无 `Runtime.exec`、不安全反序列化、动态执行用户输入

## 修改后 · 架构

- [ ] 无循环依赖
- [ ] Controller 未直接访问 Mapper / Repository
- [ ] 未把 Entity / DO 直接返回前端
- [ ] 依赖方向符合 `bootstrap → app → service → infrastructure → api`
- [ ] 接口在 `service`，实现在 `service.impl`

## 修改后 · 代码

- [ ] 未吞异常；异常有日志或向上抛
- [ ] 无魔法数字 / 未定义硬编码串
- [ ] 泛型已参数化（`List<T>` 不是 raw `List`）
- [ ] `switch` 有 `default` 且在最后
- [ ] `if` / `else` / `for` / `while` / `do` 都有大括号
- [ ] if-else 嵌套不超过 3 层
- [ ] 重写 `equals` 时同时重写 `hashCode`
- [ ] foreach 中未 `add` / `remove`
- [ ] 正则已预编译为静态常量

## 修改后 · 性能与并发

- [ ] 未在循环中开事务或逐条查库
- [ ] 无 N+1
- [ ] 无全表 `findAll`（大数据量必须分页）
- [ ] 未显式 `new Thread`
- [ ] 未用 `Executors` 建线程池（用 `ThreadPoolExecutor`）
- [ ] 日期格式化用 `DateTimeFormatter`，不用未加锁的 static `SimpleDateFormat`

## 修改后 · 日志与交付

- [ ] 无 `System.out` / `System.err`
- [ ] 日志用占位符，ERROR 带完整堆栈
- [ ] 代码可编译
- [ ] 未自动 git commit / push
