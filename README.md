# Spring Admin Starter

## 项目说明

本项目是基于 Java 8 + Spring Boot 2.7.18 的后台脚手架，当前已完成 `todolist.md` 中阶段 0、阶段 1 和阶段 2 的最小闭环。

## 当前能力

- 已完成阶段 0：工程骨架、统一响应、全局异常处理、多环境配置、CORS、OpenAPI、健康检查。
- 已完成阶段 1：`/api/auth/login`、`/api/auth/register`、`/api/auth/profile`、`/api/permission/bootstrap`。
- 已完成阶段 2：`/api/dashboard/overview`、`/api/query/list`、`/api/query/detail`、`/api/query/save`。
- 已完成阶段 3：`/api/system/{moduleKey}/list`、`/api/system/detail`、`/api/system/save`、`/api/system/status`，覆盖 `users`、`roles`、`dicts`、`logs`。
- 已完成阶段 4：`/api/system/menus/tree`、`/api/system/menus/save`、`/api/system/menus/status`、`/api/permission/assignment`，已联动权限 bootstrap。
- 已完成阶段 5：`/api/system/configs`、`/api/system/configs/save`，已接入 Redis 并覆盖 bootstrap、dict、config 三类缓存。
- 已完成阶段 6 第一阶段：登录接口升级为双 Token，新增 refresh token、登出与基于 Redis 的会话失效控制。
- 已完成阶段 6 第二阶段：新增登录日志、操作日志、异常日志写入链路，并接入 auth、permission、system 和全局异常处理。
- 已完成登录鉴权安全加固：生产 JWT 密钥强校验、匿名登录/验证码限流、refresh token 轮换校验和 demo 接口收敛。
- 已完成阶段 6 后续模块：新增新版字典类型/字典项、参数配置、文件上传下载与完整初始化 SQL。
- 已补齐公告通知、部门管理、用户角色真实联动接口；用户列表和角色列表已切到 `sys_user`、`sys_role`、`sys_user_role` 真实关系。
- 已切换数据库连接池为 Druid，统一使用 fastjson2 做项目 JSON 序列化。
- 已补齐 Redis 通用工具类，支持字符串、对象、Hash、List、Set、计数器、TTL、分布式占位和 Java 对象压缩缓存。
- 已接入 Quartz 定时任务框架，日志模块支持按配置周期将历史日志迁移到 `sys_log_archive` 留档表。
- 已接入 Elasticsearch，并封装 `ElasticsearchTemplateService` 作为项目通用 ES 工具。
- 已接入 JWT Bearer 鉴权。
- 已提供 `admin` / `viewer` 两套角色与权限差异数据。
- 当前阶段按要求先使用本地 MySQL `spring_admin`。
- 当前阶段已接入本地 Redis，用于 bootstrap、dict、config 缓存。
- 文件上传默认本地存储目录为项目根目录 `uploads/`，单文件限制 10MB。
- 注册接口仅在 dev 环境开放。
- 登录鉴权审计报告详见 [SECURITY_AUTH_AUDIT.md](./SECURITY_AUTH_AUDIT.md)。

## 启动要求

- JDK 8
- Maven 3.6+
- 本地 MySQL 5.7/8.0
- 本地 Redis 5.x+
- Elasticsearch 7.x，默认地址 `http://127.0.0.1:9200`

## 本地开发配置

默认 dev 数据源配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/spring_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: spring_admin
    password: SpringAdmin@2026
```

JWT dev 密钥已内置在 `application-dev.yml`，仅用于当前本地开发。

dev 环境默认 Redis 与 ES 配置：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
  elasticsearch:
    uris: http://127.0.0.1:9200
```

日志归档跑批默认每天 02:00 执行，迁移 30 天前的日志，每批最多 1000 条。可通过以下环境变量覆盖：

```bash
export LOG_ARCHIVE_ENABLED=true
export LOG_ARCHIVE_CRON='0 0 2 * * ?'
export LOG_ARCHIVE_RETENTION_DAYS=30
export LOG_ARCHIVE_BATCH_SIZE=1000
```

## 初始化 SQL

当前版本 SQL 脚本位于项目根目录：

- `sql/init.sql`
- `sql/seed-data.sql`

若需要一次性重建完整结构和种子数据，可执行：

```bash
mysql -u spring_admin -p spring_admin < sql/init.sql
mysql -u spring_admin -p spring_admin < sql/seed-data.sql
```

