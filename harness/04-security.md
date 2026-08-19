# 04 · 安全边界

适用：入参校验、鉴权、敏感字段、SQL / XML。

本平台按指挥信息系统处理数据。敏感范围宽于一般业务系统。

## 敏感数据

口令；证件号；手机号 / 通信标识；银行卡；密钥 / Token / apiKey；邮箱；精确作战坐标与密级位置；编制实力等未脱敏态势。

### 禁止

- 明文存储口令、证件、通信标识
- HTTP 明文传敏感字段；日志打印敏感原文
- 硬编码密钥、Token、口令、敏感配置

### 必须

- 口令用 BCrypt 或项目统一哈希；其他敏感字段走统一加解密模块
- 传输走 HTTPS；配置从环境变量或配置中心读取
- 日志脱敏：手机 `138****1234`；证件保留前 4 后 4；坐标按密级降精度或只打业务 ID

## SQL 注入

| 正确 | 错误 |
|------|------|
| `#{param}` | `${param}` 拼用户输入 |
| `WHERE id = #{id}` | `WHERE id = ${id}` |

动态排序 / 列名必须白名单，禁止直接 `${column}`。

JPA：用 Criteria / 参数绑定 JPQL；禁止字符串拼原生 SQL。

复杂 SQL 写在 XML；禁止在 XML 里拼接用户输入。

## 输入校验

Controller：`@Valid` + `@NotBlank` / `@NotNull` / `@Size` / `@Email` / `@Pattern` / `@Min` `@Max` / `@DecimalMin` `@DecimalMax`。

Service：业务规则二次校验，失败抛业务异常。禁止信任前端。

## 权限

接口鉴权（如 `@RequiresPermissions`）。查询必须带数据权限过滤。敏感操作记审计：操作人、时间、内容、结果（内容已脱敏）。

## 危险 API

禁止：`Runtime.getRuntime().exec`；无白名单 `Class.forName`；`ObjectInputStream` 反序列化不可信数据；`eval` / 动态执行用户输入。

后端输出用 `HtmlUtils.htmlEscape`。前端用 `textContent` 或已审查的消毒库，禁止直接 `innerHTML` 用户输入。

写操作走 CSRF Token；敏感操作二次确认。
