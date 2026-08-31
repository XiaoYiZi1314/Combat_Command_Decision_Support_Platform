# 路由与原生体验

## 1. 形态

一份 Vite 模块化 H5，安卓 / 鸿蒙各一个薄 WebView 壳。业务界面全部在 H5。NFC、罗盘、定位、可选语音走 JsBridge。

路由用 Hash。返回键：全屏面板先关上面板；主界面再问一次是否退出，避免清掉现场卡片。

## 2. 路由表

| Hash | 页面 | 对应现网 | 一期 |
|------|------|----------|------|
| `#/login` | 登录 | `#loginGate` | 做 |
| `#/legal` | 免责声明 | `#legalNoticeMask` | 做（首次） |
| `#/attack` | 内攻主界面（含主/副屏分页） | `#list` + 顶栏统计 | 做 |
| `#/attack/quick-add` | 快速录入（底栏面板） | `#quickPanel` | 做 |
| `#/settings` | 参数设置 | `#settingsPanel` | 做 |
| `#/roster` | 花名册 | `#rosterGroupPanel` | 做 |
| `#/roster/:id` | 编辑档案 | `#profileEditPanel` | 做 |
| `#/weather` | 天气与方位 | `#weatherCompassPanel` | 做 |
| `#/scba` | 空呼时间计算器 | `#scbaPanel` | 做 |
| `#/water` | 水源档案 | `#groupStatsPanel` | 做 |
| `#/water/map` | 水源地图 / 最近水源 | 水源地图模式 | 做 |
| `#/share` | 二维码共享 | `#syncPanel` | 做 |
| `#/hazmat` | 危化品查询 | `#hazmatPanel` | 做 |
| `#/duty` | 值班表 | `#dutyPanel` | 做 |
| `#/supply` | 供水计算 | `#waterCalcPanel` | 做 |
| `#/supply/map` | 选火灾地点 | `#waterCalcMapPanel` | 做 |
| `#/ai` | AI 助手 | `#chatPanel` | 做 |
| `#/key-units` | 重点单位 | `#keyUnitsPanel` | 做 |
| `#/key-units/:id` | 编辑/查看预案 | `#keyUnitEditPanel` / 预览 | 做 |
| `#/hq` | 大队/支队汇总 | 现网指挥首页 | 做 |
| `#/hq/station/:stationId` | 单站组与卡片 | 点进某站 | 做 |
| `#/version` | 版本信息 | `#versionInfoPanel` | 做（精简，不做商店化推送） |
| 对外 `/s/:token` | 微信扫码只读页 | 现网 Worker 分享页 | 做（后端静态/模板，非 APP 内） |

一期隐藏、不建路由：车辆、文书、内攻历史、批量 NFC、作业模式切换。（二期已按 [ADR-0012](../adr/0012-phase2-vehicle-archive-doc-nfc.md) 启用车辆、文书、内攻历史与批量 NFC 本地登记路由；作业模式切换仍不做）

## 3. 必须保住的手感

从现网 `index.html` 抄交互，不重设计：

1. 暗红顶栏 + 玻璃拟态卡片，背景 `#0a0e14`
2. 顶栏四格：安全 / 预警 / 危险 / 已撤出，点按过滤
3. 主操作：快速录入、云同步、更多功能
4. 左右滑主屏（人员卡片）/ 副屏（战斗小组总览），**不是**两条路由
5. 快速录入从底部滑出约 72vh，可拖手柄改高度
6. 压力用大滑条（0–30 MPa，步进 0.1），不是纯数字键盘
7. 卡片状态色：安全蓝、预警琥珀、危险红脉冲、已撤出变淡
8. 底栏「请进行NFC扫描」
9. 右侧抽屉「更多功能」，项高约 54px，华为/鸿蒙下抽屉可滚
10. 全屏面板顶栏「返回/完成」，不要 Material 底 Tab 重做导航
11. 指挥端先站卡片汇总，再进单站；返回用「返回主界面」
12. 语音提醒可关（设置里）

## 4. 壳职责

| 能力 | H5 | 安卓壳 | 鸿蒙壳 |
|------|----|--------|--------|
| 业务 UI | 全部 | 否 | 否 |
| NFC 读标签 | 调 `bridge.nfcRead()` | Android NFC Adapter | 鸿蒙 NFC Kit（独立实现） |
| 罗盘 | 调 `bridge.heading()` | Sensor | 鸿蒙传感器 |
| 定位 | 调 `bridge.locate()` | 系统定位 | 鸿蒙定位 |
| 语音 | Web Speech 优先，失败走桥 | TTS | TTS |
| 打开外链 | `bridge.openUrl` | Intent | 鸿蒙 Want |
| 版本号 | 展示 | 注入包版本 | 注入包版本 |

桥约定：Promise JSON `{ ok, data, errorCode }`。壳缺失某能力时 H5 降级（NFC 改为手选花名册）。

## 5. 文件拆分（H5）

```
app-h5/
  src/
    app.js                 # 启动、Hash 路由、会话
    styles/tokens.css      # 现网 CSS 变量与玻璃拟态
    bridge/index.js
    api/                   # 只调本后端
    stores/                # 本站缓存、离线队列
    pages/attack/
    pages/roster/
    pages/water/
    pages/hq/
    pages/duty/
    pages/key-units/
    pages/hazmat/
    pages/ai/
    pages/weather/
    pages/scba/
    pages/share/
    pages/supply/
    pages/settings/
    pages/login/
  android-shell/
  harmony-shell/
```

禁止再把业务堆回单文件 `index.html`。
