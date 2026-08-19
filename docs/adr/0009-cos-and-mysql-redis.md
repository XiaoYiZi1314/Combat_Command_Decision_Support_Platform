# 文件走 COS，业务库用 MySQL 8，实时通道用 Redis

预案、水源照片、共享页静态资源和短时下载地址放腾讯云 COS，密钥只在后端。业务表用 MySQL 8，前缀 `ccds_`。WebSocket 会话和离线补传队列用 Redis。不把文件塞进数据库，也不继续用 R2 / 旧 `nxgk-file`。
