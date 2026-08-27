# 阶段7开发完成说明

## 完成任务

### P7-1 值班日历 ✅
- **数据库**：`ccds_duty_day` 表
- **实体**：`DutyDayDO`
- **Mapper**：`DutyDayMapper` + XML
- **Service**：`DutyDayService` + `DutyDayServiceImpl`
- **Controller**：`DutyDayController`
- **DTO**：`DutyDayDTO`
- **功能**：
  - 按日期查询值班记录
  - 按月查询值班记录列表
  - 保存或更新值班记录（支持主官、副职、编组快照）
  - 删除值班记录

### P7-2 重点单位 + COS上传 + 预览 ✅
- **数据库**：
  - `ccds_key_unit` 表（重点单位）
  - `ccds_file_object` 表（文件对象）
- **实体**：`KeyUnitDO`、`FileObjectDO`
- **Mapper**：`KeyUnitMapper`、`FileObjectMapper` + XML
- **Service**：
  - `KeyUnitService` + `KeyUnitServiceImpl`
  - `FileObjectService` + `FileObjectServiceImpl`
  - `COSService` + `COSServiceImpl`（基础设施层）
- **Controller**：`KeyUnitController`、`FileController`
- **DTO**：`KeyUnitDTO`、`FileObjectDTO`、`FilePresignRequestDTO`、`FilePresignResponseDTO`
- **常量**：
  - `KeyUnitCategoryConstant`（7类重点单位）
  - `FileBizTypeConstant`（业务类型）
- **功能**：
  - 重点单位CRUD（支持7类：人员密集场所、高层建筑、地下建筑、危化品、文物古建、大跨度厂房、其他）
  - 按类别和关键词搜索
  - COS预签名上传URL生成
  - 文件预览URL生成
  - 支持预案文件和平面图关联
  - 软删除

### P7-3 天气代理 + 罗盘桥 ✅
- **基础设施**：`WeatherClient`（天气API客户端）
- **Service**：`WeatherService` + `WeatherServiceImpl`
- **Controller**：`WeatherController`
- **DTO**：`WeatherDTO`（含小时预报）
- **功能**：
  - 根据经纬度获取天气信息
  - 包含温度、风向、风速、湿度、气压、日出日落
  - 12小时预报
  - 后端代理，前端不直连第三方API
  - 当前为模拟实现，待接入真实天气API

### P7-4 共享令牌 + 对外只读页 ✅
- **数据库**：`ccds_share_token` 表
- **实体**：`ShareTokenDO`
- **Mapper**：`ShareTokenMapper` + XML
- **Service**：`ShareTokenService` + `ShareTokenServiceImpl`
- **Controller**：`ShareTokenController`
- **DTO**：`CreateShareTokenRequestDTO`、`CreateShareTokenResponseDTO`
- **VO**：`ShareAttackVO`（脱敏共享态势）
- **功能**：
  - 创建共享令牌（可设置过期时间，默认2小时）
  - 生成完整共享URL
  - 作废令牌
  - 匿名访问共享态势（`/s/{token}`）
  - 数据脱敏：不含电话、NFC号、账号信息
  - 按编组汇总人数和最差状态
  - 显示人员卡片：姓名、编组、气瓶、压力、剩余时间、状态
  - 令牌SHA256哈希存储

## 技术要点

### 架构遵守
- ✅ 严格遵守分层架构：`bootstrap → app → service → infrastructure → api`
- ✅ 接口在`service`，实现在`service.impl`
- ✅ Controller不直接访问Mapper
- ✅ Entity不直接返回前端，通过DTO/VO转换
- ✅ 包名符合规范：`com.ccds.duty.{业务域}.{分层}`

### 安全规范
- ✅ 所有SQL使用`#{}`参数化，无`${}`拼接
- ✅ 共享令牌使用SHA256哈希存储
- ✅ 共享态势VO脱敏，不含敏感字段
- ✅ COS密钥从环境变量读取，不硬编码
- ✅ 文件预签名URL有过期时间（1小时）

