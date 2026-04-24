# Spring Admin Starter

## 项目定位

`spring-admin-starter` 是一套面向后台管理系统的 Java 服务端基座，基于 Spring Boot 2.7 + Spring Security + MyBatis-Plus 构建，当前已具备认证鉴权、权限装配、工作台、查询管理、系统管理、公告通知、部门管理、参数配置、文件管理、缓存、日志审计、定时归档和 Elasticsearch 基础能力。

项目当前状态不再是“最小骨架”，而是已能作为前后端一体化管理平台的后端基础工程，与配套的 `react-admin-starter` 可直接联调。

## 技术架构

### 技术栈

| 类别 | 方案 |
| --- | --- |
| JDK | Java 8 |
| 应用框架 | Spring Boot 2.7.18 |
| 安全框架 | Spring Security |
| ORM / 数据访问 | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 5.7 / 8.0 |
| 缓存 | Redis |
| 连接池 | Druid |
| JSON 序列化 | fastjson2 |
| API 文档 | springdoc-openapi |
| 定时任务 | Quartz |
| 搜索能力 | Elasticsearch 7.x |
| 邮件 | Spring Mail |
| 文件存储 | 本地存储兜底，推荐接入阿里云 OSS 私有 Bucket |
| 构建工具 | Maven 3.6+ |

### 服务端分层

项目按领域模块和基础设施能力拆分，核心包结构如下：

| 包路径 | 说明 |
| --- | --- |
| `com.winsalty.quickstart.auth` | 登录注册、JWT、安全配置、用户上下文、限流、会话管理 |
| `com.winsalty.quickstart.permission` | 权限 bootstrap、角色授权、菜单权限与按钮权限 |
| `com.winsalty.quickstart.dashboard` | 工作台统计概览 |
| `com.winsalty.quickstart.query` | 查询管理示例业务模块 |
| `com.winsalty.quickstart.system` | 用户、角色、字典、菜单、系统配置等通用系统模块 |
| `com.winsalty.quickstart.notice` | 公告通知管理 |
| `com.winsalty.quickstart.department` | 部门树与部门状态管理 |
| `com.winsalty.quickstart.param` | 参数配置管理与缓存刷新 |
| `com.winsalty.quickstart.file` | 本地/阿里云 OSS 文件上传、下载、状态管理 |
| `com.winsalty.quickstart.log` | 登录日志、操作日志、接口日志与归档 |
| `com.winsalty.quickstart.common` | 统一响应、异常、常量、基础父类、工具类 |
| `com.winsalty.quickstart.infra` | CORS、Redis、Quartz、OpenAPI、ES、JSON、对象存储等基础设施配置 |

### 关键架构说明

1. 认证基于 JWT Bearer Token，登录返回 access token 与 refresh token。
2. refresh token 保存在 Redis，会话失效、轮换校验和退出登录都依赖 Redis 做统一控制。
3. 权限初始化通过 `/api/permission/bootstrap` 输出菜单树、路由码和按钮权限，直接服务前端动态权限装配。
4. 系统管理模块对用户、角色、字典、日志采用“统一控制器入口 + 服务层分发”的方式，减少重复 CRUD 模板代码。
5. 关键管理操作通过 `@AuditLog` + AOP 记录审计日志，异常链路写入异常日志。
6. 配置、字典、权限 bootstrap 等高频读取能力已接入 Redis 缓存。
7. 日志归档通过 Quartz 定时任务迁移历史日志到归档表，减少主表膨胀。
8. Elasticsearch 已完成基础接入和通用封装，可作为后续全文检索能力的扩展基础。

## 模块能力

### 认证与安全

| 模块 | 说明 |
| --- | --- |
| 登录 | `/api/auth/login`，支持用户名或邮箱 + 密码登录，返回 access token + refresh token |
| 刷新令牌 | `/api/auth/refresh-token`，支持 refresh token 轮换 |
| 退出登录 | `/api/auth/logout`，基于 Redis 失效当前会话 |
| 注册 | `/api/auth/register`，仅 `dev` 环境默认开放；提交后创建 `pending` 待激活账号并发送激活邮件 |
| 注册账号激活 | `POST /api/auth/register/verify-link` 校验邮件链接并激活账号，激活前无法登录 |
| 重发注册验证邮件 | `POST /api/auth/register/resend-verify-mail`，仅 pending 待激活账号可重新发送 |
| 个人中心 | `/api/auth/profile`、资料修改、密码修改、通知设置 |
| 登录限流 | 基于账号/IP 的匿名接口限流 |
| JWT 安全校验 | 生产环境要求外部注入强密钥 |

### 权限与菜单

| 模块 | 说明 |
| --- | --- |
| 权限初始化 | `/api/permission/bootstrap` 返回菜单、路由、按钮权限 |
| 角色权限分配 | `/api/permission/assignment` |
| 菜单树管理 | `/api/system/menus/tree`、`/save`、`/status` |
| 用户角色分配 | `/api/system/users/assign-roles` |

### 业务与系统模块

