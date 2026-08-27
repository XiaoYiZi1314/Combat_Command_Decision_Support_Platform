# 阶段7快速参考

## 数据库脚本位置
```
ccds-bootstrap/scripts/db/patch/rdb/0005_duty_plan_share/0001_duty_and_key_unit.sql
```

## 环境变量要求
```bash
# COS配置（已在TencentCloudEnvConstant中定义）
CCDS_COS_REGION=ap-guangzhou
CCDS_COS_BUCKET=your-bucket-name
CCDS_COS_SECRET_ID=your-secret-id
CCDS_COS_SECRET_KEY=your-secret-key
```

## API快速测试

### 1. 值班日历
```bash
# 查询指定日期
GET /api/v1/stations/1/duty/2024-12-25

# 查询月度
GET /api/v1/stations/1/duty/month?year=2024&month=12

# 保存
PUT /api/v1/stations/1/duty
{
  "stationId": 1,
  "dutyDate": "2024-12-25",
  "mainOfficerId": 101,
  "deputyOfficerId": 102,
  "note": "节日值班"
}
```

### 2. 重点单位
```bash
# 列表（支持category、keyword参数）
GET /api/v1/stations/1/key-units
GET /api/v1/stations/1/key-units?category=人员密集场所
GET /api/v1/stations/1/key-units?keyword=商场

# 详情
GET /api/v1/stations/1/key-units/1

# 创建
POST /api/v1/stations/1/key-units
{
  "name": "XX购物中心",
  "address": "XX区XX路100号",
  "category": "人员密集场所",
  "contact": "张三",
  "phone": "13800138000",
  "notes": "节假日人流量大",
  "planText": "# 灭火救援预案\n..."
}
```

### 3. 文件上传（两步）
```bash
# 步骤1: 获取预签名URL
POST /api/v1/files/presign
{
  "bizType": "keyunit_plan",
  "bizId": 1,
  "fileName": "plan.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 1048576
}

# 返回
{
  "fileId": 1,
  "cosKey": "keyunit_plan/20241225/abc123.pdf",
  "uploadUrl": "https://...",
  "expiresIn": 3600
}

# 步骤2: 前端直接PUT到uploadUrl（不经过后端）
PUT {uploadUrl}
Content-Type: application/pdf
Body: <文件内容>

# 步骤3: 获取预览URL
GET /api/v1/files/1
```

### 4. 共享令牌
```bash
# 创建共享
POST /api/v1/share
{
  "stationId": 1,
  "expireHours": 2
}

# 返回
{
  "tokenId": 1,
  "token": "abc123def456...",
  "shareUrl": "http://localhost:8080/s/abc123def456...",
  "expireAt": "2024-12-25T18:00:00"
}

# 匿名访问（无需认证）
GET /api/v1/s/abc123def456...

# 作废令牌
DELETE /api/v1/share/1
```

### 5. 天气
```bash
GET /api/v1/weather?lng=116.4&lat=39.9
```

## 业务类型常量
```java
FileBizTypeConstant.WATER_PHOTO      // 水源照片
FileBizTypeConstant.WATER_DOC        // 水源档案
FileBizTypeConstant.KEYUNIT_PLAN     // 重点单位预案
FileBizTypeConstant.KEYUNIT_FLOOR    // 重点单位平面图
FileBizTypeConstant.HAZMAT_IMAGE     // 危化品图片
```

## 重点单位类别
```java
KeyUnitCategoryConstant.CROWDED_PLACE   // 人员密集场所
KeyUnitCategoryConstant.HIGH_RISE       // 高层建筑
KeyUnitCategoryConstant.UNDERGROUND     // 地下建筑
KeyUnitCategoryConstant.HAZMAT          // 危险化学品
KeyUnitCategoryConstant.CULTURAL_RELIC  // 文物古建
KeyUnitCategoryConstant.LARGE_SPAN      // 大跨度厂房
KeyUnitCategoryConstant.OTHER           // 其他
```

## 共享态势VO结构
```json
{
  "stationName": "XX消防站",
  "lastUpdateTime": "2024-12-25T15:30:00",
  "groups": [
    {
      "groupName": "第一攻坚组",
      "count": 3,
      "worstStatus": "warn"
    }
  ],
  "persons": [
    {
      "name": "张三",
      "groupName": "第一攻坚组",
      "cylType": "9L/30MPa",
      "currentPressure": 18.5,
      "remainSec": 780,
      "status": "warn"
    }
  ]
}
```

## 注意事项

1. **认证**：当前Controller中accountId硬编码为1L，需集成认证上下文
2. **权限**：需添加站级权限校验
3. **文件校验**：生产环境需添加MIME类型白名单和大小限制
4. **天气API**：WeatherClient当前返回模拟数据，需接入真实API
5. **共享URL**：shareBaseUrl配置在application.yml中设置

## 数据库注意
- 所有表已设置DEFAULT CURRENT_TIMESTAMP和ON UPDATE
- 软删除使用deleted_at字段
- 唯一索引确保数据一致性
- 外键约束需根据实际情况添加
