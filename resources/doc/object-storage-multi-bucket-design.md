# 对象存储多 Bucket 方案与开发步骤

## 1. 背景与目标

当前系统已接入阿里云 OSS，头像上传使用公共读 Bucket，前端可以直接展示对象存储返回的永久访问地址。后续如果增加合同、证件、内部附件、导入导出文件等私有文件，不能继续复用公共读头像方案，否则会带来数据泄露、越权访问和审计缺失风险。

本方案目标：

- 公共文件与私有文件物理隔离，避免权限误配置造成敏感文件公开。
- 公共头像继续支持永久 URL 直接展示。
- 私有文件只保存文件标识和对象 Key，访问时由后端鉴权后生成临时签名 URL 或代理下载。
- 文件服务保持统一抽象，后续可以扩展临时 Bucket、生命周期清理、CDN、自定义域名和多云实现。

## 2. 总体设计

推荐采用多 Bucket 模型：

| Bucket 类型 | 权限 | 典型文件 | 访问方式 |
| --- | --- | --- | --- |
| public | 公共读 | 头像、公开图片、Logo | 数据库保存永久 file_url，前端直接访问 |
| private | 私有读写 | 合同、证件、内部附件 | 数据库保存 object_key，后端鉴权后生成临时签名 URL |
| temp | 私有读写，可选 | 导入临时文件、未确认文件 | 配置生命周期自动清理，确认后转存 |

核心原则：

- 公共文件可以保存长期可访问 URL。
- 私有文件不保存长期可访问 URL，只保存 `fileId`、`bucketType`、`bucketName`、`objectKey` 等元数据。
- 前端不能直接传 `objectKey` 下载私有文件，必须传 `fileId`，由后端查询文件与业务关系后做权限判断。
- OSS SDK 只负责生成私有对象临时访问地址，不负责业务用户是否有权访问该文件。

## 3. 配置方案

建议将单 Bucket 配置升级为多 Bucket 配置。

```yaml
app:
  object-storage:
    enabled: true
    provider: aliyun
    aliyun:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:}
      access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
      key-prefix: uploads
      public-bucket: ${ALIYUN_OSS_PUBLIC_BUCKET:ai-quick-web}
      public-domain: ${ALIYUN_OSS_PUBLIC_DOMAIN:https://ai-quick-web.oss-cn-hangzhou.aliyuncs.com}
      private-bucket: ${ALIYUN_OSS_PRIVATE_BUCKET:}
      private-url-expire-seconds: ${ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS:600}
      temp-bucket: ${ALIYUN_OSS_TEMP_BUCKET:}
      temp-file-expire-days: ${ALIYUN_OSS_TEMP_FILE_EXPIRE_DAYS:1}
```

推荐环境变量：

```bash
APP_OBJECT_STORAGE_ENABLED=true
ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_PUBLIC_BUCKET=ai-quick-web
ALIYUN_OSS_PUBLIC_DOMAIN=https://ai-quick-web.oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_PRIVATE_BUCKET=ai-quick-web-private
ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS=600
```

生产环境建议：

- AccessKey 不写入仓库，只通过环境变量、KMS、容器 Secret 或云平台密钥管理注入。
- 公共 Bucket 只存可公开资源。
- 私有 Bucket 禁止公共读。
- 临时 Bucket 配置生命周期规则，自动删除过期对象。

## 4. 数据库设计

建议在 `sys_file` 基础上补充多 Bucket 与访问策略字段。

```sql
ALTER TABLE sys_file
    ADD COLUMN bucket_type VARCHAR(32) NULL COMMENT 'Bucket类型：public/private/temp' AFTER storage_type,
    ADD COLUMN bucket_name VARCHAR(128) NULL COMMENT '实际Bucket名称' AFTER bucket_type,
    ADD COLUMN access_policy VARCHAR(32) NULL COMMENT '访问策略：public_read/private_read' AFTER bucket_name,
    ADD COLUMN business_type VARCHAR(64) NULL COMMENT '关联业务类型' AFTER status,
    ADD COLUMN business_id BIGINT NULL COMMENT '关联业务ID' AFTER business_type,
    ADD COLUMN expire_time DATETIME NULL COMMENT '临时文件过期时间' AFTER business_id;
```

字段使用规则：