| 模块 | 说明 |
| --- | --- |
| 工作台 | `/api/dashboard/overview` 输出指标、趋势、分类与状态统计 |
| 查询管理 | `/api/query/list`、`/detail`、`/save` |
| 用户管理 | `/api/system/users/list` 等 |
| 角色管理 | `/api/system/roles/list` 等 |
| 字典管理 | `/api/system/dicts/list` 等 |
| 日志管理 | `/api/system/logs/list` 等 |
| 系统配置 | `/api/system/configs`、`/configs/save` |
| 公告通知 | `/api/system/notices/list`、`/detail`、`/save`、`/status`、`/active` |
| 部门管理 | `/api/system/departments/tree`、`/save`、`/status` |
| 参数配置 | `/api/system/params/list`、`/detail`、`/save`、`/status`、`/cache/refresh` |
| 文件管理 | `/api/file/upload`、`/avatar/upload`、`/biz/upload`、`/biz/{id}/download-url`、`/biz/{id}/download`、`/public/**`、`/private/upload`、`/private/{id}/download-url`、`/private/{id}/download`、`/avatar/{id}`、`/object-storage/status`、`/list`、`/{id}/download`、`/{id}/delete`、`/{id}/status` |

## 配套环境说明

### 必需环境

| 软件 | 要求 |
| --- | --- |
| JDK | 1.8 |
| Maven | 3.6 及以上 |
| MySQL | 5.7 或 8.0 |
| Redis | 5.x 及以上 |
| Elasticsearch | 7.x |

### 推荐联调环境

| 组件 | 默认说明 |
| --- | --- |
| 前端 | `react-admin-starter` |
| 前端地址 | `http://localhost:5173` |
| 后端服务 | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| 健康检查 | `http://localhost:8080/actuator/health` |

## 配置说明

### Profile 说明

| Profile | 说明 |
| --- | --- |
| `dev` | 本地开发环境，默认启用 |
| `test` | 测试环境 |
| `prod` | 生产环境，要求外部注入关键配置 |

### 核心配置文件

| 文件 | 说明 |
| --- | --- |
| `src/main/resources/application.yml` | 通用配置，包含端口、上传限制、对象存储、邮件、Quartz、ES 等 |
| `src/main/resources/application-dev.yml` | 开发环境数据库、Redis、JWT、CORS 配置 |
| `src/main/resources/application-prod.yml` | 生产环境数据库、Redis、JWT 与 CORS 外部化配置 |
| `src/main/resources/logback-spring.xml` | 日志配置 |

### 关键配置项

#### 1. 数据库

开发环境默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/spring_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: ${DB_USERNAME:spring_admin}
    password: ${DB_PASSWORD:SpringAdmin@2026}
```

#### 2. Redis

```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
```

Redis 当前承担以下职责：

1. 登录会话与 refresh token 控制
2. 权限 bootstrap 缓存
3. 字典缓存
4. 系统配置缓存
5. 匿名接口限流
6. 通用对象缓存

#### 3. JWT

开发环境默认值：

```yaml
app:
  security:
    jwt-secret: ${JWT_SECRET:winsalty-quickstart-dev-jwt-secret-2026-secure-key}
    jwt-expire-seconds: 7200
    jwt-refresh-expire-seconds: 604800
```

生产环境必须显式提供：

```bash
export JWT_SECRET='replace-with-a-long-random-secret'
```

#### 4. CORS

开发环境默认允许：

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:5173
```

生产环境需改为真实前端域名，不允许使用宽泛通配。

#### 5. 文件上传

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

默认单文件大小限制：

- `10MB`

阿里云 OSS 不是系统运行的必需依赖，但生产环境推荐启用。`object-storage.enabled` 默认关闭，关闭时新文件写入本地存储，文件上传能力不关闭；开启时新文件统一写入阿里云 OSS 私有 Bucket。开启阿里云 OSS 时必须配置 `ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET`、`ALIYUN_OSS_PRIVATE_BUCKET`。当前用户头像通过 `/api/file/avatar/upload` 上传，云存储模式下 `sys_user.avatar_url` 保存后端受控地址 `/api/file/avatar/{id}`，浏览器访问该地址时由后端代理读取 OSS 对象并输出图片流。

当前文件存储方案：

1. 本地存储是默认兜底方案，适合本地开发、单机测试和未配置 OSS 的环境。
2. 阿里云 OSS 是推荐生产方案，所有云端文件统一写入私有 Bucket，不使用公共 Bucket。
3. OSS 不返回永久外链，数据库 `sys_file.file_url` 为空；私有下载类接口访问时后端按 `fileId` 生成有效期 URL。
4. 头像这类业务公开文件也不直接暴露 OSS 地址，前端保存 `/api/file/avatar/{id}`，展示时由后端同域代理输出图片。
5. 本地文件和 OSS 文件可以长期混合存在，系统按 `sys_file.storage_type` 路由读取。

头像展示规则：

