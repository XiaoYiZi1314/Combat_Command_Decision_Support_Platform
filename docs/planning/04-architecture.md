# 一期技术架构

## 1. 逻辑结构

```
安卓壳 / 鸿蒙壳
        │ JsBridge
        ▼
   模块化 H5（Vite + Hash 路由）
        │ HTTPS + WebSocket
        ▼
   Java 后端（腾讯云 CVM）
        │
        ├─ MySQL 8    业务表 ccds_*
        ├─ Redis      WS 会话、补传去重
        ├─ COS        预案/照片/共享页资源
        ├─ 百度地图代理
        └─ 模型网关    危化品 / 研判 / 供水 AI
```

分层遵守仓库 harness：`bootstrap → app → service → infrastructure → api`。包根 `com.ccds.{模块}.{业务域}.{分层}`。

## 2. 建议模块切分

| 模块 | 业务域 | 职责 |
|------|--------|------|
| `ccds-iam` | identity | 账号、会话、改密、锁定 |
| `ccds-org` | org | 支队/大队/站编制，只读查询 |
| `ccds-roster` | roster | 花名册、编组、NFC 编号 |
| `ccds-attack` | attack | 人员卡片、状态、空呼计算、实时快照 |
| `ccds-duty` | duty | 值班 |
| `ccds-water` | water | 水源、最近水源 |
| `ccds-plan` | keyunit | 重点单位与预案文件 |
| `ccds-share` | share | 共享令牌与只读投影 |
| `ccds-assist` | assist | 天气、危化品、AI、供水计算 |
| `ccds-infra` | — | COS、Redis、地图、模型客户端 |
| `ccds-bootstrap` | — | 启动、配置、MyBatis XML、种子脚本 |

一期可以先做成多 Maven 模块的单体进程（一个 bootstrap 进一个 CVM），不要先拆微服务。模块边界按上表切包，便于以后拆。

## 3. 实时与离线

```
站端写卡片
  → 本地 store + 补传队列（IndexedDB）
  → POST /attack/events   （幂等 eventId）
  → service 落库、重算站快照
  → Redis pub
  → 指挥端 / 共享页 WS

指挥端
  连接 /ws/command?token=
  订阅可见 stationId
  断线 → GET /attack/snapshots?since=
```

幂等：每条入场/复测/撤出带客户端 `eventId`（UUID）。重复投递不生成第二张卡。

冲突：同一卡片以 `updatedAt` 更大的为准；站端展示「已用云端更新覆盖」仅当远端更新不是自己刚写的。

## 4. 安全

- HTTPS + JWT 访问令牌 + 短 refresh（已定，见 ADR-0011）
- 口令 BCrypt；种子口令首次登录作废
- 接口按站数据权限过滤，禁止只靠前端藏按钮
- COS 预签名 URL，短 TTL
- 地图 AK、模型 Key 只在 infrastructure
- 日志禁止打口令、证件、精确敏感坐标（harness 04/07）
- 共享令牌随机、可撤销、TTL

## 5. 腾讯云部署（一期最小）

| 资源 | 用途 |
|------|------|
| CVM 1 台 | Java + Nginx（API + WS 反代 + 对外共享页） |
| 云 MySQL 8 | 主库 |
| Redis | 通道 |
| COS | 文件 |
| 安全组 | 仅 443 对外；3306/6379 仅内网 |

域名与证书开工时再定，不写死 IP 进 APP。APP 只配一个 `API_BASE`。

## 6. 与现网切割

| 现网 | 一期 |
|------|------|
| `localStorage` 当主库 | 后端主库 + 本机缓存 |
| R2 直传 + 前端密钥 | 删除 |
| `http://119.45.196.24:8080/nxgk-file` | 删除 |
| Agnes Key 在 HTML | 后端模型网关 |
| Cloudflare Worker 分享 | 本后端 `/s/:token` |
| 作业模式 dispatch-index | 用「未撤出人数 > 0」 |

## 7. 安卓 / 鸿蒙节奏

1. H5 在 Chrome 可跑通业务（NFC 用模拟器按钮）
2. 安卓壳接入真 NFC / 定位 / 罗盘
3. 鸿蒙壳先跑同一 H5，桥返回「不支持」时 H5 降级
4. 再补鸿蒙 NFC/传感器，出 HAP
