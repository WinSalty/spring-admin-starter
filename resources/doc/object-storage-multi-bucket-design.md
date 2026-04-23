# 对象存储多 Bucket 方案与开发步骤

## 1. 背景与目标

当前系统已接入阿里云 OSS，头像上传使用公共读 Bucket，前端可以直接展示对象存储返回的永久访问地址。后续如果增加合同、证件、内部附件、导入导出文件等私有文件，不能继续复用公共读头像方案，否则会带来数据泄露、越权访问和审计缺失风险。

本方案目标：

- 公共文件与私有文件物理隔离，避免权限误配置造成敏感文件公开。
- 公共头像继续支持永久 URL 直接展示。
- 私有文件只保存文件标识和对象 Key，访问时由后端鉴权后生成临时签名 URL 或代理下载。
- 文件服务保持统一抽象，后续可以扩展临时 Bucket、生命周期清理、CDN、自定义域名、本地存储和多云实现。
- 未启用云存储时自动使用本地存储替代方案，业务系统上传、展示、下载体验保持无感。

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
- 业务层只依赖统一文件服务接口，不感知底层是阿里云 OSS 还是本地磁盘。
- `object-storage.enabled=false` 不代表关闭文件上传能力，而是切换到本地存储 Provider。

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
    local:
      root-path: ${LOCAL_STORAGE_ROOT_PATH:./data/files}
      public-base-url: ${LOCAL_STORAGE_PUBLIC_BASE_URL:/api/file/public}
      private-url-expire-seconds: ${LOCAL_STORAGE_PRIVATE_URL_EXPIRE_SECONDS:600}
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

本地存储环境变量：

```bash
APP_OBJECT_STORAGE_ENABLED=false
LOCAL_STORAGE_ROOT_PATH=/data/quickstart/files
LOCAL_STORAGE_PUBLIC_BASE_URL=/api/file/public
LOCAL_STORAGE_PRIVATE_URL_EXPIRE_SECONDS=600
```

配置语义：

| 配置 | 含义 |
| --- | --- |
| `object-storage.enabled=true` | 使用云存储 Provider，例如阿里云 OSS |
| `object-storage.enabled=false` | 使用本地存储 Provider，不关闭文件能力 |
| `local.root-path` | 本地文件根目录，生产环境必须配置到持久化磁盘 |
| `local.public-base-url` | 本地公共文件访问入口，由后端静态资源映射或 Controller 输出 |
| `local.private-url-expire-seconds` | 本地私有文件临时访问令牌有效期 |

生产环境建议：

- AccessKey 不写入仓库，只通过环境变量、KMS、容器 Secret 或云平台密钥管理注入。
- 公共 Bucket 只存可公开资源。
- 私有 Bucket 禁止公共读。
- 临时 Bucket 配置生命周期规则，自动删除过期对象。
- 本地存储根目录必须挂载到持久化磁盘，不允许使用容器临时目录。
- 多实例部署时，本地存储必须使用共享文件系统、NAS、NFS 或统一文件服务，否则不同实例之间会出现文件不可见。

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
| bucket_name | 公共 Bucket 名或 local-public | 私有 Bucket 名或 local-private |
| object_key | 必填 | 必填 |
| file_url | 可保存永久 URL | 为空 |
| access_policy | public_read | private_read |
| business_type | 可选 | 建议必填 |
| business_id | 可选 | 建议必填 |

如果文件还没有绑定业务单据，可以先保存为临时文件，业务确认后再更新为私有文件并绑定业务关系。

本地存储时建议字段取值：

| 字段 | 示例 |
| --- | --- |
| storage_type | local |
| bucket_type | public/private/temp |
| bucket_name | local-public/local-private/local-temp |
| object_key | public/avatar/ab/hash.png |
| file_url | 公共文件为 `/api/file/public/public/avatar/ab/hash.png`，私有文件为空 |

本地存储不要把服务器绝对路径保存到 `file_url`，避免泄露部署目录，也避免迁移云存储时改动前端。

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

建议使用 Provider 适配器模式：

```java
public interface FileStorageProvider {

    String storageType();

    FileUploadResult upload(StorageUploadCommand command);

    String createPublicUrl(String objectKey);

    String createPrivateDownloadUrl(String objectKey, long expireSeconds);

    InputStream getObjectStream(String objectKey);

    void deleteObject(String objectKey);
}
```

Provider 选择规则：

| 条件 | Provider |
| --- | --- |
| `object-storage.enabled=true` 且 `provider=aliyun` | AliyunOssStorageProvider |
| `object-storage.enabled=false` | LocalFileStorageProvider |

业务 Service 只调用 `ObjectStorageService`，由其根据配置委托到具体 Provider。这样头像、附件、下载接口不用区分云存储和本地存储，前端体验保持一致。

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

本地存储私有下载能力：

- 本地公共文件可以通过 `/api/file/public/**` 映射读取，效果等同公共 URL。
- 本地私有文件不能暴露真实路径，必须通过 `/api/file/private/{fileId}/download` 由后端鉴权后流式输出。
- 如果需要本地临时下载 URL，可生成应用内签名令牌，例如 `/api/file/private/{fileId}/download?token=...`，令牌由后端 HMAC 签名并设置过期时间。
- 本地私有文件签名令牌只能代表短期访问凭证，生成前仍需业务权限校验。

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

