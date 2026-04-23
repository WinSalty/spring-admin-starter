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
| 登录 | `/api/auth/login`，支持 access token + refresh token |
| 刷新令牌 | `/api/auth/refresh-token`，支持 refresh token 轮换 |
| 退出登录 | `/api/auth/logout`，基于 Redis 失效当前会话 |
| 注册 | `/api/auth/register`，仅 `dev` 环境默认开放 |
| 注册验证码 | `/api/auth/register/verify-code`，基于邮件发送 |
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
| 文件管理 | `/api/file/upload`、`/avatar/upload`、`/avatar/{id}`、`/object-storage/status`、`/list`、`/{id}/download`、`/{id}/delete`、`/{id}/status` |

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
| `src/main/resources/application.yml` | 通用配置，包含端口、上传限制、邮件、Quartz、ES 等 |
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
        bucket: ${ALIYUN_OSS_BUCKET:}
        domain: ${ALIYUN_OSS_DOMAIN:}
        key-prefix: ${ALIYUN_OSS_KEY_PREFIX:uploads}
```

默认单文件大小限制：

- `10MB`

`object-storage.enabled` 默认关闭。关闭时普通文件继续使用本地目录，头像上传接口返回对象存储未开启，前端使用用户名首字作为头像占位。开启阿里云 OSS 时必须配置 `ALIYUN_OSS_ENDPOINT`、`ALIYUN_OSS_ACCESS_KEY_ID`、`ALIYUN_OSS_ACCESS_KEY_SECRET`、`ALIYUN_OSS_BUCKET`、`ALIYUN_OSS_DOMAIN`；`ALIYUN_OSS_DOMAIN` 应为可公开访问的 Bucket 域名或 CDN 域名，例如 `https://static.example.com`。当前用户头像通过 `/api/file/avatar/upload` 上传，后端仅允许图片类型并将返回的 `fileUrl` 保存到 `sys_user.avatar_url`。

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
2. 数据库、Redis、JWT 密钥、上传目录或阿里云 OSS 密钥、CORS 域名等必须外部化配置。
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
6. CORS 白名单已配置前端正式域名。
7. 默认账号已下线或修改密码。
8. Swagger 是否对公网暴露已按安全策略处理。
9. 日志目录与归档策略已确认。

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
