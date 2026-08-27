# 08 · 禁止行为（红线）

违反即停。完成后与 [CHECKLIST.md](CHECKLIST.md) 对照。

## 安全 · Critical

- 禁止拼接 SQL / `${}` 拼用户输入。用参数绑定 `#{}` 或 `?`。
- 禁止明文存传口令、证件、通信标识；禁止日志打敏感原文与精确敏感坐标。
- 禁止硬编码密钥、Token、口令。
- 禁止 `Runtime.getRuntime().exec` 执行用户相关命令。
- 禁止对不可信数据使用 `ObjectInputStream`；用 JSON 等安全反序列化。
- 禁止动态执行用户输入。

## 架构 · Critical

- 禁止循环依赖。
- 禁止 Controller 注入或调用 Mapper / Repository。
- 禁止把 Entity / DO 直接返回前端。
- 禁止下层依赖上层。

## 代码 · High

- 禁止空 catch。
- 禁止魔法数字与未定义硬编码串。
- 禁止 raw 泛型。
- 禁止无防护的深层 NPE 链式调用。

## 性能 · High

- 禁止循环内开事务或逐条查库。
- 禁止 N+1。
- 禁止大数据量全表 `findAll`。

## 日志 · Medium

- 禁止 `System.out` / `System.err`。
- 禁止日志打印敏感信息。
- 禁止把超大对象整包打进 INFO。

## 并发 · High

- 禁止无同步的共享可变计数（用 `Atomic*` 或锁）。
- 禁止循环里 `Thread.sleep` 当重试（用调度或有界退避）。
- 禁止显式 `new Thread`。
- 禁止 `Executors` 建线程池。
- 禁止 static 未加锁 `SimpleDateFormat`。

## 集合 · High

- 禁止只改 `equals` 不改 `hashCode`。
- 禁止 `subList` 强转 `ArrayList`，禁止改原集合后继续用 `subList`。
- 禁止无参 `toArray()` 再强转。
- 禁止对 `Arrays.asList` 做结构性修改。
- 禁止 foreach 中 `add` / `remove`。
- 禁止 Comparator 不处理相等。

## 控制 · Medium

- 禁止 `switch` 无 `default`。
- 禁止 `if` / `for` / `while` / `do` 省略大括号。
- 禁止 if-else 超过 3 层（卫语句 / 策略 / 状态）。

## 其他 · Medium

- 禁止方法体内每次编译正则；用 `static final Pattern`。
- 禁止在视图模板写复杂业务判断。
- 禁止 `(int) (Math.random() * n)`；用 `Random.nextInt` 或 `ThreadLocalRandom`。
