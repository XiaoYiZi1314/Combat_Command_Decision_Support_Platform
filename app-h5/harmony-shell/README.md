# 鸿蒙薄壳（一期降级）

同一份 H5。桥对 NFC / 定位 / 罗盘返回 `UNSUPPORTED`，H5 降级为手选花名册与提示。后续再补 NFC Kit。

把 `app-h5/dist` 拷到 `entry/src/main/resources/rawfile/www/` 后用 DevEco 打开本目录出 HAP。

打包前把 `entry/src/main/ets/config/HostConfig.ets` 的 `API_BASE` 改成本环境后端根地址（与安卓 `ccds.apiBase` 相同）。一期不实现鸿蒙 NFC / 定位 / 罗盘 / 打开外链。
