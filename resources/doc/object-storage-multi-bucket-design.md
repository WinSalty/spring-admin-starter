# 文件存储方案与开发步骤

## 1. 背景与目标

当前系统支持两种文件存储 Provider：本地存储和阿里云 OSS。阿里云 OSS 不是系统运行的必需依赖，未配置 OSS 时系统使用本地存储兜底；生产环境推荐启用 OSS，并统一使用私有 Bucket，避免公开 Bucket、永久外链和公网盗刷风险。

本方案目标：

- 文件上传、头像展示、下载接口对业务和前端保持无感。
- 本地存储作为默认兜底方案，适合开发、单机测试和未配置 OSS 的环境。
- 阿里云 OSS 作为推荐生产方案，所有云端文件统一写入私有 Bucket。
- OSS 文件不保存永久外链，访问时由后端按文件编号生成短期签名 URL。
- 本地文件和 OSS 文件可长期混合存在，历史文件按 `sys_file.storage_type` 路由访问。

## 2. 总体设计

系统保留 `bucket_type` 作为业务访问分类，但不再使用多个 OSS Bucket：

| bucket_type | 业务含义 | 存储位置 | 访问方式 |
| --- | --- | --- | --- |
| public | 头像、公开图片、Logo | 本地 public 目录或 OSS 私有 Bucket | 本地返回 `/api/file/public/**`，OSS 返回 `/api/file/avatar/{fileId}` 等后端受控地址 |
| private | 合同、证件、内部附件 | 本地 private 目录或 OSS 私有 Bucket | 前端传 `fileId`，后端鉴权后返回签名 URL 或代理下载 |
| temp | 临时上传、导入中间文件 | 本地 temp 目录或 OSS 私有 Bucket | 按业务确认或清理策略处理 |

核心原则：

- `APP_OBJECT_STORAGE_ENABLED=false` 表示新文件写入本地存储，不关闭文件上传能力。
- `APP_OBJECT_STORAGE_ENABLED=true` 表示新文件写入阿里云 OSS 私有 Bucket。
- `sys_file.storage_type` 是读取、下载、删除历史文件的权威依据。
- OSS 文件记录统一保存 `access_policy=private_read`，`file_url` 为空。
- 头像上传后，云存储模式下前端保存 `/api/file/avatar/{fileId}`，不保存 OSS URL。
- 私有文件下载只接受 `fileId`，前端不能传 `objectKey`。

## 3. 配置方案

```yaml
app:
  file:
    upload-dir: uploads
    object-storage:
      enabled: ${APP_OBJECT_STORAGE_ENABLED:false}
      provider: aliyun-oss
      aliyun:
        endpoint: ${ALIYUN_OSS_ENDPOINT:}
        access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:}
        access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
        private-bucket: ${ALIYUN_OSS_PRIVATE_BUCKET:}
        private-url-expire-seconds: ${ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS:600}
        key-prefix: ${ALIYUN_OSS_KEY_PREFIX:uploads}
      local:
        root-path: ${LOCAL_STORAGE_ROOT_PATH:${app.file.upload-dir}}
        public-base-url: ${LOCAL_STORAGE_PUBLIC_BASE_URL:/api/file/public}
        private-url-expire-seconds: ${LOCAL_STORAGE_PRIVATE_URL_EXPIRE_SECONDS:600}
```

OSS 模式最小环境变量：

```bash
APP_OBJECT_STORAGE_ENABLED=true
ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY_ID=
ALIYUN_OSS_ACCESS_KEY_SECRET=
ALIYUN_OSS_PRIVATE_BUCKET=ai-web-private
```

OSS 可选环境变量：

```bash
ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS=600
ALIYUN_OSS_KEY_PREFIX=uploads
```

本地存储环境变量：

```bash
APP_OBJECT_STORAGE_ENABLED=false
LOCAL_STORAGE_ROOT_PATH=/data/spring-admin-starter/uploads
LOCAL_STORAGE_PUBLIC_BASE_URL=/api/file/public
LOCAL_STORAGE_PRIVATE_URL_EXPIRE_SECONDS=600
```

生产环境要求：

- AccessKey 不写入仓库，只通过环境变量、容器 Secret、KMS 或云平台密钥管理注入。
- OSS Bucket 必须为私有读写，不开启公共读。
- 本地存储根目录必须挂载到持久化磁盘。
- 多实例部署且未启用 OSS 时，本地存储必须使用 NAS、NFS 或其他共享文件系统。

## 4. 数据库设计

`sys_file` 关键字段：

| 字段 | 说明 |
| --- | --- |
| storage_type | 存储 Provider：`local` 或 `aliyun-oss` |
| bucket_type | 业务分类：`public`、`private`、`temp` |
| bucket_name | OSS 私有 Bucket 名或本地逻辑空间，例如 `local-public` |
| access_policy | 访问策略，OSS 统一为 `private_read` |
| object_key | 存储对象 Key |
| file_url | 本地 public 文件可保存 `/api/file/public/**`，OSS 文件为空 |
| content_hash | SHA-256 内容 Hash，用于去重和迁移校验 |

典型记录：

| 场景 | storage_type | bucket_type | bucket_name | access_policy | file_url |
| --- | --- | --- | --- | --- | --- |
| 本地头像 | local | public | local-public | public_read | `/api/file/public/public/avatar/ab/hash.png` |
| OSS 头像 | aliyun-oss | public | ai-web-private | private_read | 空 |
| 本地私有附件 | local | private | local-private | private_read | 空 |
| OSS 私有附件 | aliyun-oss | private | ai-web-private | private_read | 空 |

## 5. 接口设计

头像上传：

```text
POST /api/file/avatar/upload
```

本地模式返回：