1. `APP_OBJECT_STORAGE_ENABLED=false`：头像上传到本地存储，`fileUrl` 返回 `/api/file/public/**`。
2. `APP_OBJECT_STORAGE_ENABLED=true`：头像上传到阿里云 OSS 私有 Bucket，前端与数据库统一只保存 `/api/file/avatar/{id}` 这种稳定地址，不保存短期签名 URL。
3. OSS 文件记录统一保存 `access_policy=private_read`，头像访问时由后端代理读取并设置短期浏览器缓存；OSS 签名地址不会直接暴露到前端状态。
4. 历史文件读取、下载、删除按 `sys_file.storage_type` 路由，切换本地或 OSS 默认写入配置不会影响存量文件访问。
5. 私有文件上传接口为 `/api/file/private/upload`，下载前通过 `/api/file/private/{id}/download-url` 获取临时签名 URL 或本地代理下载地址。
6. 文件上传会计算 SHA-256 内容 Hash，相同内容会复用已有本地文件或 OSS 对象，减少重复上传和存储占用；业务层仍新增文件记录，保留上传人、原始文件名和审计时间。
7. 文件上传和头像上传统一按 IP 与用户双维度限流：同一 IP 每 10 分钟最多 60 次，同一用户每 10 分钟最多 20 次。
8. `sys_file` 额外记录 `biz_module`、`biz_id`、`visibility`、`owner_type`、`owner_id`，支持多个业务模块共享同一文件中心并按归属做授权。

文件访问控制规则：

1. `/api/file/list`、`/api/file/{id}/download`、`/api/file/private/**`、`/api/file/{id}/delete`、`/api/file/{id}/status` 仅管理员可访问。
2. `/api/file/public/**` 仅用于读取数据库中状态为 `active` 的本地公共文件。
3. `/api/file/avatar/{id}` 允许浏览器匿名读取，但文件必须同时满足“公开文件、状态启用、图片类型、且已被用户资料引用为头像”四个条件。
4. 上传与列表接口不再向前端返回对象存储 `bucketName`、`accessPolicy`、`objectKey`、`contentHash` 等内部字段。
5. `/api/file/biz/upload` 默认把文件归属到当前登录用户；`/api/file/biz/{id}/download-url` 与 `/api/file/biz/{id}/download` 会按当前用户、文件状态、可见性和归属信息执行授权。

业务文件接口约定：

1. 业务模块上传使用 `POST /api/file/biz/upload`，表单字段包含 `file`、`bizModule`、`bizId`、`visibility`。
2. 通用上传接口适合订单附件、工单图片、文章素材等“归属当前用户”的场景；更复杂的跨角色授权场景可直接复用服务层 `uploadWithCommand` 能力扩展。
3. 业务文件下载优先使用 `/api/file/biz/{id}/download-url` 或 `/api/file/biz/{id}/download`，不要直接复用管理员下载接口。

OSS 模式最小环境变量：

```bash
export APP_OBJECT_STORAGE_ENABLED=true
export ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export ALIYUN_OSS_ACCESS_KEY_ID=
export ALIYUN_OSS_ACCESS_KEY_SECRET=
export ALIYUN_OSS_PRIVATE_BUCKET=ai-web-private
```

可选环境变量：

```bash
export ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS=600
export ALIYUN_OSS_KEY_PREFIX=uploads
```

阿里云准备步骤：

1. 进入阿里云控制台，打开对象存储 OSS，创建 Bucket，例如 `ai-web-private`。
2. Bucket 地域选择业务所在地域，例如华东 1 杭州，对应 Endpoint 为 `https://oss-cn-hangzhou.aliyuncs.com`。
3. Bucket 读写权限选择私有，不要开启公共读。
4. 进入 RAM 访问控制，创建一个专用用户，例如 `spring-admin-oss`，访问方式选择 OpenAPI 调用访问。
5. 为该 RAM 用户创建 AccessKey，得到 `AccessKeyId` 和 `AccessKeySecret`。
6. 给 RAM 用户授权最小 OSS 权限，建议只允许访问目标 Bucket 的 `GetObject`、`PutObject`、`DeleteObject`、`ListObjects` 等必要操作。
7. 将 AccessKey 和 Bucket 信息通过环境变量注入运行环境，不要写入 Git 仓库、README 示例值或配置文件。

本地开发如果使用 IDEA 启动后端，建议在 Run Configuration 的 Environment variables 中显式配置 OSS 环境变量。仅写入 `~/.zshrc` 后，已经启动的 IDEA 通常不会自动继承，需要重启 IDEA 或手动配置。

#### 6. 日志归档

```yaml
app:
  batch:
    log-archive:
      enabled: ${LOG_ARCHIVE_ENABLED:true}
      cron: ${LOG_ARCHIVE_CRON:0 0 2 * * ?}
      retention-days: ${LOG_ARCHIVE_RETENTION_DAYS:30}
      batch-size: ${LOG_ARCHIVE_BATCH_SIZE:1000}
```

#### 7. 邮件服务

