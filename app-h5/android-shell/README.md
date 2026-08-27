# 安卓薄壳

承载 `app-h5/dist`，只提供 NFC / 定位 / 罗盘 / 打开外链。业务界面全部在 H5。

## 打包

在 `app-h5` 目录：

```
npm run build
npm run pack:android
```

`pack:android` 把 `dist/` 拷到 `android-shell/app/src/main/assets/www/`。

再用 Android Studio 打开 `android-shell/` 出 APK。

`local.properties` 只放本机 SDK 与 `ccds.apiBase`，见 `local.properties.example`。release 构建强制 `DEBUG_NFC=false`，正式包不含调试密钥。