### 数据库规范
- ✅ 表名前缀`ccds_`
- ✅ 字段名`snake_case`
- ✅ 必备字段：`id`、`gmt_create`、`gmt_modified`
- ✅ 软删除使用`deleted_at`
- ✅ 唯一索引：`uk_station_date`、`uk_token_hash`
- ✅ 普通索引：`idx_station`、`idx_category`等

### 代码规范
- ✅ 使用Lombok：`@Data`、`@Builder`、`@RequiredArgsConstructor`、`@Slf4j`
- ✅ 日志使用占位符：`log.info("创建重点单位：stationId={}, name={}", ...)`
- ✅ 事务注解：`@Transactional(rollbackFor = Exception.class)`
- ✅ 参数校验：`@Valid`、`@NotNull`、`@NotBlank`、`@Size`等
- ✅ 中文Javadoc注释
- ✅ 异常使用`BizException`，带错误码

## 文件清单

### 数据库脚本
- `ccds-bootstrap/scripts/db/patch/rdb/0005_duty_plan_share/0001_duty_and_key_unit.sql`

### 实体（Entity）
- `ccds-service/src/main/java/com/ccds/duty/entity/DutyDayDO.java`
- `ccds-service/src/main/java/com/ccds/duty/entity/KeyUnitDO.java`
- `ccds-service/src/main/java/com/ccds/duty/entity/FileObjectDO.java`
- `ccds-service/src/main/java/com/ccds/duty/entity/ShareTokenDO.java`

### Mapper
- `ccds-service/src/main/java/com/ccds/duty/mapper/DutyDayMapper.java`
- `ccds-service/src/main/java/com/ccds/duty/mapper/KeyUnitMapper.java`
- `ccds-service/src/main/java/com/ccds/duty/mapper/FileObjectMapper.java`
- `ccds-service/src/main/java/com/ccds/duty/mapper/ShareTokenMapper.java`
- `ccds-bootstrap/src/main/resources/mapper/duty/DutyDayMapper.xml`
- `ccds-bootstrap/src/main/resources/mapper/duty/KeyUnitMapper.xml`
- `ccds-bootstrap/src/main/resources/mapper/duty/FileObjectMapper.xml`
- `ccds-bootstrap/src/main/resources/mapper/duty/ShareTokenMapper.xml`

### Service接口
- `ccds-service/src/main/java/com/ccds/duty/service/DutyDayService.java`
- `ccds-service/src/main/java/com/ccds/duty/service/KeyUnitService.java`
- `ccds-service/src/main/java/com/ccds/duty/service/FileObjectService.java`
- `ccds-service/src/main/java/com/ccds/duty/service/ShareTokenService.java`
- `ccds-service/src/main/java/com/ccds/duty/service/WeatherService.java`

### Service实现
- `ccds-service/src/main/java/com/ccds/duty/service/impl/DutyDayServiceImpl.java`
- `ccds-service/src/main/java/com/ccds/duty/service/impl/KeyUnitServiceImpl.java`
- `ccds-service/src/main/java/com/ccds/duty/service/impl/FileObjectServiceImpl.java`
- `ccds-service/src/main/java/com/ccds/duty/service/impl/ShareTokenServiceImpl.java`
- `ccds-service/src/main/java/com/ccds/duty/service/impl/WeatherServiceImpl.java`

### Controller
- `ccds-app/src/main/java/com/ccds/duty/controller/DutyDayController.java`
- `ccds-app/src/main/java/com/ccds/duty/controller/KeyUnitController.java`
- `ccds-app/src/main/java/com/ccds/duty/controller/FileController.java`
- `ccds-app/src/main/java/com/ccds/duty/controller/ShareTokenController.java`
- `ccds-app/src/main/java/com/ccds/duty/controller/WeatherController.java`