```yaml
spring:
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: ${MAIL_SMTP_AUTH:true}
          connectiontimeout: ${MAIL_SMTP_CONNECTION_TIMEOUT:5000}
          timeout: ${MAIL_SMTP_READ_TIMEOUT:5000}
          writetimeout: ${MAIL_SMTP_WRITE_TIMEOUT:5000}
          starttls:
            enable: ${MAIL_SMTP_STARTTLS_ENABLE:false}
            required: ${MAIL_SMTP_STARTTLS_REQUIRED:false}
          ssl:
            enable: ${MAIL_SMTP_SSL_ENABLE:true}

app:
  mail:
    enabled: ${MAIL_ENABLED:true}
    from: ${MAIL_FROM:${MAIL_USERNAME:}}
    default-encoding: ${APP_MAIL_DEFAULT_ENCODING:UTF-8}
    async:
      core-pool-size: ${MAIL_ASYNC_CORE_POOL_SIZE:2}
      max-pool-size: ${MAIL_ASYNC_MAX_POOL_SIZE:8}
      queue-capacity: ${MAIL_ASYNC_QUEUE_CAPACITY:200}
      await-termination-seconds: ${MAIL_ASYNC_AWAIT_TERMINATION_SECONDS:30}
    aliyun:
      enabled: ${ALIYUN_MAIL_ENABLED:false}
      endpoint: ${ALIYUN_MAIL_ENDPOINT:dm.aliyuncs.com}
      region-id: ${ALIYUN_MAIL_REGION_ID:cn-hangzhou}
      access-key-id: ${ALIYUN_MAIL_ACCESS_KEY_ID:}
      access-key-secret: ${ALIYUN_MAIL_ACCESS_KEY_SECRET:}
      account-name: ${ALIYUN_MAIL_ACCOUNT_NAME:${MAIL_FROM:}}
      from-alias: ${ALIYUN_MAIL_FROM_ALIAS:}
      address-type: ${ALIYUN_MAIL_ADDRESS_TYPE:1}
      reply-to-address: ${ALIYUN_MAIL_REPLY_TO_ADDRESS:true}
      reply-address: ${ALIYUN_MAIL_REPLY_ADDRESS:}
      reply-address-alias: ${ALIYUN_MAIL_REPLY_ADDRESS_ALIAS:}
      tag-name: ${ALIYUN_MAIL_TAG_NAME:}
      click-trace: ${ALIYUN_MAIL_CLICK_TRACE:0}
      connect-timeout: ${ALIYUN_MAIL_CONNECT_TIMEOUT:5000}
      read-timeout: ${ALIYUN_MAIL_READ_TIMEOUT:10000}
    template:
      brand-name: ${APP_MAIL_TEMPLATE_BRAND_NAME:React Admin Starter}
      signature: ${APP_MAIL_TEMPLATE_SIGNATURE:React Admin Starter}
      primary-color: ${APP_MAIL_TEMPLATE_PRIMARY_COLOR:#1677ff}
      background-color: ${APP_MAIL_TEMPLATE_BACKGROUND_COLOR:#f4f7fb}
      card-background-color: ${APP_MAIL_TEMPLATE_CARD_BACKGROUND_COLOR:#ffffff}
    register:
      enabled: ${MAIL_REGISTER_ENABLED:true}
      subject: ${MAIL_REGISTER_SUBJECT:Spring Admin 账号激活}
      verify-link-base-url: ${MAIL_REGISTER_VERIFY_LINK_BASE_URL:http://localhost:5173}
    verification-code:
      enabled: ${MAIL_VERIFICATION_CODE_ENABLED:true}
      code-length: ${MAIL_VERIFICATION_CODE_LENGTH:6}
      ttl-seconds: ${MAIL_VERIFICATION_CODE_TTL_SECONDS:300}
      verified-ttl-seconds: ${MAIL_VERIFICATION_CODE_VERIFIED_TTL_SECONDS:600}
      fail-limit: ${MAIL_VERIFICATION_CODE_FAIL_LIMIT:5}
      subject: ${MAIL_VERIFICATION_CODE_SUBJECT:邮箱验证码}
      title: ${MAIL_VERIFICATION_CODE_TITLE:邮箱验证码}
```

邮件能力已升级为通用服务，当前内置的注册账号激活邮件只是其中一个业务实现。项目内其他业务模块可以直接注入 `com.winsalty.quickstart.infra.mail.MailService` 发送文本或 HTML 邮件，不需要关心底层使用 SMTP 还是阿里云 DirectMail。系统同时内置了统一的卡片式 HTML 邮件模板，默认对齐 `react-admin-starter` 的浅色品牌风格与文案语气，默认品牌名为 `React Admin Starter`，并自动附带纯文本 fallback，兼容只支持纯文本的客户端。

注册提交接口 `POST /api/auth/register` 会完成用户名和邮箱唯一性校验，创建 `pending` 状态账号，分配默认 viewer 角色，并发送账号激活邮件。`pending` 账号会占用用户名和邮箱，但登录时会返回“账号尚未激活”的业务错误。用户点击邮件中的链接后，前端调用 `POST /api/auth/register/verify-link` 校验一次性 token，校验成功后后端把账号状态切换为 `active`，之后才能正常登录。用户在激活前用同一用户名和邮箱再次提交注册时，系统会刷新待激活账号密码并重发激活邮件；也可以在前端待验证页面调用 `POST /api/auth/register/resend-verify-mail` 重新发送验证邮件，后端会校验邮箱仍处于 `pending` 状态并复用注册验证邮件限流。

注册激活已经切换为一次性邮件链接，不再复用手填验证码。后续业务如果仍需要邮箱验证码，可注入 `com.winsalty.quickstart.infra.verification.EmailVerificationCodeService`。该服务按 `scene` 隔离不同业务，Redis key 使用 `scene + email fingerprint`，缓存内容为 HMAC 摘要而非明文验证码，并支持错误次数限制、`verifyCode` 后一次性消费、或 `consumeCode` 直接校验消费。

