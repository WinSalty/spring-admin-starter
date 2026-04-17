# Spring Admin Starter Todolist

## 项目定位

本项目重置后，新的目标是开发一个与 `/Users/salty/codeProject/ai/react-admin-starter` 配套的后端脚手架。

定位如下：

- 基于 Java 8 + Spring Boot 2.7.18 的通用后台后端脚手架。
- 作为 `react-admin-starter` 的配套服务端，优先满足现有前端页面和接口契约。
- 提供认证鉴权、权限菜单、系统管理、查询管理、工作台统计、配置、日志等基础能力。
- 支持本地开发环境直接连接 MySQL，并支持本地安装和接入 Redis。
- 后续可在该脚手架上继续扩展用户、角色、菜单、部门、字典、配置、文件、公告等模块。

## 对齐依据

本次后端脚手架设计以以下两部分为准：

1. 前端项目：`/Users/salty/codeProject/ai/react-admin-starter`
2. 后端设计文档：`/Users/salty/codeProject/ai/spring-admin-starter/springboot_java_8_management_system_doc.md`

前端当前已存在的主要页面和模块：

- 登录页 `/login`
- 注册页 `/register`
- 工作台 `/dashboard`
- 查询管理 `/query`
- 权限管理 `/permission`
- 系统管理 `/system/users`、`/system/roles`、`/system/menus`、`/system/dicts`、`/system/logs`
- 系统配置 `/system/configs`

后端第一轮开发应优先保证这些页面能从 mock 平滑切到真实接口。

## 本轮开发原则

- 先做前端已存在页面的最小可用后端闭环，不先追求“大而全”。
- 先跑通“登录 -> 权限 bootstrap -> 动态菜单/路由 -> 列表页 -> 保存/状态修改”主链路。
- 接口结构优先兼容前端当前类型定义，避免后端先行造成联调阻塞。
- 在基础闭环完成后，再扩展更完整的双 Token、审计日志、文件上传、通知公告等通用能力。
- 所有阶段完成状态全部重置，当前统一按未完成处理。

## 每次开发前流程

1. 阅读本文件的“当前进度”和“下一步任务”。
2. 执行 `git status --short`，确认工作区状态。
3. 确认当前要开发的是哪一个阶段或子任务。
4. 实现接口前先核对前端请求参数、响应字段、分页结构、权限字段。
5. 修改代码后更新本文件中的“当前进度”“下一步任务”“完成记录”。

## 本地开发环境要求

### 本地 MySQL

- [x] 本机已安装 MySQL 5.7.44，并已按 `/Users/salty/codeProject/ai/doc/mysqluse.md` 验证可用。
- [x] 本地已存在开发数据库 `spring_admin`。
- [x] 本地已存在可用账号 `spring_admin`，可连接 `localhost:3306`。
- [ ] 如需与项目命名严格一致，再创建独立开发数据库 `spring_admin_starter`。
- [ ] 统一字符集为 `utf8mb4`。
- [ ] 在 `application-dev.yml` 中提供本地连接配置。
- [ ] 提供建表初始化脚本和基础种子数据。
- [ ] 提供管理员账号、只读账号或演示账号初始化数据，方便前端联调。

当前本地开发可先使用以下已验证配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/spring_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: spring_admin
    password: SpringAdmin@2026
```

### 本地 Redis

- [ ] 安装本地 Redis。
- [ ] 在本地开发环境中支持 `127.0.0.1:6379` 连接。
- [ ] 在 `application-dev.yml` 中提供 Redis 配置模板。
- [ ] Redis 用于缓存登录态、权限 bootstrap、字典缓存、配置缓存、限流计数。
- [ ] 提供 Redis 未启用时的报错说明，避免本地联调时定位困难。

建议本地开发默认配置：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 0
    timeout: 3000
```

### 联调与运行要求

- [ ] 允许前端本地开发地址跨域访问，默认支持 `http://localhost:5173`。
- [ ] 支持 `Authorization: Bearer <token>` 请求头。
- [ ] 提供健康检查接口，例如 `/actuator/health`。
- [ ] 提供 OpenAPI/Swagger 文档，便于前后端联调。
- [ ] 明确 dev/test/prod 三套配置文件。
- [ ] 明确日志输出目录、级别和本地调试方式。

## 统一接口契约

### 基础响应结构

后端统一返回 `ApiResponse<T>`，与前端类型保持一致：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段要求：

- `code`: 业务码，`0` 表示成功
- `message`: 用户可读提示
- `data`: 业务数据

### 分页响应结构

后端分页结构必须对齐前端当前定义：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "pageNo": 1,
    "pageSize": 10,
    "total": 0
  }
}
```

### 登录接口兼容要求

前端当前登录逻辑先按如下结构消费：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "jwt-token"
  }
}
```

