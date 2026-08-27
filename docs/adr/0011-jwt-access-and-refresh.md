# 会话用 JWT 访问令牌加短 refresh

APP 与指挥端都是无 PC 的多设备客户端，CVM 上先跑单体也不适合把会话只钉在单机内存。访问令牌短寿，refresh 换新；refresh 失效则重登。不选纯服务端 Session，避免壳与 WebView 多端续期绑死一台机器。