### DTO/VO
- `ccds-api/src/main/java/com/ccds/duty/dto/DutyDayDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/KeyUnitDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/FileObjectDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/FilePresignRequestDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/FilePresignResponseDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/CreateShareTokenRequestDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/CreateShareTokenResponseDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/dto/WeatherDTO.java`
- `ccds-api/src/main/java/com/ccds/duty/vo/ShareAttackVO.java`

### 常量
- `ccds-api/src/main/java/com/ccds/duty/constant/FileBizTypeConstant.java`
- `ccds-api/src/main/java/com/ccds/duty/constant/KeyUnitCategoryConstant.java`

### 基础设施
- `ccds-infrastructure/src/main/java/com/ccds/infra/cloud/cos/COSConfig.java`
- `ccds-infrastructure/src/main/java/com/ccds/infra/cloud/cos/COSService.java`
- `ccds-infrastructure/src/main/java/com/ccds/infra/cloud/cos/COSServiceImpl.java`
- `ccds-infrastructure/src/main/java/com/ccds/infra/weather/WeatherClient.java`

## API端点

### 值班日历
- `GET /api/v1/stations/{stationId}/duty/{date}` - 查询指定日期值班
- `GET /api/v1/stations/{stationId}/duty/month?year=2024&month=12` - 查询月度值班
- `PUT /api/v1/stations/{stationId}/duty` - 保存或更新值班
- `DELETE /api/v1/stations/{stationId}/duty/{date}` - 删除值班记录

### 重点单位
- `GET /api/v1/stations/{stationId}/key-units` - 查询列表（支持category、keyword参数）
- `GET /api/v1/stations/{stationId}/key-units/{id}` - 查询详情
- `POST /api/v1/stations/{stationId}/key-units` - 创建
- `PUT /api/v1/stations/{stationId}/key-units/{id}` - 更新
- `DELETE /api/v1/stations/{stationId}/key-units/{id}` - 删除

### 文件上传
- `POST /api/v1/files/presign` - 生成预签名上传URL
- `GET /api/v1/files/{fileId}` - 获取文件预览URL
- `DELETE /api/v1/files/{fileId}` - 删除文件

### 共享令牌
- `POST /api/v1/share` - 创建共享令牌
- `DELETE /api/v1/share/{tokenId}` - 作废令牌
- `GET /api/v1/s/{token}` - **匿名**访问共享态势

### 天气
- `GET /api/v1/weather?lng=116.4&lat=39.9` - 获取天气信息

## 待完成事项

1. **认证集成**：Controller中账号ID当前硬编码，需集成认证上下文获取当前用户
2. **权限校验**：需添加站级权限校验（确保只能访问本站数据）
3. **天气API接入**：`WeatherClient`当前为模拟实现，需接入真实天气服务（如和风天气、心知天气）
4. **罗盘桥**：罗盘功能在H5壳中实现，后端无需处理
5. **WebSocket推送**：共享页实时更新需要WebSocket或轮询机制
6. **文件类型校验**：文件上传需添加MIME类型白名单校验
7. **文件大小限制**：需添加文件大小限制配置
8. **配置externalize**：`shareBaseUrl`等配置需从配置文件读取

## 环境变量要求

需在`TencentCloudEnvConstant`中已定义的COS相关环境变量：
- `CCDS_COS_REGION` - COS地域
- `CCDS_COS_BUCKET` - COS桶名
- `CCDS_COS_SECRET_ID` - COS SecretId
- `CCDS_COS_SECRET_KEY` - COS SecretKey

## 测试建议

1. **单元测试**：为Service层编写单元测试
2. **集成测试**：测试COS上传下载流程
3. **权限测试**：测试跨站访问拦截
4. **共享令牌测试**：测试过期、作废场景
5. **数据脱敏测试**：验证共享态势VO不含敏感字段

## 依赖版本

- `com.qcloud:cos_api:5.6.89` - 腾讯云COS SDK
- Spring Boot Web（用于RestTemplate）