## 默认演示账号

- `admin / 123456`
- `viewer / 123456`

## 启动命令

```bash
mvn clean compile -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

浏览器访问：

- 健康检查：`http://localhost:8080/actuator/health`
- Swagger：`http://localhost:8080/swagger-ui.html`

本地停止：

```bash
CTRL+C
```

如果使用后台方式启动，可按端口查找并停止：

```bash
lsof -i :8080
kill <PID>
```

## 生产部署说明

后端生产部署的目标是使用 `prod` profile 运行打包后的 Spring Boot JAR，并把 MySQL、Redis、JWT 密钥、CORS 域名、文件存储目录等配置全部外部化。生产环境不要使用 `dev` profile，也不要继续使用 README 中的本地数据库账号和开发 JWT 密钥。

### 部署前准备

1. 准备 JDK 8 运行环境。
2. 准备 MySQL 5.7/8.0，并创建生产数据库和专用账号。
3. 准备 Redis，用于登录会话、权限 bootstrap、字典、系统配置、限流和压缩对象缓存。
4. 准备 Elasticsearch 7.x，用于后续业务检索能力开箱即用。
5. 准备文件上传目录，确保运行用户有读写权限。
6. 准备足够长且不可提交到仓库的 `JWT_SECRET`；`prod` 环境缺失或使用默认占位密钥会启动失败。
7. 确认前端生产域名，例如 `https://admin.example.com`，用于 CORS 白名单。
8. 确认日志归档策略，默认每天 02:00 归档 30 天前日志。
9. 确认是否由 Nginx 或网关统一暴露 `/api`，建议后端只监听内网地址或受控端口。

### 初始化生产数据库

首次部署时，在生产 MySQL 中执行初始化脚本：

```bash
mysql -u <prod_user> -p <prod_database> < sql/init.sql
mysql -u <prod_user> -p <prod_database> < sql/seed-data.sql
```

`seed-data.sql` 会创建演示账号和初始权限。正式上线前必须修改或禁用默认账号 `admin / 123456`、`viewer / 123456`，并按实际组织重新分配角色权限。

### 配置生产环境变量

当前 `application-prod.yml` 已读取 Redis 和 JWT 相关环境变量，但生产仍需要补齐或通过外部配置注入 MySQL 数据源。推荐通过环境变量或独立配置文件管理敏感配置，不要把生产密码写入仓库。

推荐环境变量：

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

如果使用外部配置文件，至少需要覆盖：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
  elasticsearch:
    uris: ${ES_URIS}
    username: ${ES_USERNAME}
    password: ${ES_PASSWORD}

app:
  cors:
    allowed-origins:
      - https://admin.example.com
  security:
    jwt-secret: ${JWT_SECRET}
    register-enabled: false
  file:
    upload-dir: ${APP_FILE_UPLOAD_DIR}
```

注意：`app.cors.allowed-origins` 生产环境必须填写明确域名，不要使用通配符；`register-enabled` 建议保持 `false`。
注意：生产环境建议关闭或限制 Swagger/OpenAPI 访问，并在网关层叠加登录、验证码等匿名接口限流。

### 打包与启动

在项目根目录打包：

```bash
cd /Users/salty/codeProject/ai/spring-admin-starter
mvn clean package -DskipTests
```

启动 JAR：

```bash
java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

后台启动示例：

```bash
nohup java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > logs/startup.log 2>&1 &
echo $! > spring-admin-starter.pid
```

也可以把外部配置文件放到服务器固定目录：

```bash
java -jar target/spring-admin-starter-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/etc/spring-admin-starter/
```

建议用 systemd、Supervisor、Docker 或 Kubernetes 托管进程，确保异常退出后能自动拉起，并把日志输出接入服务器日志系统。

### 停止服务

如果使用上面的 PID 文件启动：

```bash
kill $(cat spring-admin-starter.pid)
rm -f spring-admin-starter.pid
```

如果服务未正常退出，可先确认进程仍存在，再使用强制停止：

```bash
ps -ef | grep spring-admin-starter
kill -9 <PID>
```

systemd 托管时使用：

```bash
systemctl stop spring-admin-starter
```

### Nginx 反向代理示例