| 字段 | 公共文件 | 私有文件 |
| --- | --- | --- |
| bucket_type | public | private |
| bucket_name | 公共 Bucket 名 | 私有 Bucket 名 |
| object_key | 必填 | 必填 |
| file_url | 可保存永久 URL | 为空 |
| access_policy | public_read | private_read |
| business_type | 可选 | 建议必填 |
| business_id | 可选 | 建议必填 |

如果文件还没有绑定业务单据，可以先保存为临时文件，业务确认后再更新为私有文件并绑定业务关系。

## 5. 后端抽象设计

建议将 OSS 操作抽象为统一服务，避免 Controller 或业务 Service 直接依赖 OSS SDK。

核心接口建议：

```java
public interface ObjectStorageService {

    FileUploadResult uploadPublic(MultipartFile file, String scene);

    FileUploadResult uploadPrivate(MultipartFile file, String scene);

    String generatePrivateDownloadUrl(String bucketName, String objectKey, long expireSeconds);

    InputStream getObjectStream(String bucketName, String objectKey);

    void deleteObject(String bucketName, String objectKey);
}
```

阿里云 OSS SDK 私有下载签名能力：

```java
Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
return url.toString();
```

注意事项：

- `generatePresignedUrl` 是 OSS SDK 提供的私有对象临时访问能力。
- 签名 URL 只表示持有 URL 的人在过期前可以访问对象。
- 生成签名 URL 前必须先做业务权限校验。

## 6. 接口设计

公共头像上传：

```text
POST /api/file/avatar/upload
```

返回：

```json
{
  "id": 1,
  "fileUrl": "https://ai-quick-web.oss-cn-hangzhou.aliyuncs.com/uploads/avatar.png"
}
```

私有文件上传：

```text
POST /api/file/private/upload
```

返回：

```json
{
  "id": 1001,
  "fileCode": "FILE202604230001",
  "originalName": "contract.pdf",
  "sizeBytes": 204800
}
```

私有文件获取临时下载地址：

```text
GET /api/file/private/{fileId}/download-url
```

返回：

```json
{
  "downloadUrl": "https://bucket.oss-cn-hangzhou.aliyuncs.com/private/contract.pdf?Expires=...",
  "expireSeconds": 600
}
```

高敏文件代理下载：

```text
GET /api/file/private/{fileId}/download
```

该模式由后端读取 OSS 文件流并写回 HTTP Response，签名 URL 不暴露给前端。缺点是后端承担文件带宽和连接压力。

## 7. 权限与审计设计

私有文件下载必须做两层校验：

- 接口级权限：判断当前用户是否具备调用私有文件下载接口的权限。
- 数据级权限：判断当前用户是否能访问该 `fileId` 绑定的业务数据。

推荐流程：

```text
前端请求 fileId
    -> JWT 识别当前用户
    -> 查询 sys_file
    -> 校验文件未删除、状态有效、bucket_type=private
    -> 根据 business_type + business_id 调用业务权限服务
    -> 记录下载审计日志
    -> OSS SDK 生成临时签名 URL 或后端代理下载
```

不要让前端传 `objectKey` 下载文件，否则容易发生枚举和越权访问。

建议新增权限服务：

```java
public interface FilePermissionService {

    void checkReadable(LoginUser currentUser, FileRecord fileRecord);

    void checkWritable(LoginUser currentUser, FileRecord fileRecord);
}
```

审计日志建议记录：

- 用户 ID、用户名、IP、User-Agent
- fileId、fileCode、businessType、businessId
- 下载方式：signed_url 或 proxy_stream
- 签名 URL 过期秒数
- 成功或失败原因

## 8. Hash 去重策略

公共文件：

- 可以继续按 `content_hash` 全局复用 OSS 对象。
- 头像、公开图片重复上传时可以减少 OSS 存储和 PUT 次数。

私有文件：

- 默认不做跨用户、跨业务全局秒传。
- 可以复用物理对象，但必须为每次上传创建独立 `sys_file` 记录和独立业务授权关系。
- 高敏文件不建议跨用户 hash 秒传，避免通过 hash 命中间接推断别人是否上传过同一文件。

推荐策略：

