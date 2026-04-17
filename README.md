# Spring Admin Starter

## 项目说明

本项目是基于 Java 8 + Spring Boot 2.7.18 的后台脚手架，当前已完成 `todolist.md` 中阶段 0、阶段 1 和阶段 2 的最小闭环。

## 当前能力

- 已完成阶段 0：工程骨架、统一响应、全局异常处理、多环境配置、CORS、OpenAPI、健康检查。
- 已完成阶段 1：`/api/auth/login`、`/api/auth/register`、`/api/auth/profile`、`/api/permission/bootstrap`。
- 已完成阶段 2：`/api/dashboard/overview`、`/api/query/list`、`/api/query/detail`、`/api/query/save`。
- 已接入 JWT Bearer 鉴权。
- 已提供 `admin` / `viewer` 两套角色与权限差异数据。
- 当前阶段按要求先使用本地 MySQL `spring_admin`。
- 当前阶段按要求暂不接入 Redis。
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

若需要重建当前版本 RBAC 表，可执行：

```bash
mysql -u spring_admin -p spring_admin < resources/sql/V1__init_rbac_schema.sql
mysql -u spring_admin -p spring_admin < resources/sql/V2__seed_auth_permission_data.sql
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
- `GET /api/permission/bootstrap`
- `GET /api/query/list`
- `GET /api/query/detail`
- `POST /api/query/save`