```json
{
  "id": "1",
  "storageType": "local",
  "bucketType": "public",
  "fileUrl": "/api/file/public/public/avatar/ab/hash.png"
}
```

OSS 模式返回：

```json
{
  "id": "1",
  "storageType": "aliyun-oss",
  "bucketType": "public",
  "fileUrl": ""
}
```

前端保存头像地址时：

- 本地模式使用返回的 `fileUrl`。
- OSS 模式使用 `/api/file/avatar/{id}`。

头像访问：

```text
GET /api/file/avatar/{id}
```

访问逻辑：

- `storage_type=local`：后端读取本地文件流返回。
- `storage_type=aliyun-oss`：后端使用 OSS SDK 生成短期签名 URL，然后 302 跳转。

私有文件上传：

```text
POST /api/file/private/upload
```

私有文件下载地址：

```text
GET /api/file/private/{id}/download-url
```

返回：

```json
{
  "fileId": "1001",
  "downloadUrl": "https://bucket.oss-cn-hangzhou.aliyuncs.com/private/a.pdf?Expires=...",
  "expireSeconds": 600,
  "downloadMode": "signed_url"
}
```

本地私有文件返回后端代理下载地址：

```json
{
  "fileId": "1001",
  "downloadUrl": "/api/file/private/1001/download",
  "expireSeconds": 600,
  "downloadMode": "proxy_stream"
}
```

## 6. 后端实现要点

上传写入 Provider 选择：

```text
APP_OBJECT_STORAGE_ENABLED=false -> LocalFileStorageProvider
APP_OBJECT_STORAGE_ENABLED=true  -> AliyunOssStorageProvider
```

历史文件读取 Provider 选择：

```text
sys_file.storage_type=local      -> LocalFileStorageProvider
sys_file.storage_type=aliyun-oss -> AliyunOssStorageProvider
```

阿里云 OSS 签名访问：

```java
Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
return url.toString();
```

安全要求：

- 本地文件读取前必须校验规范化路径仍位于 `LOCAL_STORAGE_ROOT_PATH` 下。
- `/api/file/public/**` 只能读取 `bucket_type=public` 且状态有效的本地文件。
- OSS 文件不得返回永久 URL。
- 私有文件下载必须先做接口权限和数据权限校验。
- 文件上传必须保留扩展名、MIME、魔数、大小限制和限流。

## 7. Hash 去重策略

公共头像、公开图片可以按 `storage_type + bucket_type + access_policy + content_hash` 复用物理对象，减少重复上传和存储占用。业务层仍新增独立 `sys_file` 记录，保留上传人、原始文件名和审计时间。

私有文件默认不建议跨用户做全局秒传。若后续需要复用物理对象，必须保证每条业务文件记录独立授权，避免通过 Hash 命中推断别人是否上传过同一文件。

## 8. 存量数据与存储切换

系统必须支持以下场景：

| 切换路径 | 结果要求 |
| --- | --- |
| 本地存储切 OSS | 历史本地文件继续可访问，新文件写入 OSS |
| OSS 切本地存储 | 历史 OSS 文件继续可访问，新文件写入本地 |
| 反复切换 | 每条文件按自身 `storage_type` 访问 |

本地迁移到 OSS 流程：

1. 查询 `storage_type=local` 的待迁移文件。
2. 根据 `LOCAL_STORAGE_ROOT_PATH + object_key` 读取本地文件。
3. 上传到 OSS 私有 Bucket，生成新的 `object_key`。
4. 校验文件大小和 `content_hash` 一致。
5. 更新 `sys_file.storage_type=aliyun-oss`、`bucket_name=ai-web-private`、`object_key`、`file_url=''`、`access_policy=private_read`。
6. 如果是头像，同步更新 `sys_user.avatar_url=/api/file/avatar/{fileId}`。
7. 源文件保留观察期，确认无回滚需求后再清理。

迁移安全要求：

- 迁移任务必须支持断点续跑。
- 迁移前后必须校验 `content_hash`。
- 迁移失败不得删除源文件。
- 迁移期间读取逻辑以数据库当前记录为准。

## 9. 开发步骤

1. 配置 `ObjectStorageProperties`，保留本地和 OSS 两类 Provider 配置。
2. 使用 `APP_OBJECT_STORAGE_ENABLED` 决定新上传文件写入本地还是 OSS。
3. 上传后写入 `sys_file.storage_type`、`bucket_type`、`bucket_name`、`access_policy`、`object_key`、`content_hash`。
4. OSS 上传统一写入 `ALIYUN_OSS_PRIVATE_BUCKET`。
5. OSS 访问统一通过后端接口生成短期签名 URL。
6. 本地 public 文件通过 `/api/file/public/**` 输出。
7. 头像前端只保存后端可访问地址，本地为 `/api/file/public/**`，OSS 为 `/api/file/avatar/{id}`。
8. 下载、删除、迁移都按 `sys_file.storage_type` 路由。

## 10. 测试验证

1. `APP_OBJECT_STORAGE_ENABLED=false` 时，头像上传后返回 `/api/file/public/**` 并可展示。
2. `APP_OBJECT_STORAGE_ENABLED=true` 时，头像上传到 OSS 私有 Bucket，`sys_file.file_url` 为空，`sys_user.avatar_url` 为 `/api/file/avatar/{id}`。
3. 浏览器访问 `/api/file/avatar/{id}` 时，OSS 文件会 302 到短期签名 URL。
4. 私有文件下载未授权时返回无权限。
5. 私有文件授权后返回签名 URL 或本地代理下载地址。
6. 切换本地和 OSS 配置后，历史文件仍按 `sys_file.storage_type` 正确访问。
7. 路径穿越请求不能读取本地根目录外文件。
8. 上传限流、文件大小、MIME、魔数校验正常生效。