| 文件类型 | Hash 复用 |
| --- | --- |
| 公共头像 | 允许全局复用 |
| 普通私有附件 | 可复用物理对象，但授权记录必须独立 |
| 高敏文件 | 不建议复用 |
| 临时文件 | 不复用，过期清理 |

## 9. 限流与安全

上传接口需要保留现有双维度限流：

- IP 维度：限制同一 IP 的上传频率。
- 用户维度：限制同一登录用户的上传频率。

私有下载建议补充限流：

- 同一用户下载频率。
- 同一 fileId 下载频率。
- 同一 IP 下载频率。

安全要求：

- 限制文件大小。
- 校验文件扩展名和 Content-Type。
- 私有文件禁止返回永久 URL。
- 签名 URL 有效期建议 5 到 10 分钟。
- 高敏文件优先使用后端代理下载。
- 删除文件时优先软删除数据库记录，异步删除 OSS 对象，避免误删不可恢复。

## 10. 详细开发步骤

### 阶段一：配置与属性类

1. 扩展对象存储配置，支持 `publicBucket`、`publicDomain`、`privateBucket`、`privateUrlExpireSeconds`、`tempBucket`。
2. 保留 `enabled=false` 行为，不开启对象存储时公共头像仍使用用户名首字展示。
3. 启动时校验：开启对象存储后 `endpoint`、AccessKey、公共 Bucket 必填；启用私有文件功能时私有 Bucket 必填。

### 阶段二：数据库升级

1. 新增 SQL 迁移脚本，扩展 `sys_file` 多 Bucket 字段。
2. 将历史头像文件迁移为 `bucket_type=public`、`access_policy=public_read`。
3. 私有文件历史数据如果不存在，可暂不迁移，后续新增功能时按新字段写入。

### 阶段三：对象存储服务封装

1. 扩展 `ObjectStorageService`，区分公共上传和私有上传。
2. 阿里云实现类根据 Bucket 类型选择不同 Bucket。
3. 公共上传返回永久 `fileUrl`。
4. 私有上传只返回 `bucketName`、`objectKey`、文件元数据，不返回永久 URL。
5. 增加 `generatePrivateDownloadUrl`，内部调用 OSS SDK `generatePresignedUrl`。

### 阶段四：文件记录服务改造

1. `FileRecordService` 增加 `uploadPublic`、`uploadPrivate`、`createPrivateDownloadUrl`。
2. 公共头像上传继续写 `sys_user.avatar_url`。
3. 私有文件上传写入 `bucket_type=private`、`access_policy=private_read`、`file_url=null`。
4. 私有下载前查询 `sys_file` 并校验文件状态。

### 阶段五：权限服务

1. 新增 `FilePermissionService`。
2. 根据 `business_type` 路由到对应业务权限校验。
3. 未绑定业务的私有文件默认只允许创建人访问。
4. 下载失败时记录明确原因，日志级别使用 `info` 或 `error`。

### 阶段六：接口与前端接入

1. 保留现有头像上传接口。
2. 新增私有上传接口，只返回 `fileId` 和基础元数据。
3. 新增私有下载签名接口，返回短期 `downloadUrl`。
4. 前端私有附件列表只保存和展示 `fileId`、文件名、大小，不保存 OSS 永久 URL。
5. 点击下载时调用后端下载接口，不直接拼接 OSS 地址。

### 阶段七：测试与验证

1. 对公共头像上传做回归测试，确认 `sys_user.avatar_url` 是公共 OSS URL。
2. 对私有上传测试，确认 `sys_file.file_url` 为空。
3. 对未授权用户下载私有文件测试，确认返回无权限。
4. 对授权用户下载私有文件测试，确认签名 URL 可访问且过期后失效。
5. 对对象存储关闭场景测试，确认头像回退用户名首字。
6. 对限流场景测试，确认上传和下载超限返回明确错误。

## 11. 推荐落地顺序

1. 先完成配置和数据库字段扩展。
2. 再改造对象存储服务，支持公共 Bucket 与私有 Bucket。
3. 保持头像公共上传接口行为不变，降低现有功能回归风险。
4. 新增私有文件接口，不改动现有公共接口语义。
5. 最后补权限、审计、限流和前端私有附件下载入口。

该方案可以保证当前头像功能稳定，同时为后续私有文件、临时文件和高敏文件下载预留清晰扩展路径。