通用邮件服务使用示例：

```java
@Service
public class NoticeMailService {

    private final MailService mailService;

    public NoticeMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendNotice(String email, String noticeTitle, String content) {
        mailService.sendText(email, noticeTitle, content);
    }
}
```

如果业务需要统一样式模板，可注入 `MailTemplateService` 先生成 HTML 和纯文本内容，再调用 `MailService` 发送：

```java
@Service
public class WorkflowMailService {

    private final MailService mailService;
    private final MailTemplateService mailTemplateService;

    public WorkflowMailService(MailService mailService, MailTemplateService mailTemplateService) {
        this.mailService = mailService;
        this.mailTemplateService = mailTemplateService;
    }

    public void sendApproveNotice(String email, String approveUrl) {
        StandardMailTemplate template = new StandardMailTemplate();
        template.setTitle("审批待处理提醒");
        template.setGreeting("您好，");
        template.setSummary("您有一条新的审批任务待处理，请尽快登录系统查看。");
        template.setActionText("立即处理");
        template.setActionUrl(approveUrl);
        template.setFooterNote("此邮件由系统自动发送，请勿直接回复。");
        MailTemplateContent content = mailTemplateService.renderStandard(template);
        mailService.sendHtml(email, "审批待处理提醒", content.getTextContent(), content.getHtmlContent());
    }
}
```

通用邮箱验证码使用示例：

```java
@Service
public class PasswordResetVerifyService {

    private static final String SCENE_PASSWORD_RESET = "password-reset";

    private final EmailVerificationCodeService emailVerificationCodeService;

    public PasswordResetVerifyService(EmailVerificationCodeService emailVerificationCodeService) {
        this.emailVerificationCodeService = emailVerificationCodeService;
    }

    public void sendResetCode(String email) {
        EmailVerificationCodeSendRequest request = new EmailVerificationCodeSendRequest();
        request.setScene(SCENE_PASSWORD_RESET);
        request.setEmail(email);
        request.setSubject("密码重置验证码");
        request.setTitle("密码重置验证");
        request.setSummary("请使用以下验证码完成密码重置。");
        emailVerificationCodeService.sendCode(request);
    }

    public void consumeResetCode(String email, String code) {
        EmailVerificationCodeVerifyRequest request = new EmailVerificationCodeVerifyRequest();
        request.setScene(SCENE_PASSWORD_RESET);
        request.setEmail(email);
        request.setCode(code);
        emailVerificationCodeService.consumeCode(request);
    }
}
```

当前邮件开关分为四层：

1. `app.mail.enabled`：控制整个项目的通用邮件服务是否启用。
2. `app.mail.register.enabled`：只控制注册账号激活邮件是否启用。
3. `app.mail.verification-code.enabled`：控制后续业务复用的通用邮箱验证码服务是否启用。
4. `app.mail.aliyun.enabled`：控制是否使用阿里云 DirectMail API；默认 `false` 时使用 SMTP。

`prod` profile 中 `app.mail.register.enabled` 默认值为 `false`，即使通用邮件服务可用，也不会自动开放注册账号激活邮件。若生产环境确需开放自助注册，需要同时显式开启 `APP_SECURITY_REGISTER_ENABLED=true`、`MAIL_REGISTER_ENABLED=true`，并把 `MAIL_REGISTER_VERIFY_LINK_BASE_URL` 配置为前端站点地址。

通用邮件发送使用有界线程池异步提交远端发送任务，并配置 SMTP 或阿里云 SDK 超时，避免邮件服务慢调用长期占用 Web 请求线程。邮件发送日志只记录脱敏收件人和主题指纹，不记录明文邮箱主题。

邮件相关配置项说明：

