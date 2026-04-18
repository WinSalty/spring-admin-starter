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
- 已完成阶段 6 后续模块：新增新版字典类型/字典项、参数配置、文件上传下载与完整初始化 SQL。
- 已接入 JWT Bearer 鉴权。
- 已提供 `admin` / `viewer` 两套角色与权限差异数据。
- 当前阶段按要求先使用本地 MySQL `spring_admin`。
- 当前阶段已接入本地 Redis，用于 bootstrap、dict、config 缓存。
- 文件上传默认本地存储目录为项目根目录 `uploads/`，单文件限制 10MB。
- 注册接口仅在 dev 环境开放。

## 启动要求

- JDK 8
- Maven 3.6+
- 本地 MySQL 5.7/8.0

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

## 初始化 SQL

当前版本 SQL 脚本位于项目根目录：

- `resources/sql/V1__init_rbac_schema.sql`
- `resources/sql/V2__seed_auth_permission_data.sql`
- `resources/sql/V3__init_query_schema.sql`
- `resources/sql/V4__seed_query_data.sql`
- `resources/sql/V5__init_system_schema.sql`
- `resources/sql/V6__seed_system_data.sql`
- `resources/sql/V7__init_config_schema.sql`
- `resources/sql/V8__seed_config_data.sql`
- `resources/sql/V9__init_dict_param_file_schema.sql`
- `resources/sql/V10__seed_dict_param_file_data.sql`

完整初始化脚本也同步到：

- `sql/init.sql`
- `sql/seed-data.sql`

若需要重建当前版本 RBAC 表，可执行：

```bash
mysql -u spring_admin -p spring_admin < resources/sql/V1__init_rbac_schema.sql
mysql -u spring_admin -p spring_admin < resources/sql/V2__seed_auth_permission_data.sql
```

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

## 接口入口

- 健康检查：`/actuator/health`
- 基础联调接口：`/api/common/ping`
- OpenAPI：`/swagger-ui.html`

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
- `POST /api/file/upload`
- `GET /api/file/list`
- `GET /api/file/{id}/download`
- `POST /api/file/{id}/delete`
- `POST /api/file/{id}/status`