如果前端和后端同域部署，可由 Nginx 把 `/api` 转发到后端：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /actuator/health {
    proxy_pass http://127.0.0.1:8080/actuator/health;
}
```

生产环境建议只对内网或监控系统开放 `/actuator/health`，不要公开 Swagger 和详细 Actuator 信息。

### 上线后验证

上线后按顺序验证：

- `GET /actuator/health` 返回 `UP`。
- Redis 可写入会话，登录后 refresh token 和 logout 正常。
- Elasticsearch `/` 地址可访问，应用启动日志没有 ES 客户端初始化错误。
- Quartz 日志归档任务按 `LOG_ARCHIVE_CRON` 注册，`sys_log_archive` 表存在。
- `POST /api/auth/login` 能登录生产账号。
- `GET /api/permission/bootstrap` 返回菜单、路由和按钮权限。
- `GET /api/dashboard/overview`、`GET /api/query/list`、`GET /api/system/users/list` 返回正常。
- 文件上传目录可写，上传、下载、删除接口正常。
- 前端生产域名请求后端接口无 CORS 报错。
- 日志中没有数据库连接失败、Redis 连接失败、JWT secret 过短或权限初始化失败等错误。

### 生产安全 Checklist

- 修改或禁用默认演示账号密码。
- 使用强随机 `JWT_SECRET`，不要使用仓库中的示例值。
- 生产数据库、Redis 密码只通过环境变量、密钥管理或外部配置注入。
- Elasticsearch 只允许应用内网访问；如启用账号密码，必须通过环境变量或密钥管理注入。
- CORS 只允许明确的前端生产域名。
- 关闭生产注册入口，除非业务明确需要开放。
- 限制 Swagger、Actuator、数据库和 Redis 的公网访问。
- 文件上传目录放在独立数据盘或持久卷，并配置备份和容量监控。
- 配置 HTTPS、访问日志、错误日志、进程守护和告警。

## 接口入口

- 健康检查：`/actuator/health`
- 基础联调接口：`/api/common/ping`
- OpenAPI：`/swagger-ui.html`

## 前端联调说明

- 后端 dev 环境默认允许 `http://localhost:5173` 跨域访问，并在 Spring Security 中启用 CORS。
- 前端可通过 Vite `/api` 代理访问本服务，也可配置 `VITE_API_BASE_URL=http://localhost:8080` 直连。
- 登录接口 `POST /api/auth/login` 返回 `token/accessToken/refreshToken` 以及 `roleCode/roleName`，前端使用 Bearer Token 访问受保护接口。
- 权限初始化接口 `GET /api/permission/bootstrap` 基于当前登录用户和角色返回菜单、路由、按钮权限。

## 已验证接口

- `GET /api/dashboard/overview`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/profile`
- `POST /api/auth/refresh-token`
- `POST /api/auth/logout`
- `GET /api/permission/bootstrap`
- `GET /api/query/list`
- `GET /api/query/detail`
- `POST /api/query/save`
- `GET /api/system/users/list`
- `GET /api/system/roles/list`
- `GET /api/system/dicts/list`
- `GET /api/system/logs/list`
- `GET /api/system/detail`
- `POST /api/system/save`
- `POST /api/system/status`
- `GET /api/system/menus/tree`
- `POST /api/system/menus/save`
- `POST /api/system/menus/status`
- `GET /api/permission/assignment`
- `POST /api/permission/assignment`
- `GET /api/system/configs`
- `POST /api/system/configs/save`
- `GET /api/system/dict/types/list`
- `POST /api/system/dict/types/save`
- `POST /api/system/dict/types/status`
- `GET /api/system/dict/data/list`
- `GET /api/system/dict/data/detail`
- `POST /api/system/dict/data/save`
- `POST /api/system/dict/data/status`
- `POST /api/system/dict/cache/refresh`
- `GET /api/system/params/list`
- `GET /api/system/params/detail`
- `POST /api/system/params/save`
- `POST /api/system/params/status`
- `POST /api/system/params/cache/refresh`
- `GET /api/system/notices/list`
- `GET /api/system/notices/detail`
- `POST /api/system/notices/save`
- `POST /api/system/notices/status`
- `GET /api/system/notices/active`
- `GET /api/system/departments/tree`
- `POST /api/system/departments/save`
- `POST /api/system/departments/status`
- `POST /api/system/users/assign-roles`
- `POST /api/file/upload`
- `GET /api/file/list`
- `GET /api/file/{id}/download`
- `POST /api/file/{id}/delete`
- `POST /api/file/{id}/status`
