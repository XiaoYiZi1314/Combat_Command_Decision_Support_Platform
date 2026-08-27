# 03 · 代码风格

适用：`**/*.{java,xml}`。基于《阿里巴巴 Java 开发手册》，并与本仓库分层对齐。

## 命名

- 禁止 `_` / `$` 开头或结尾；禁止拼音英文混用（国际通用名除外）。
- 布尔属性不加 `is` 前缀：`Boolean deleted`，不是 `isDeleted`。
- 类：UpperCamelCase；DO/DTO/VO/BO/AO 后缀全大写（`UserDTO`）。
- 抽象类：`Abstract` / `Base` 开头；异常：`Exception` 结尾；测试：`{被测类}Test`。
- 方法 / 变量：lowerCamelCase；常量：`UPPER_SNAKE_CASE`。
- 包名全小写、单数、一点一词：`com.ccds.command.plan`。
- 表 / 字段：小写 + 下划线；表名单数；表前缀 `ccds_`。

## 类型后缀

DTO / DO / Entity / VO / BO / Query / Command / Constant / Enum / Handler / Translator / Wrapper / Service / ServiceImpl / Controller / Repository / Mapper / Client / Provider / Consumer / Util / Exception。

对外服务必须是接口，实现类 `Impl` 后缀。枚举类名带 `Enum`，成员 `UPPER_SNAKE_CASE`。

## 常量

禁止魔法值。`long` 字面量用 `2L`。按功能拆常量类，禁止一个巨型 `Constants`。仅在有限集合且有附加属性时用枚举。

## OOP（强制摘要）

- 静态成员用类名访问；覆写加 `@Override`。
- 可变参数仅同类型同语义，且放最后；禁止 `Object...`。
- 对外接口不改签名；过时加 `@Deprecated` 并指向新接口。
- `equals`：常量或 `Objects.equals` 在前；包装类比较用 `equals`。
- POJO / RPC 入参出参用包装类型；POJO 属性不设默认值。
- 构造器不做业务；POJO 必须 `toString`（继承时先 `super.toString()`）。
- 循环内字符串拼接用 `StringBuilder`。

## 集合（强制摘要）

- 重写 `equals` 必须重写 `hashCode`。
- `subList` 不可强转 `ArrayList`，不可在改原集合后继续用。
- `toArray(T[] array)` 传入同类型、大小为 `list.size()`。
- `Arrays.asList` 不可 `add` / `remove` / `clear`。
- foreach 中禁止 `add` / `remove`，用 `Iterator`。
- Comparator 必须满足相反、传递、相等一致。
- 集合初始化尽量指定容量；遍历 Map 用 `entrySet` 或 `forEach`。

## 并发（强制摘要）

- 禁止显式 `new Thread`；线程池必须 `ThreadPoolExecutor`，禁止 `Executors`。
- 禁止 static 未加锁 `SimpleDateFormat`；用 `DateTimeFormatter`。
- 能无锁不用锁；锁区块优于锁方法；多资源加锁顺序固定。
- 乐观锁冲突率 < 20% 可用，重试不少于 3 次。
- 用 `ScheduledExecutorService` 替代 `Timer`；多线程随机用 `ThreadLocalRandom`。

## 方法前缀

查询：`get` / `find` / `query` / `select` / `list` / `count` / `is` / `has` / `can`。

修改：`save` / `create` / `update` / `delete` / `remove` / `add` / `batch`。

业务：`validate` / `check` / `convert` / `build` / `handle` / `process` / `execute`。

## 注释

类、公共方法、字段必须中文 Javadoc；`@author`、`@since` 必填。复杂逻辑加行内中文注释。禁止 `// 赋值` 一类废话。

## Lombok

实体：`@Data` `@NoArgsConstructor` `@AllArgsConstructor` `@Builder`。日志：`@Slf4j`。注入：`@RequiredArgsConstructor` + `final`。

## 格式

- 4 空格，禁止 Tab。
- 行宽建议 100，最大 120。
- 左大括号不换行；空块写 `{}`。
- `if (` 保留字与括号有空格；括号内侧无空格；运算符两侧有空格；逗号后有空格。
- `//` 后恰好一个空格。不为对齐加空格。
- 导包：`java` → `javax` → `org.springframework` → `com.ccds` → `com.baomidou` → `lombok` → 第三方 → `static`。