| 环境变量 | 说明 | 是否必填 |
| --- | --- | --- |
| `MAIL_HOST` | SMTP 服务器地址 | 是 |
| `MAIL_PORT` | SMTP 端口，默认 `465` | 否 |
| `MAIL_USERNAME` | SMTP 用户名，通常为完整邮箱地址 | 是 |
| `MAIL_PASSWORD` | SMTP 密码或授权码 | 是 |
| `MAIL_FROM` | 发件人地址，不配置时默认取 `MAIL_USERNAME` | 否 |
| `MAIL_ENABLED` | 通用邮件服务总开关 | 否 |
| `MAIL_REGISTER_ENABLED` | 注册账号激活邮件开关 | 否 |
| `MAIL_REGISTER_SUBJECT` | 注册账号激活邮件主题 | 否 |
| `MAIL_REGISTER_VERIFY_LINK_BASE_URL` | 邮件激活链接使用的前端站点地址，例如 `https://admin.example.com` | 生产环境必填 |
| `MAIL_VERIFICATION_CODE_ENABLED` | 通用邮箱验证码服务开关 | 否 |
| `MAIL_VERIFICATION_CODE_LENGTH` | 通用邮箱验证码长度，默认 `6`，允许 `4` 到 `8` 位 | 否 |
| `MAIL_VERIFICATION_CODE_TTL_SECONDS` | 通用邮箱验证码有效期，单位秒，默认 `300` | 否 |
| `MAIL_VERIFICATION_CODE_VERIFIED_TTL_SECONDS` | `verifyCode` 成功后的已验证状态有效期，单位秒，默认 `600` | 否 |
| `MAIL_VERIFICATION_CODE_FAIL_LIMIT` | 验证码错误次数上限，默认 `5` | 否 |
| `MAIL_VERIFICATION_CODE_SUBJECT` | 通用邮箱验证码默认邮件主题 | 否 |
| `MAIL_VERIFICATION_CODE_TITLE` | 通用邮箱验证码默认邮件标题 | 否 |
| `MAIL_SMTP_AUTH` | 是否开启 SMTP 鉴权 | 否 |
| `MAIL_SMTP_STARTTLS_ENABLE` | 是否开启 STARTTLS，默认 `false` | 否 |
| `MAIL_SMTP_STARTTLS_REQUIRED` | 是否强制要求 STARTTLS | 否 |
| `MAIL_SMTP_SSL_ENABLE` | 是否开启 SSL，默认 `true` | 否 |
| `MAIL_SMTP_CONNECTION_TIMEOUT` | SMTP 建连超时时间，单位毫秒，默认 `5000` | 否 |
| `MAIL_SMTP_READ_TIMEOUT` | SMTP 读取超时时间，单位毫秒，默认 `5000` | 否 |
| `MAIL_SMTP_WRITE_TIMEOUT` | SMTP 写入超时时间，单位毫秒，默认 `5000` | 否 |
| `MAIL_ASYNC_CORE_POOL_SIZE` | 邮件发送核心线程数，默认 `2` | 否 |
| `MAIL_ASYNC_MAX_POOL_SIZE` | 邮件发送最大线程数，默认 `8` | 否 |
| `MAIL_ASYNC_QUEUE_CAPACITY` | 邮件发送队列容量，默认 `200` | 否 |
| `MAIL_ASYNC_AWAIT_TERMINATION_SECONDS` | 停机等待已提交邮件任务完成的秒数，默认 `30` | 否 |
| `ALIYUN_MAIL_ENABLED` | 是否启用阿里云 DirectMail API，默认 `false` | 否 |
| `ALIYUN_MAIL_ENDPOINT` | 阿里云邮件推送 Endpoint，默认 `dm.aliyuncs.com` | 否 |
| `ALIYUN_MAIL_REGION_ID` | 阿里云区域，默认 `cn-hangzhou` | 否 |
| `ALIYUN_MAIL_ACCESS_KEY_ID` | 阿里云 AccessKeyId，启用阿里云邮件时必填 | 条件必填 |
| `ALIYUN_MAIL_ACCESS_KEY_SECRET` | 阿里云 AccessKeySecret，启用阿里云邮件时必填 | 条件必填 |
| `ALIYUN_MAIL_ACCOUNT_NAME` | 邮件推送控制台配置并验证通过的发信地址，启用阿里云邮件时必填 | 条件必填 |
| `ALIYUN_MAIL_FROM_ALIAS` | 阿里云邮件发信人昵称 | 否 |
| `ALIYUN_MAIL_ADDRESS_TYPE` | 阿里云地址类型，默认 `1` 表示发信地址 | 否 |
| `ALIYUN_MAIL_REPLY_TO_ADDRESS` | 是否启用控制台配置的回信地址，默认 `true` | 否 |
| `ALIYUN_MAIL_REPLY_ADDRESS` | 回信地址 | 否 |
| `ALIYUN_MAIL_REPLY_ADDRESS_ALIAS` | 回信地址昵称 | 否 |
| `ALIYUN_MAIL_TAG_NAME` | 邮件标签，用于阿里云发送统计和跟踪 | 否 |
| `ALIYUN_MAIL_CLICK_TRACE` | 点击跟踪开关，默认 `0` 关闭 | 否 |
| `ALIYUN_MAIL_CONNECT_TIMEOUT` | 阿里云 SDK 建连超时时间，单位毫秒，默认 `5000` | 否 |
| `ALIYUN_MAIL_READ_TIMEOUT` | 阿里云 SDK 读取超时时间，单位毫秒，默认 `10000` | 否 |
| `APP_MAIL_DEFAULT_ENCODING` | 邮件默认编码 | 否 |
| `APP_MAIL_TEMPLATE_BRAND_NAME` | HTML 模板品牌名称 | 否 |
| `APP_MAIL_TEMPLATE_SIGNATURE` | HTML 模板页脚签名 | 否 |
| `APP_MAIL_TEMPLATE_PRIMARY_COLOR` | HTML 模板主色，仅支持 `#RRGGBB` | 否 |
| `APP_MAIL_TEMPLATE_BACKGROUND_COLOR` | HTML 模板背景色，仅支持 `#RRGGBB` | 否 |
| `APP_MAIL_TEMPLATE_CARD_BACKGROUND_COLOR` | HTML 模板卡片背景色，仅支持 `#RRGGBB` | 否 |

163 邮箱推荐配置示例：