因此后端第一版登录接口先兼容返回 `token` 字段，等前后端联调稳定后，再升级到更完整的双 Token 结构。

### 权限 bootstrap 结构

后端必须提供：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "menus": [],
    "routes": [],
    "actions": []
  }
}
```

字段说明：

- `menus`: 动态菜单树
- `routes`: 路由权限码数组
- `actions`: 按钮权限数组，元素结构为 `{ "code": "xxx", "name": "xxx" }`

### 菜单节点结构

后端输出的菜单节点至少支持以下字段：

- `id`
- `parentId`
- `title`
- `path`
- `icon`
- `orderNo`
- `type`
- `permissionCode`
- `hiddenInMenu`
- `redirect`
- `keepAlive`
- `externalLink`
- `badge`
- `disabled`
- `children`

其中 `type` 支持：

- `catalog`
- `menu`
- `hidden`
- `external`

## 前端已明确依赖的接口

### 认证接口

- [ ] `POST /api/auth/login`
- [ ] `POST /api/auth/register`
- [ ] `GET /api/auth/profile`

### 权限接口

- [ ] `GET /api/permission/bootstrap`
- [ ] `GET /api/permission/assignment`
- [ ] `POST /api/permission/assignment`

### 工作台接口

- [x] `GET /api/dashboard/overview`

### 查询管理接口

- [x] `GET /api/query/list`
- [x] `GET /api/query/detail`
- [x] `POST /api/query/save`

### 系统管理接口

- [ ] `GET /api/system/{moduleKey}/list`
- [ ] `GET /api/system/detail`
- [ ] `POST /api/system/save`
- [ ] `POST /api/system/status`
- [ ] `GET /api/system/menus/tree`
- [ ] `POST /api/system/menus/save`
- [ ] `POST /api/system/menus/status`
- [ ] `GET /api/system/configs`
- [ ] `POST /api/system/configs/save`

## 阶段计划

### 阶段 0：工程初始化与本地开发环境打通

目标：创建可运行、可联调、可连接本地 MySQL 和 Redis 的后端基础工程。

Deliverable：

- [x] 初始化 Maven 工程，使用 Java 8 + Spring Boot 2.7.18。
- [x] 接入 Web、Validation、Security、MyBatis-Plus、MySQL、Redis、AOP、OpenAPI。
- [x] 创建基础包结构：`common`、`auth`、`permission`、`dashboard`、`query`、`system`、`log`、`infra`。
- [x] 提供 `application.yml`、`application-dev.yml`、`application-test.yml`、`application-prod.yml`。
- [x] 配置本地 MySQL 连接模板。
- [ ] 配置本地 Redis 连接模板。
- [x] 配置统一响应结构、全局异常处理、分页对象。
- [x] 配置 CORS，允许前端本地 Vite 地址访问。
- [x] 配置 OpenAPI/Swagger 文档。
- [x] 提供健康检查接口。

验收标准：

- [x] 项目可启动。
- [x] 本地 MySQL 可成功连接。
- [ ] 本地 Redis 可成功连接。
- [x] OpenAPI 页面可访问。
- [x] `/actuator/health` 或自定义健康接口可访问。
- [x] 所有示例接口返回统一 `ApiResponse<T>`。

### 阶段 1：认证与权限最小闭环

目标：先跑通前端登录、注册、权限加载、动态菜单渲染。

Deliverable：

- [x] 实现 `POST /api/auth/login`，第一版兼容返回 `data.token`。
- [x] 实现 `POST /api/auth/register`。
- [x] 实现 `GET /api/auth/profile`。
- [x] 实现 `GET /api/permission/bootstrap`。
- [x] 设计用户、角色、菜单、角色菜单、角色按钮权限等基础表。
- [x] 初始化 `admin` 和 `viewer` 两套角色与演示数据。
- [x] 初始化菜单、路由码、按钮权限数据。
- [x] 打通 Bearer Token 鉴权。
- [x] 未登录访问受保护接口返回 401。
- [x] 无权限访问受保护资源返回 403。

验收标准：

- [x] 前端可通过真实接口完成登录。
- [x] 登录后可成功获取 `menus`、`routes`、`actions`。
- [x] `admin` 与 `viewer` 看到的菜单和权限不同。
- [x] 刷新页面后仍能依赖 token 重新拉取权限。
- [x] 路由守卫和按钮权限控制可正常生效。

### 阶段 2：工作台与查询管理接口

目标：先让前端首页和查询管理页面脱离 mock。

Deliverable：

- [x] 实现 `GET /api/dashboard/overview`。
- [x] 实现 `GET /api/query/list`。
- [x] 实现 `GET /api/query/detail`。
- [x] 实现 `POST /api/query/save`。
- [x] 查询列表支持 `keyword`、`status`、`pageNo`、`pageSize`。
- [x] 查询记录支持新增和编辑。
- [x] 查询记录支持启用和禁用。

验收标准：

- [x] 工作台页面可显示真实统计数据。
- [x] 查询管理列表分页结构与前端 `PageResult` 一致。
- [x] 查询管理新增、编辑、详情展示可联调。
- [x] 状态切换后前端列表可正确回显。

### 阶段 3：系统通用模块接口

目标：完成前端系统管理页面当前已依赖的通用接口。

Deliverable：

- [x] 实现 `GET /api/system/{moduleKey}/list`。
- [x] 实现 `GET /api/system/detail`。
- [x] 实现 `POST /api/system/save`。
- [x] 实现 `POST /api/system/status`。
- [x] 覆盖 `users`、`roles`、`dicts`、`logs` 四类模块。
- [x] 列表分页支持 `keyword`、`status`、`logType`、`pageNo`、`pageSize`。
- [x] 保证返回字段与前端 `SystemRecord` 兼容。

验收标准：

- [ ] `/system/users` 页面可正常展示列表并执行保存。
- [ ] `/system/roles` 页面可正常展示列表并执行保存。
- [ ] `/system/dicts` 页面可正常展示列表并执行保存。
- [ ] `/system/logs` 页面可正常展示日志列表。

### 阶段 4：菜单管理与权限分配

目标：完成前端权限页和菜单管理页所需的真实数据源。

Deliverable：

- [x] 实现 `GET /api/system/menus/tree`。
- [x] 实现 `POST /api/system/menus/save`。
- [x] 实现 `POST /api/system/menus/status`。
- [x] 实现 `GET /api/permission/assignment`。
- [x] 实现 `POST /api/permission/assignment`。
- [x] 菜单支持目录、菜单、隐藏路由、外链四种类型。
- [x] 菜单支持图标、排序、路径、权限码、外链地址、父子关系。
- [x] 角色权限保存后能影响 bootstrap 返回结果。

验收标准：

- [x] 菜单管理页可展示树形数据。
- [x] 菜单新增、编辑、状态修改可联调。
- [x] 权限页可读取角色的菜单、路由、按钮权限分配结果。
- [x] 保存角色权限后重新登录或重新拉取 bootstrap 能生效。

### 阶段 5：系统配置与缓存能力

目标：实现前端系统配置页，并把 Redis 真正用于缓存场景。

Deliverable：

- [x] 实现 `GET /api/system/configs`。
- [x] 实现 `POST /api/system/configs/save`。
- [x] 配置项支持 `basic`、`switch`、`cache` 三种类型。
- [x] 将权限 bootstrap、字典数据、系统配置纳入 Redis 缓存。
- [x] 配置保存后支持缓存刷新。
- [x] 提供缓存命中与刷新日志，便于排查联调问题。

验收标准：

- [x] `/system/configs` 页面可读取真实配置。
- [x] 页面修改配置后可保存成功。
- [x] Redis 中可以看到对应缓存 key。
- [x] 配置更新后缓存能够刷新。

### 阶段 6：认证增强与脚手架通用能力补全

目标：在不影响前端当前联调的前提下，把后端脚手架补到更可复用状态。

Deliverable：

- [x] 登录接口升级为双 Token 方案，并同步规划前端适配。
- [x] 增加 refresh token、登出、会话失效控制。
- [x] 增加登录日志、操作日志、异常日志。
- [ ] 增加字典模块、参数模块更完整的数据结构。
- [ ] 增加文件上传模块，支持本地文件存储。
- [ ] 增加基础 SQL 初始化脚本与二次开发说明。
- [ ] 增加本地开发 README 或启动说明。

验收标准：

- [ ] 保留对现有前端联调的平滑迁移路径。
- [ ] 日志能覆盖登录、修改、保存、权限分配等关键动作。
- [ ] 文件模块具备本地存储能力。
- [ ] 项目具备二次开发基础。

## 数据与权限初始化要求

- [x] 初始化管理员角色 `admin`。
- [x] 初始化只读角色 `viewer`。
- [x] 初始化工作台、查询管理、权限管理、用户管理、角色管理、菜单管理、字典管理、日志管理、系统配置等菜单。
- [ ] 初始化前端所需 `routeCode`，至少包括：`dashboard`、`query`、`statistics`、`permission`、`users`、`roles`、`menus`、`dicts`、`logs`、`configs`。
- [x] 初始化前端所需按钮权限码。
- [x] 初始化演示账号及默认密码。

## 安全与工程约束

- [ ] 密码不得明文存储。
- [ ] Token 仅通过认证相关接口返回。
- [x] 所有写接口保留权限控制入口。
- [x] 所有列表接口统一分页对象。
- [x] 所有异常统一返回结构化错误。
- [ ] 生产环境禁止使用开发环境默认账号密码。
- [ ] CORS 配置禁止在生产环境使用通配符。

## 当前进度

- 2026-04-17：项目已重置，原有开发完成状态全部失效。
- 2026-04-17：已根据 `react-admin-starter` 当前页面、接口类型和权限模型重新整理后端开发方向。
- 2026-04-17：已根据 `springboot_java_8_management_system_doc.md` 补充后端脚手架的基础能力范围。
- 2026-04-17：已核对 `/Users/salty/codeProject/ai/doc/mysqluse.md`，确认本机已安装并成功连接 MySQL 5.7.44，本地可用数据库为 `spring_admin`。
- 2026-04-17：已完成阶段 0 的工程骨架、多环境配置、统一响应、异常处理、CORS、OpenAPI、健康检查。
- 2026-04-17：已完成阶段 1 的登录、注册、profile、permission bootstrap、JWT 鉴权与 RBAC 初始化脚本。
- 2026-04-17：已完成阶段 2 的 dashboard/query 接口、查询表结构与种子数据，并验证分页、详情、保存与 403 返回。
- 2026-04-17：已完成阶段 3 的 system 通用模块接口，新增 users/roles/dicts/logs 四类真实数据表、种子数据和 `/api/system/{moduleKey}/list`、`/api/system/detail`、`/api/system/save`、`/api/system/status`，已完成 admin token 冒烟验证。
- 2026-04-17：已完成阶段 4 的菜单管理与权限分配接口，新增 `/api/system/menus/tree`、`/api/system/menus/save`、`/api/system/menus/status`、`/api/permission/assignment`，补充角色路由权限表并完成 bootstrap 联动验证。
- 2026-04-17：已完成阶段 5 的系统配置接口与 Redis 缓存能力，新增 `/api/system/configs`、`/api/system/configs/save`、配置表与种子数据，并完成 bootstrap、dict、config 三类缓存命中与刷新验证。
- 2026-04-17：已完成阶段 6 第一阶段，登录接口升级为双 Token，新增 `/api/auth/refresh-token`、`/api/auth/logout` 和基于 Redis 的会话失效控制，并验证 refresh 后刷新、logout 后 refresh 失效。
- 2026-04-17：已完成阶段 6 第二阶段，新增登录日志、操作日志、异常日志写入链路，并验证登录、权限分配、system 保存和异常场景会落入 `/system/logs` 列表。
- 当前已具备工作台、认证、权限、查询管理、system 通用模块、菜单/权限分配、系统配置、Redis 缓存、双 Token 会话控制和基础日志链路的最小联调能力。

## 下一步任务

1. 阶段 1 收尾：完成前端实际页面联调验证，确认路由守卫和按钮权限控制生效，并适配双 Token 登录。
2. 阶段 6 下一步：补字典模块、参数模块更完整的数据结构。
3. 继续阶段 6：增加文件上传模块、本地开发 README 和初始化脚本说明。

## 完成记录

- 2026-04-17：项目重置后，清空历史完成标记，按 `react-admin-starter` 配套后端脚手架目标重写 `todolist.md`。
- 2026-04-17：已根据 `mysqluse.md` 验证本机 MySQL 5.7.44 可用，并将 `todolist.md` 中的本地 MySQL 状态修正为当前实际环境。
- 2026-04-17：已完成阶段 0 工程初始化，创建 Spring Boot 2.7.18 工程骨架、统一响应、异常处理、CORS、OpenAPI 和健康检查。
- 2026-04-17：已完成阶段 1 最小闭环，打通 `login/register/profile/bootstrap`、JWT Bearer 鉴权，并重建当前版本所需 RBAC 表与种子数据。
- 2026-04-17：已完成阶段 2，新增 `dashboard/query` 接口、查询表结构与种子数据，并验证 403 权限返回。
- 2026-04-17：已完成阶段 3，新增 users/roles/dicts/logs 四类真实数据表与通用 system 接口，并验证 admin token 下的分页、详情、保存与状态切换。
- 2026-04-17：已完成阶段 4，新增菜单树、菜单保存、菜单状态切换、角色权限分配接口，补充 `sys_role_route` 路由权限关系表，并验证权限分配对 bootstrap 的联动生效。
- 2026-04-17：已完成阶段 5，新增系统配置表、配置种子数据、`/api/system/configs`、`/api/system/configs/save` 和 Redis 缓存基础设施，并验证 bootstrap、dict、config 三类缓存 key 与版本刷新。
