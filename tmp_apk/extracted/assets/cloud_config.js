// Optional cloud defaults. UI keeps the existing Tencent Cloud wording, while storage uses R2.
window.NXGK_CLOUD_CONFIG = window.NXGK_CLOUD_CONFIG || {
  endpoint: '',
  username: '',
  password: '',
  prefix: 'nxgk_inner_attack_cloud_v1',
  cosTokenUrl: '',
  fileServiceUrl: '',
  fileServiceKey: 'nxgk_f90200da92a944a9bb3b3506c42f45d1',
  r2Endpoint: 'https://51f5e121cef646ebec251bfb154a5f83.r2.cloudflarestorage.com',
  r2Bucket: 'nxgk-files',
  r2AccessKeyId: 'd98591bfba8112c07f040867257083e1',
  r2SecretAccessKey: 'd1bc1119b43cc5ca2480a69e95b150abafd6a6dea03e82bff6d3644b822f10d9',
  r2PublicBaseUrl: 'https://pub-1dd80a45875a4c56b928d7fc95f6f676.r2.dev',
  r2ShareWorkerUrl: 'https://nxgk-share.nxgk-yuhao.workers.dev'};

if(!window.NXGK_CLOUD_CONFIG.cosTokenUrl)window.NXGK_CLOUD_CONFIG.cosTokenUrl='http://119.45.196.24:8080/nxgk-file';
if(!window.NXGK_CLOUD_CONFIG.fileServiceUrl)window.NXGK_CLOUD_CONFIG.fileServiceUrl='http://119.45.196.24:8080/nxgk-file';