```bash
export MAIL_HOST=smtp.163.com
export MAIL_PORT=465
export MAIL_USERNAME='winsalty@163.com'
export MAIL_PASSWORD='replace-with-163-auth-code'
export MAIL_FROM='winsalty@163.com'
export MAIL_REGISTER_ENABLED=true
export MAIL_REGISTER_SUBJECT='Spring Admin 账号激活'
export MAIL_REGISTER_VERIFY_LINK_BASE_URL='http://localhost:5173'
export MAIL_SMTP_AUTH=true
export MAIL_SMTP_STARTTLS_ENABLE=false
export MAIL_SMTP_STARTTLS_REQUIRED=false
export MAIL_SMTP_SSL_ENABLE=true
export MAIL_SMTP_CONNECTION_TIMEOUT=5000
export MAIL_SMTP_READ_TIMEOUT=5000
export MAIL_SMTP_WRITE_TIMEOUT=5000
```

说明：

1. 默认通道使用 SMTP 发信，不需要配置 POP3 或 IMAP。
2. `MAIL_PASSWORD` 应填写邮箱服务商提供的 SMTP 授权码，不建议直接使用邮箱登录密码。
3. 163 邮箱优先推荐 `465 + SSL` 组合；如改用 `587`，通常应切换到 `STARTTLS`。

阿里云 DirectMail 推荐配置示例：

```bash
export ALIYUN_MAIL_ENABLED=true
export ALIYUN_MAIL_ENDPOINT=dm.aliyuncs.com
export ALIYUN_MAIL_REGION_ID=cn-hangzhou
export ALIYUN_MAIL_ACCESS_KEY_ID=
export ALIYUN_MAIL_ACCESS_KEY_SECRET=
export ALIYUN_MAIL_ACCOUNT_NAME=notice@example.com
export ALIYUN_MAIL_FROM_ALIAS='Spring Admin'
export ALIYUN_MAIL_TAG_NAME=register
export ALIYUN_MAIL_CLICK_TRACE=0
export MAIL_FROM=notice@example.com
export MAIL_REGISTER_ENABLED=true
export MAIL_REGISTER_VERIFY_LINK_BASE_URL=https://admin.example.com
```

启用阿里云邮件后，`MailService` 会使用阿里云 `SingleSendMail` API 发送文本和 HTML 邮件；未启用时继续使用默认 SMTP。阿里云发信地址必须先在邮件推送控制台配置并验证通过，AccessKey 建议使用只授予 `dm:SingleSendMail` 权限的 RAM 用户。

IDEA Run Configuration 可以直接使用如下环境变量串：

```text
MAIL_HOST=smtp.163.com;MAIL_PORT=465;MAIL_USERNAME=winsalty@163.com;MAIL_PASSWORD=replace-with-163-auth-code;MAIL_FROM=winsalty@163.com;MAIL_REGISTER_ENABLED=true;MAIL_REGISTER_SUBJECT=Spring Admin 账号激活;MAIL_REGISTER_VERIFY_LINK_BASE_URL=http://localhost:5173;MAIL_SMTP_AUTH=true;MAIL_SMTP_STARTTLS_ENABLE=false;MAIL_SMTP_STARTTLS_REQUIRED=false;MAIL_SMTP_SSL_ENABLE=true;MAIL_SMTP_CONNECTION_TIMEOUT=5000;MAIL_SMTP_READ_TIMEOUT=5000;MAIL_SMTP_WRITE_TIMEOUT=5000
```

如果在 `prod` 环境需要开放用户自助注册，还要额外追加：

```text
;APP_SECURITY_REGISTER_ENABLED=true;MAIL_REGISTER_ENABLED=true;MAIL_REGISTER_VERIFY_LINK_BASE_URL=https://admin.example.com
```

原因是生产环境 [application-prod.yml](src/main/resources/application-prod.yml) 默认 `app.security.register-enabled=false` 且 `app.mail.register.enabled=false`，即便通用邮件服务已经可用，也不会自动开放 `/api/auth/register` 注册入口和注册邮箱验证入口。

### 推荐环境变量

```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080

export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=spring_admin
export DB_USERNAME=spring_admin_prod
export DB_PASSWORD='replace-with-strong-password'

export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379

export ES_URIS=http://127.0.0.1:9200
export ES_USERNAME=
export ES_PASSWORD=

export JWT_SECRET='replace-with-a-long-random-secret'
export APP_FILE_UPLOAD_DIR=/data/spring-admin-starter/uploads

export APP_OBJECT_STORAGE_ENABLED=false
export ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export ALIYUN_OSS_ACCESS_KEY_ID=
export ALIYUN_OSS_ACCESS_KEY_SECRET=
export ALIYUN_OSS_PRIVATE_BUCKET=ai-web-private
export ALIYUN_OSS_PRIVATE_URL_EXPIRE_SECONDS=600
export ALIYUN_OSS_KEY_PREFIX=uploads
export LOCAL_STORAGE_ROOT_PATH=/data/spring-admin-starter/uploads
export LOCAL_STORAGE_PUBLIC_BASE_URL=/api/file/public
export LOCAL_STORAGE_PRIVATE_URL_EXPIRE_SECONDS=600

export LOG_ARCHIVE_ENABLED=true
export LOG_ARCHIVE_CRON='0 0 2 * * ?'
export LOG_ARCHIVE_RETENTION_DAYS=30
export LOG_ARCHIVE_BATCH_SIZE=1000
```

