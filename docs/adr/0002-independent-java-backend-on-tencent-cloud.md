# 腾讯云上的全新 Java 独立后端

现网把 R2 密钥、文件服务地址和 AI Key 写在 APK 里，数据主要在设备 `localStorage`。一期新建符合仓库分层（`bootstrap → app → service → infrastructure → api`）的 Java 后端，部署在腾讯云；APP 只调我们的 API。不沿用 `119.45.196.24` 旧文件协议，也不再让前端直连对象存储。
