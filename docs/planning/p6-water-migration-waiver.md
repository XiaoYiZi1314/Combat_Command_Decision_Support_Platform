# P6-5 现网内置水源迁移放弃清单

## 结论

P6-5 要求把现网内置水源数据（`water_sources_manifest.js`）迁入后端数据库：能解析的进库，其余进放弃清单。

经核查现网源码（`nxgk_6241_source/assets/index.html` v7.84）：

1. `index.html` 第 1483 行引用了 `<script src="water_sources_manifest.js"></script>`，
   但该文件在现网源码包中**不存在**（assets 目录只有 `index.html`、`brigade_organization.js`、`cloud_config.js`）。
2. 现网代码通过 `window.WATER_SOURCE_MANIFEST` 数组读取内置水源，运行时该数组为 undefined，
   `getBuiltInWaterSources()` 实际返回空列表（水氺数据主要来自各单位检查记录表导入与 R2 云端同步）。
3. 因此**没有可解析的内置水源数据**，本期零条进库。

## 已迁入能力（不受影响）

水源数据落库通道已就绪，后续数据可经以下途径进库：

- `POST /api/v1/stations/{id}/waters` 手工新建
- `POST /api/v1/stations/{id}/waters/import` 导入现网同列结构的检查记录表
  （表头兼容：类别 / 辖区队站 / 设置方式 / 地理位置 / 使用状态 / 是否报修 / 备注 / 经度 / 纬度）

## 放弃明细

| 项目 | 原因 |
|------|------|
| 现网内置水源清单（`WATER_SOURCE_MANIFEST`） | 源文件缺失，无数据可解析 |
| `PROTECTED_RESTORED_WATER_IDS` 强制保留清单（海河路 municipal_001、安顺路 municipal_027/028/029） | 依赖 R2 云端快照数据，本库无对应记录 |
| 水源照片及水印经纬度识别（`recognizeWaterCoordsInMemory`） | 照片存 COS 属 P7-2 范围，本期水源表不含照片字段 |
| 水源检查记录「汇总表」（`WATER_SUMMARY_TARGET` 北三区/南四区/外县目标数） | 属现网总部报表视图，一期需求未包含 |
| 水源审批流（新增/修改/删除申请） | 规格明确「一期不做」 |

## 后续补救建议

若后续拿到 `water_sources_manifest.js` 原始文件，其结构为
`[{station, name, type, address, lng, lat, status, notes, ...}]`，
可直接用检查记录表导入或编写一次性脚本写入 `ccds_water_source` 表（type 归一：
`消防水鹤→crane`、`地上消火栓→ground`、`地下消火栓→underground`；status 归一：
`好用→active`、`不好用/报修→repair`）。