## SQL 与初始化说明

### 初始化脚本

项目当前同时保留两套 SQL 资源：

| 路径 | 说明 |
| --- | --- |
| `sql/init.sql` | 一次性初始化主表结构 |
| `sql/seed-data.sql` | 一次性导入种子数据 |
| `resources/sql/` | 按版本拆分的 SQL 脚本集合 |

如果你是从旧版本库升级，而不是重新执行 `sql/init.sql`，文件模块上线前至少需要补齐 `resources/sql/V18__extend_file_business_scope_schema.sql`。当前头像上传、业务文件上传和 OSS 文件复用都依赖 `sys_file.biz_module`、`biz_id`、`visibility`、`owner_type`、`owner_id` 字段；缺失这些字段时，接口会返回“文件表结构未升级”的明确提示。

认证模块新增“用户名或邮箱登录”和“邮箱唯一注册”能力后，旧库还需要继续执行 `resources/sql/V19__add_unique_email_login_support.sql`，为 `sys_user.email` 增加唯一索引。执行前应先清理历史重复邮箱数据，否则索引脚本会被数据库拒绝。

### 本地初始化

```bash
cd /Users/salty/codeProject/ai/spring-admin-starter
mysql -u spring_admin -p spring_admin < sql/init.sql
mysql -u spring_admin -p spring_admin < sql/seed-data.sql
```

### 默认演示账号

- `admin / 123456`
- `viewer / 123456`

正式部署前必须修改或禁用默认账号。

## 启动与验证

### 1. 安装依赖并编译

```bash
cd /Users/salty/codeProject/ai/spring-admin-starter
mvn clean compile -DskipTests
```

### 2. 启动开发环境

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 常用命令

```bash
mvn test
mvn clean package -DskipTests
java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

| 命令 | 作用 |
| --- | --- |
| `mvn spring-boot:run -Dspring-boot.run.profiles=dev` | 启动开发环境 |
| `mvn test` | 执行测试 |
| `mvn clean package -DskipTests` | 打包生产 JAR |
| `java -jar ... --spring.profiles.active=prod` | 以生产配置运行 |

### 4. 启动后检查

启动成功后建议至少验证以下地址：

1. `http://localhost:8080/actuator/health`
2. `http://localhost:8080/swagger-ui.html`
3. `POST /api/auth/login`
4. `GET /api/permission/bootstrap`
5. `GET /api/file/object-storage/status`

### 5. 停止服务

前台运行：

```bash
Ctrl + C
```

按端口定位进程：

```bash
lsof -i :8080
kill <PID>
```

## 部署说明

### 部署原则

1. 生产环境只使用 `prod` profile。
2. 数据库、Redis、JWT 密钥、上传目录、阿里云 OSS 密钥和 CORS 域名等必须外部化配置；未启用 OSS 时必须保证本地上传目录持久化。
3. 不要继续使用开发环境默认数据库账号、JWT 密钥和默认演示密码。
4. 建议由 Nginx、网关、systemd、Supervisor、Docker 或 Kubernetes 托管进程。

### 打包

```bash
cd /Users/salty/codeProject/ai/spring-admin-starter
mvn clean package -DskipTests
```

### 启动 JAR

```bash
java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

### 外部配置文件方式

```bash
java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/etc/spring-admin-starter/
```

### 后台运行示例

```bash
nohup java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > logs/startup.log 2>&1 &
echo $! > spring-admin-starter.pid
```

### 生产部署前检查清单

1. MySQL 已初始化表结构和种子数据。
2. Redis 已可连接。
3. Elasticsearch 已可连接。
4. `JWT_SECRET` 已替换为强随机密钥。
5. 上传目录已创建且应用用户有读写权限。
6. 如启用阿里云 OSS，私有 Bucket、Endpoint、RAM 权限和 AccessKey 注入方式已确认；如未启用 OSS，本地存储目录已挂载到持久化磁盘。
7. CORS 白名单已配置前端正式域名。
8. 默认账号已下线或修改密码。
9. Swagger 是否对公网暴露已按安全策略处理。
10. 日志目录与归档策略已确认。

## 与前端配套关系

当前项目默认对接 `/Users/salty/codeProject/ai/react-admin-starter`，两者的职责边界如下：

| 系统 | 职责 |
| --- | --- |
| `react-admin-starter` | 页面展现、交互编排、权限 UI 渲染 |
| `spring-admin-starter` | 鉴权、权限计算、业务数据、缓存、日志审计、文件存储 |

推荐本地联调顺序：

1. 初始化 MySQL、Redis、Elasticsearch。
2. 启动当前后端项目。
3. 启动前端项目。
4. 使用演示账号登录并验证工作台、权限目录、系统管理、公告管理等页面。

## 运维与扩展建议

1. 若继续新增系统模块，建议沿用 `controller + service + mapper + entity/dto/vo` 的分层方式。
2. 若新增高频读配置，优先接入现有 Redis 缓存服务，避免重复实现缓存模板。
3. 若新增审计敏感操作，建议统一使用 `@AuditLog` 接入日志链路。
4. Elasticsearch 目前是基础设施已就绪状态，新增检索场景时优先复用 `ElasticsearchTemplateService`。