本地存储关闭云服务时返回同结构：

```json
{
  "id": 1,
  "fileUrl": "/api/file/public/public/avatar/ab/hash.png"
}
```

前端仍然保存和展示 `fileUrl`，不需要知道文件来自 OSS 还是本地磁盘。

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

本地存储下私有文件默认使用代理下载模式，因为本地文件没有 OSS 这类外部签名 URL 能力。后端必须校验权限后读取 `local.root-path + object_key` 对应文件流，并写回 HTTP Response。

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
    -> 云存储生成临时签名 URL，或本地存储由后端代理下载
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
- 本地存储同样可以按 `content_hash` 复用磁盘文件，减少重复文件占用。

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
- 本地存储必须限制路径穿越，`object_key` 只能由后端生成，禁止接收前端传入的相对路径。
- 本地文件读取前必须校验规范化路径仍位于 `local.root-path` 下。
- 本地公共文件访问接口也要限制只允许读取 `public` 目录，不能读取 `private` 或 `temp` 目录。

## 10. 详细开发步骤

### 阶段一：配置与属性类

1. 扩展对象存储配置，支持 `publicBucket`、`publicDomain`、`privateBucket`、`privateUrlExpireSeconds`、`tempBucket`。
2. 增加本地存储配置，支持 `local.rootPath`、`local.publicBaseUrl`、`local.privateUrlExpireSeconds`。
3. 调整 `enabled=false` 语义：不启用云存储时自动使用本地存储，而不是关闭文件能力。
4. 启动时校验：启用云存储后 `endpoint`、AccessKey、公共 Bucket 必填；启用本地存储后 `rootPath` 必填且目录可创建或可写；启用私有云文件功能时私有 Bucket 必填。

### 阶段二：数据库升级

1. 新增 SQL 迁移脚本，扩展 `sys_file` 多 Bucket 字段。
2. 将历史头像文件迁移为 `bucket_type=public`、`access_policy=public_read`。
3. 私有文件历史数据如果不存在，可暂不迁移，后续新增功能时按新字段写入。

### 阶段三：对象存储服务封装

1. 扩展 `ObjectStorageService`，区分公共上传和私有上传。
2. 新增 `FileStorageProvider` 抽象，提供阿里云 OSS 与本地文件两个实现。
3. 阿里云实现类根据 Bucket 类型选择不同 Bucket。
4. 本地实现类根据 Bucket 类型写入 `local.rootPath/public`、`local.rootPath/private`、`local.rootPath/temp`。
5. 公共上传返回 `fileUrl`，云存储返回 OSS URL，本地存储返回 `/api/file/public/**`。
6. 私有上传只返回 `bucketName`、`objectKey`、文件元数据，不返回永久 URL。
7. 云存储增加 `generatePrivateDownloadUrl`，内部调用 OSS SDK `generatePresignedUrl`。
8. 本地存储私有下载优先使用后端代理流式输出；如需临时 URL，则生成应用内短期签名令牌。

### 阶段四：文件记录服务改造

1. `FileRecordService` 增加 `uploadPublic`、`uploadPrivate`、`createPrivateDownloadUrl`。
2. 公共头像上传继续写 `sys_user.avatar_url`，云存储写 OSS URL，本地存储写 `/api/file/public/**`。
3. 私有文件上传写入 `bucket_type=private`、`access_policy=private_read`、`file_url=null`。
4. 私有下载前查询 `sys_file` 并校验文件状态。
5. 所有业务层只依赖文件服务返回值，不根据 `storage_type` 写分支逻辑。

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
6. 前端头像展示继续使用 `fileUrl`，云存储和本地存储返回同一字段，业务体验无感。

### 阶段七：测试与验证

1. 对公共头像上传做回归测试，确认 `sys_user.avatar_url` 是公共 OSS URL。
2. 对关闭云存储场景做回归测试，确认头像上传后保存为 `/api/file/public/**` 且页面正常展示。
3. 对本地存储路径安全测试，确认不能通过 `../` 读取根目录外文件。
4. 对私有上传测试，确认 `sys_file.file_url` 为空。
5. 对未授权用户下载私有文件测试，确认返回无权限。
6. 对授权用户下载私有文件测试，确认云存储签名 URL 可访问且过期后失效。
7. 对授权用户下载本地私有文件测试，确认后端代理下载可用。
8. 对限流场景测试，确认上传和下载超限返回明确错误。

## 11. 推荐落地顺序

1. 先调整配置语义，将 `enabled=false` 明确为本地存储 Provider。
2. 新增本地存储实现，保证头像上传在无云存储时仍然可上传、保存和展示。
3. 完成数据库字段扩展，统一记录云存储和本地存储元数据。
4. 再改造对象存储服务，支持公共 Bucket、私有 Bucket 和本地 public/private/temp 目录。
5. 保持头像公共上传接口行为不变，降低现有功能回归风险。
6. 新增私有文件接口，不改动现有公共接口语义。
7. 最后补权限、审计、限流和前端私有附件下载入口。

该方案可以保证当前头像功能稳定，并确保未启用云存储时仍由本地存储提供同等上传、展示和下载能力，同时为后续私有文件、临时文件和高敏文件下载预留清晰扩展路径。
