# P10-4 安全走查

依据：[01-scope.md](01-scope.md) 第 7 节、[04-security.md](../../harness/04-security.md)、[08-forbidden.md](../../harness/08-forbidden.md)、[06-dev-breakdown.md](06-dev-breakdown.md) P10-4。范围：仓库源码与构建配置，**不含**未入库的 `local.properties` / 运行环境变量实测值。基线 `c7fa013`。

P10-4 三项：无前端密钥、无明文口令、共享页脱敏。扩展项按红线一并勾。

## 1. 结论

| 项 | 结果 |
|----|------|
| APP / H5 无 COS、地图 AK、模型 Key、R2、Agnes | **通过**（地图 AK 由后端下发短时凭证，源码无写死 AK） |
| 仓库无明文口令、无种子明文、无硬编码 JWT/字段密钥 | **通过** |
| 共享投影不含电话、NFC、账号 | **通过**（JSON VO）；对外仍是 API 不是 HTML 页 |
| 正式安卓包 `DEBUG_NFC=false`，调试密钥不进 release | **通过** |
| 现网旧 IP / R2 SDK / 前端 Agnes | **未检出** |
| 电话入库 | **密文** `phone_cipher` + AES-GCM |

**走查通过。** 剩余是收口观察项，不构成「前端藏密钥 / 明文口令」。

## 2. 无前端密钥

检索范围：`app-h5/**/*.{js,html,json,java,ets,gradle,xml,properties}`（排除 lockfile 完整性哈希）。关键词：`AKID`、`sk-`、`apiKey` 字面量、`SecretId`/`SecretKey` 赋值、`agnes`、`cloudflare`、`r2.`、`119.45.196.24`。

| 位置 | 情况 |
|------|------|
| H5 | 无硬编码密钥。`maps/baidu.js` 用 `GET /maps/baidu-token` 的 `ak` 拼百度脚本，AK 不进仓库 |
| 安卓壳 | `API_BASE`、`DEBUG_NFC` 来自 `local.properties`（已 gitignore）。`defaultConfig` 与 **release** 均 `DEBUG_NFC=false`；仅 debug 可读本地 `ccds.debugNfc` |
| 鸿蒙壳 | `HostConfig.ets` 的 `API_BASE = ''`，无密钥 |
| 后端 | COS / JWT / 字段密钥 / 模型 Key / 百度 AK 全部 `${CCDS_*}` 或 `System.getenv`，清单见 `tencent-cloud-env.xml` |

`application.yml` 数据库默认用户名 `ccds`、口令空，属本地占位，生产必须覆写 `CCDS_MYSQL_PASSWORD`。

## 3. 无明文口令

| 检查 | 结果 |
|------|------|
| `0002_seed_org_account.sql` | 只写 `@seed_password_hash`；非 `$2` 开头则脚本失败 |
| 种子账号 | 全部 `is_must_change_password=1` |
| `AccountDO` / `LoginCommand` | `@ToString(exclude = password/passwordHash)` |
| 哈希 | `PasswordHashUtil` BCrypt |
| 花名册种子 | 明确不含电话 |
| 登录失败日志 | `unknown_or_bad_password`，不打口令、不区分账号是否存在 |
| APK / H5 | 无默认口令、无「测试腾讯云」账号配置项 |

口令明文只应出现在运维一次性清单（环境变量 `CCDS_SEED_PASSWORD`），不进 git。

## 4. 共享页脱敏

`ShareAttackVO.PersonCard` 字段：`name, groupName, cylType, currentPressure, remainSec, status`。编组汇总：`groupName, count, worstStatus`。另有站名、最后更新时间。

构建路径 `ShareTokenServiceImpl.buildPersonCards` 只映射上述列，不读 `ProfileDO.phoneCipher` / `nfcTag` / 账号。

令牌：UUID 去横线明文只回创建者一次；库内 SHA-256 hex。过期默认 2 小时，可 `revoked_at`。匿名路径 `/api/v1/s/**` 已从鉴权与改密拦截排除。

观察项：`shareUrl` 指向 JSON API，微信内不是独立 HTML；脱敏在 JSON 层已成立。补对外页时禁止把花名册/账号拼进模板。

## 5. 敏感字段与日志

| 数据 | 处理 |
|------|------|
| 电话 | AES-GCM（`v1:` 前缀），`FieldCipherUtil`；列表 `phoneMasked`（前 3 后 4） |
| 花名册 VO | `@ToString(exclude = phone)`；指挥端只给脱敏 |
| 重点单位电话 | 同样脱敏字段 |
| 内攻 AI | `AssistPromptBuilder.whitelistPersons`：姓名、状态、压力、剩余、编组；跳过已撤出 |
| 坐标 | 水源经纬度业务需要；日志规范禁止打精确敏感坐标原文 |
| JWT | 访问 / 刷新分密钥，环境注入 |

## 6. 其它红线抽查

| 项 | 结果 |
|----|------|
| SQL `${}` 拼用户输入 | Mapper 走 `#{}`（抽查花名册 / 共享 / 水源） |
| `Runtime.exec` / 不安全反序列化 / eval 用户输入 | 业务代码未检出 |
| 现网 R2 / Agnes / 旧文件 IP | 业务代码未检出 |
| release 调试 NFC 假标签 | `NfcSession` 仅 `BuildConfig.DEBUG_NFC` 返回 `DEBUG-NFC-0001`；release 为 false |

## 7. 观察项（不挡 P10-4 通过）

1. 对外共享仍是 JSON，需 Nginx/模板页才能满足「微信扫码看态势」；页上仍不得出现电话。
2. `#/share` 未做，站级无法在 APP 内出码（能力在 API）。
3. `WeatherClient` 为模拟，接真天气时 Key 必须留在服务端。
4. 鸿蒙 `API_BASE` 打包前手改，勿把生产域名当密钥提交；当前为空字符串。
5. 反编译验收：正式 APK 应搜不到 `CCDS_COS_SECRET`、`AKID`、模型 Key、地图 AK 字面量。本走查覆盖源码与 Gradle，**未**对已签名 APK 做反编。出包后由发布人补这一刀。
