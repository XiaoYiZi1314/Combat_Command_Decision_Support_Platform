# 06 · 数据库

适用：Mapper、SQL、实体、脚本。基于《阿里巴巴 Java 开发手册》MySQL 规约。

## 建表（强制）

- 是否字段：`is_xxx`，`unsigned tinyint`（1 是 / 0 否）。非负字段用 `unsigned`。
- 表名、字段名：小写或数字；禁止数字开头；禁止两下划线间只有数字。
- 表名单数；禁用保留字（`desc`、`range`、`match` 等）。
- 索引：主键 `pk_`，唯一 `uk_`，普通 `idx_`。
- 小数用 `decimal`，禁止 `float` / `double`。
- `varchar` 不超过 5000；更长用独立 `text` 表。
- 必备：`id`（`unsigned bigint`）、`gmt_create`、`gmt_modified`。
- 表前缀 `ccds_`。业务唯一字段必须有唯一索引。
- 禁止外键与级联（应用层保证）；禁止存储过程。
- 单表预计三年内不到 500 万行 / 2GB，不要提前分库分表。

## 索引与 SQL（强制）

- 超过三表禁止 JOIN；JOIN 字段类型必须一致且有索引。
- `varchar` 索引必须指定长度；禁止左模糊 / 全模糊（走搜索）。
- 用 `count(*)`，不要用 `count(列)` 替代行数。
- `sum` 注意全 NULL 返回 NULL。
- 判空用 `ISNULL()` / `IS NULL`，不要用 `= NULL`。
- count 为 0 则不要再跑分页 SQL。
- 订正数据：先 `select` 确认再 `update` / `delete`。
- `in` 集合控制在 1000 以内。
- 禁止在业务代码中 `TRUNCATE`。
- 字符集 `utf8mb4`。

## ORM（强制）

- 禁止 `SELECT *`；必须 `resultMap`，禁止用 HashMap 当查询输出。
- Java 布尔属性不加 `is`；库字段用 `is_`；在 `resultMap` 映射。
- 参数只用 `#{}`，禁止 `${}` 拼用户输入。
- 更新必须同时更新 `gmt_modified`。
- 禁止大而全的全字段 update；只更新变更列。
- `@Transactional` 仅 Service，`rollbackFor = Exception.class`；禁止循环内开事务。
- 默认隔离 `READ_COMMITTED`。禁止滥用大事务。

## 查询

- 必须分页；禁止大数据量 `findAll`。
- 避免循环内查库（N+1）；用 JOIN、批量查询或 `@EntityGraph`。
- 开启 `map-underscore-to-camel-case`。

## 缓存

- 必须设过期；防穿透可缓存空值；防雪崩用分散过期 / 锁。
- Key：`{模块}:{业务}:{操作}:{标识}`，例如 `ccds:plan:get:12345`。

## 脚本

```
bootstrap/scripts/db/patch/rdb/
├── 0001_init/
└── 00xx_{版本}/
```

禁止删除已发布脚本；禁止物理删表 / 删列（用废弃标记）。未经用户明确要求不得改已发布 DDL。
