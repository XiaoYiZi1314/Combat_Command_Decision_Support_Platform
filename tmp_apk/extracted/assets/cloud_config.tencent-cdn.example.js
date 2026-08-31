window.NXGK_CLOUD_CONFIG = {
  // 腾讯云 TDMQ MQTT WebSocket 地址，例如 wss://xxx.mqtt.tencenttdmq.com:443/mqtt
  endpoint: '',
  username: '',
  password: '',
  prefix: 'nxgk_inner_attack_cloud_v1',

  // 腾讯云 SCF/API 网关签名服务地址，部署 cloud/tencent-cos-sign 后填写。
  cosTokenUrl: '',

  // 与 cloud/tencent-cos-sign 的 SERVICE_KEY 保持一致。
  fileServiceKey: '',

  // 腾讯云 COS/CDN 不需要在客户端填写永久密钥，下面 R2 字段保持为空。
  r2Endpoint: '',
  r2Bucket: '',
  r2AccessKeyId: '',
  r2SecretAccessKey: '',
  r2PublicBaseUrl: '',
  r2ShareWorkerUrl: ''
};
