# Spring Admin Starter Todolist

## 项目定位

本项目是基于 Java 8 + Spring Boot 2.7.x 的企业级后台管理系统脚手架，面向通用管理后台快速搭建场景。后端提供认证鉴权、用户、角色、菜单、部门、字典、参数、日志、文件、公告通知等基础能力，并与 `react-admin-starter` 前端项目保持接口风格、字段命名和权限模型一致。

核心目标：

- 使用 Spring Boot 2.7.18 作为基础版本，严格兼容 Java 8。
- 使用 Spring Security + JWT + Redis 实现认证、鉴权和会话控制。
- 使用 MyBatis-Plus + MySQL 5.7/8.0 实现持久化。
- 使用 Redis 承载验证码、限流、登录失败计数、Token 会话缓存。
- 使用统一接口响应、统一异常、统一参数校验、统一日志审计。
- 支持 RBAC 权限模型、动态菜单、路由权限和按钮权限。
- 与前端 `/api/auth/*`、`/api/permission/bootstrap`、`ApiResponse<T>`、`PageResult<T>` 对齐。

## 每次开发前流程

1. 阅读本文件的“当前进度”和“下一步任务”。
2. 执行 `git status --short`，确认工作区状态，避免覆盖他人变更。
3. 按阶段顺序推进，优先完成认证鉴权模块，再实现系统基础模块。
4. 每次实现接口前先确认请求 DTO、响应 VO、错误码、权限编码和审计要求。
5. 完成开发后运行单元测试、必要的接口测试和构建验证。
6. 修改代码或文档后更新本文件的“当前进度”“下一步任务”“完成记录”。
7. 每次提交使用清晰 commit message，并在末尾追加模型后缀，例如：`实现认证鉴权基础能力 gpt-5.4`。

## 技术选型

- Java 8
- Spring Boot 2.7.18
- Spring MVC
- Spring Security 5.x
- MyBatis-Plus 3.5.x
- MySQL 5.7 / 8.0
- Redis 5.x+
- JJWT 0.9.1 或兼容 Java 8 的 JWT 实现
- Spring Validation
- Spring AOP
- springdoc-openapi 1.8.0 / Knife4j
- Maven 3.6+
- Logback + Slf4j
- Lombok
- MapStruct

## 统一接口规范

### 基础响应格式

后端统一返回 `ApiResponse<T>`，成功响应与前端项目保持一致：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段约定：

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | 业务状态码，`0` 表示成功，非 0 表示失败 |
| `message` | string | 用户可读提示 |
| `data` | object/null | 响应数据 |
| `traceId` | string | 可选，错误或审计链路追踪 ID |

### 分页响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 错误码分段

| 范围 | 类型 | 示例 |
|---|---|---|
| `0` | 成功 | `0 success` |
| `40000-40099` | 参数错误 | `40001 参数校验失败` |
| `40100-40199` | 认证错误 | `40101 登录已过期` |
| `40300-40399` | 权限错误 | `40301 无操作权限` |
| `40900-40999` | 业务冲突 | `40901 邮箱已注册` |
| `42900-42999` | 限流风控 | `42901 请求过于频繁` |
| `50000-50099` | 系统错误 | `50000 系统内部异常` |

## 阶段计划

### 阶段 0：项目初始化与工程骨架

目标：建立可运行、可测试、可扩展的 Spring Boot 后端基础工程。

Deliverable：

- [x] 初始化 Maven 工程，配置 Java 8、Spring Boot 2.7.18。
- [x] 接入 Web、Validation、Security、MyBatis-Plus、MySQL、Redis、AOP、JWT、OpenAPI。
- [x] 创建基础包结构：`common`、`auth`、`system`、`log`、`file`、`message`、`infra`。
- [x] 创建 `application.yml`、`application-dev.yml`、`application-test.yml`、`application-prod.yml`。
- [x] 创建统一响应、分页对象、异常体系、基础实体、枚举常量。
- [x] 配置全局 CORS、Jackson 时间格式、MyBatis-Plus 分页插件、逻辑删除策略。
- [x] 配置 Logback 日志滚动与不同环境日志级别。

验收标准：

- [x] `mvn clean test` 通过。
- [x] `mvn clean package` 通过。
- [x] 启动后可访问 `/actuator/health` 或自定义健康检查接口。
- [x] OpenAPI 文档可访问，且基础接口展示正常。
- [x] 所有接口返回统一 `ApiResponse<T>`。

### 阶段 1：认证鉴权模块

目标：完成邮箱验证码注册、账号密码登录、双 Token、JWT 鉴权过滤器、权限上下文和基础安全能力。

Deliverable：

- [x] 创建认证鉴权相关表：`sys_user`、`sys_user_role`、`sys_email_verify_code`、`sys_refresh_token`。
- [x] 实现邮箱验证码发送、校验、频率限制和验证码消费。
- [x] 实现仅支持邮箱验证码的注册流程。
- [x] 实现账号密码登录、登录失败限制、账号锁定和登录日志。
- [x] 实现 access token + refresh token 双 Token 签发与刷新。
- [x] 实现 Redis + 数据库的 Token 存储、撤销和续期策略。
- [x] 实现 JWT 认证过滤器、Spring Security 配置、接口白名单。
- [x] 实现当前用户上下文 `LoginUserContext` 与 `SecurityUtils`。
- [x] 实现 `/api/auth/profile` 返回当前用户、角色、权限概要。
- [x] 实现 `/api/permission/bootstrap` 返回前端动态菜单、路由权限和按钮权限。

验收标准：

- [x] 注册必须经过邮箱验证码，不能绕过验证码直接创建用户。
- [x] 密码必须 BCrypt 加密存储，数据库无明文密码。
- [x] 登录成功返回 `accessToken`、`refreshToken`、`expiresIn`、用户信息和权限 bootstrap 所需的最小信息。
- [x] access token 过期后可使用 refresh token 刷新；退出登录后 Token 立即失效。
- [x] 未登录访问受保护接口返回 `40101`；无权限访问返回 `40301`。
- [x] 前端可按 `Authorization: Bearer <accessToken>` 调用受保护接口。
- [x] `/api/permission/bootstrap` 字段能映射到前端 `PermissionBootstrap`。
- [x] 对发送验证码、注册、登录、刷新 Token、登出均有测试覆盖。

### 阶段 2：用户管理模块

目标：实现后台用户生命周期管理，支撑角色分配、状态管理、密码重置和个人资料维护。

Deliverable：

- [ ] 用户分页查询、详情、新增、编辑、删除或逻辑删除。
- [ ] 用户启用/禁用，禁用后立即踢出在线会话。
- [ ] 用户重置密码，强制下次登录修改密码预留。
- [ ] 用户绑定角色、绑定部门。
- [ ] 当前用户个人资料查询和修改。
- [ ] 用户列表支持按账号、邮箱、状态、部门、创建时间筛选。

验收标准：

- [ ] 用户管理接口具备按钮权限控制。
- [ ] 删除或禁用用户不能影响超级管理员账号。
- [ ] 用户敏感字段脱敏，响应中不返回 `password`。
- [ ] 关键操作写入操作日志。

### 阶段 3：角色与权限管理模块

目标：实现 RBAC 角色、菜单、路由、按钮权限配置。

Deliverable：

- [ ] 角色分页、详情、新增、编辑、删除、启用/禁用。
- [ ] 角色分配菜单权限、路由权限、按钮权限。
- [ ] 权限编码规范校验，防止重复编码。
- [ ] 角色权限变更后刷新用户权限缓存。

验收标准：

- [ ] 角色权限保存后 `/api/permission/bootstrap` 实时生效或按权限版本刷新。
- [ ] 禁用角色后关联用户不再获得该角色权限。
- [ ] 角色编码唯一，系统内置角色不可误删。

### 阶段 4：菜单管理模块

目标：实现前端动态菜单、路由映射和按钮权限配置的后端数据源。

Deliverable：

- [ ] 菜单树查询。
- [ ] 菜单新增、编辑、删除、排序、启用/禁用。
- [ ] 支持类型：`catalog`、`menu`、`hidden`、`external`、`button`。
- [ ] 支持字段：路由路径、组件键、图标、权限编码、排序、外链、隐藏、缓存、徽标、重定向。
- [ ] 输出前端 `PermissionMenu` 可消费结构。

验收标准：

- [ ] 菜单树支持多级目录。
- [ ] 禁用菜单及其子节点不进入普通用户 bootstrap。
- [ ] 菜单权限、路由权限、按钮权限集合独立输出。

### 阶段 5：部门管理模块

目标：实现组织架构树和用户部门归属，为后续数据权限预留基础。

Deliverable：

- [ ] 部门树查询、新增、编辑、删除、启用/禁用。
- [ ] 部门负责人、联系电话、排序、父子层级维护。
- [ ] 删除部门前校验是否存在子部门或关联用户。
- [ ] 预留数据权限字段和部门路径字段。

验收标准：

- [ ] 部门树排序稳定。
- [ ] 禁用部门后其下用户仍保留归属，但新增或迁入受限。
- [ ] 所有变更写入操作日志。

### 阶段 6：字典与参数配置模块

目标：提供前后端共用的枚举、字典和系统参数配置能力。

Deliverable：

- [ ] 字典类型、字典项 CRUD。
- [ ] 字典缓存刷新接口。
- [ ] 参数配置 CRUD。
- [ ] 参数按 key 查询，支持公开参数和内部参数隔离。
- [ ] Redis 缓存与版本刷新机制。

验收标准：

- [ ] 字典项按排序稳定输出。
- [ ] 参数 key 全局唯一。
- [ ] 敏感参数不通过普通查询接口返回明文。

### 阶段 7：日志审计模块

目标：完成登录日志、操作日志、异常日志和接口访问审计。

Deliverable：

- [ ] 登录日志记录登录成功、失败、锁定、登出。
- [ ] 操作日志基于注解 + AOP 采集模块、动作、请求参数、结果、耗时。
- [ ] 异常日志与全局异常处理打通。
- [ ] 日志查询支持账号、模块、结果、时间范围筛选。
- [ ] 敏感字段脱敏。

验收标准：

- [ ] 密码、Token、验证码、手机号、邮箱等敏感信息不完整落日志。
- [ ] 审计日志不影响主业务事务。
- [ ] 异常日志携带 traceId。

### 阶段 8：文件管理模块

目标：提供统一文件上传、下载、预览和元数据管理。

Deliverable：

- [ ] 本地存储实现。
- [ ] 文件元数据表、上传接口、下载接口。
- [ ] 文件大小、扩展名、MIME 类型校验。
- [ ] MinIO/OSS 扩展接口预留。
- [ ] 文件访问权限校验预留。

验收标准：

- [ ] 上传非法文件类型会被拒绝。
- [ ] 文件下载不暴露服务器真实路径。
- [ ] 文件记录可追踪上传人、业务类型和上传时间。

### 阶段 9：公告通知模块

目标：提供后台公告发布、用户通知和已读未读能力。

Deliverable：

- [ ] 公告 CRUD、发布、撤回。
- [ ] 通知列表、详情、标记已读。
- [ ] 按角色、部门、用户定向通知预留。
- [ ] 邮件、短信、企业微信、钉钉发送通道预留。

验收标准：

- [ ] 未发布公告不进入用户端列表。
- [ ] 已读状态按用户维度记录。
- [ ] 公告发布和撤回写入审计日志。

### 阶段 10：质量保障与生产化

目标：完善测试、接口文档、安全验证、部署和二次开发说明。

Deliverable：

- [ ] 单元测试覆盖认证、权限、验证码、Token、异常处理。
- [ ] 接口测试覆盖主要 CRUD 和权限边界。
- [ ] OpenAPI 文档补齐接口说明、错误码和示例。
- [ ] 生产环境配置模板、部署说明、Nginx 反向代理说明。
- [ ] 安全测试清单：暴力破解、越权、Token 泄露、重放、XSS、SQL 注入。

验收标准：

- [ ] `mvn clean test` 通过。
- [ ] `mvn clean package -DskipTests` 通过。
- [ ] 核心接口均有 OpenAPI 示例。
- [ ] 安全清单逐项验证并记录结果。

## 认证鉴权模块详细设计

### 1. 数据库表设计

#### 1.1 `sys_user` 用户表

用途：存储系统登录账号、个人资料、账号状态和安全状态。

字段定义：

| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| `id` | bigint unsigned | PK | 用户 ID |
| `username` | varchar(50) | NOT NULL UNIQUE | 登录账号，默认取邮箱前缀或邮箱 |
| `email` | varchar(100) | NOT NULL UNIQUE | 注册邮箱 |
| `password` | varchar(100) | NOT NULL | BCrypt 密码摘要 |
| `nick_name` | varchar(50) | NOT NULL DEFAULT '' | 昵称 |
| `real_name` | varchar(50) | NOT NULL DEFAULT '' | 真实姓名 |
| `phone` | varchar(20) | NOT NULL DEFAULT '' | 手机号 |
| `gender` | tinyint unsigned | NOT NULL DEFAULT 0 | 性别：0 未知，1 男，2 女 |
| `avatar_url` | varchar(255) | NOT NULL DEFAULT '' | 头像地址 |
| `dept_id` | bigint unsigned | NULL | 部门 ID |
| `status` | tinyint unsigned | NOT NULL DEFAULT 1 | 状态：0 禁用，1 启用 |
| `lock_until` | datetime | NULL | 锁定截止时间 |
| `login_fail_count` | int unsigned | NOT NULL DEFAULT 0 | 连续登录失败次数 |
| `last_login_time` | datetime | NULL | 最近登录时间 |
| `last_login_ip` | varchar(64) | NOT NULL DEFAULT '' | 最近登录 IP |
| `password_update_time` | datetime | NULL | 密码更新时间 |
| `remark` | varchar(255) | NOT NULL DEFAULT '' | 备注 |
| `deleted` | tinyint unsigned | NOT NULL DEFAULT 0 | 逻辑删除：0 正常，1 删除 |
| `create_by` | bigint unsigned | NULL | 创建人 |
| `create_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | bigint unsigned | NULL | 更新人 |
| `update_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

索引：

- 主键：`pk_sys_user_id`
- 唯一索引：`uk_sys_user_username_deleted(username, deleted)`
- 唯一索引：`uk_sys_user_email_deleted(email, deleted)`
- 普通索引：`idx_sys_user_dept_id(dept_id)`
- 普通索引：`idx_sys_user_status(status)`
- 普通索引：`idx_sys_user_create_time(create_time)`

#### 1.2 `sys_user_role` 用户角色关系表

用途：建立用户与角色的多对多关系。

字段定义：

| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| `id` | bigint unsigned | PK | 主键 ID |
| `user_id` | bigint unsigned | NOT NULL | 用户 ID |
| `role_id` | bigint unsigned | NOT NULL | 角色 ID |
| `create_by` | bigint unsigned | NULL | 创建人 |
| `create_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

索引：

- 唯一索引：`uk_sys_user_role(user_id, role_id)`
- 普通索引：`idx_sys_user_role_role_id(role_id)`

#### 1.3 `sys_email_verify_code` 邮箱验证码表

用途：记录邮箱验证码发送、校验、消费状态，配合 Redis 完成频率限制和短期校验。

字段定义：

| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| `id` | bigint unsigned | PK | 主键 ID |
| `email` | varchar(100) | NOT NULL | 邮箱地址 |
| `scene` | varchar(32) | NOT NULL | 场景：`REGISTER`、`RESET_PASSWORD` |
| `code_hash` | varchar(64) | NOT NULL | 验证码 SHA-256 摘要，不存明文 |
| `salt` | varchar(32) | NOT NULL | 摘要盐值 |
| `status` | tinyint unsigned | NOT NULL DEFAULT 0 | 状态：0 未使用，1 已使用，2 已过期，3 已作废 |
| `send_ip` | varchar(64) | NOT NULL DEFAULT '' | 发送 IP |
| `user_agent` | varchar(255) | NOT NULL DEFAULT '' | 请求 UA 摘要或截断值 |
| `send_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP | 发送时间 |
| `expire_time` | datetime | NOT NULL | 过期时间 |
| `verify_time` | datetime | NULL | 校验成功时间 |
| `fail_count` | int unsigned | NOT NULL DEFAULT 0 | 校验失败次数 |
| `create_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

索引：

- 普通索引：`idx_email_scene_status(email, scene, status)`
- 普通索引：`idx_email_expire_time(expire_time)`
- 普通索引：`idx_email_send_ip_time(send_ip, send_time)`

#### 1.4 `sys_refresh_token` 刷新 Token 表

用途：持久化 refresh token 会话，用于主动撤销、异地登录控制、审计和 Redis 丢失后的兜底。

字段定义：

| 字段 | 类型 | 约束 | 注释 |
|---|---|---|---|
| `id` | bigint unsigned | PK | 主键 ID |
| `user_id` | bigint unsigned | NOT NULL | 用户 ID |
| `token_id` | varchar(64) | NOT NULL UNIQUE | refresh token 的 jti |
| `token_hash` | varchar(128) | NOT NULL | refresh token SHA-256 摘要 |
| `device_id` | varchar(64) | NOT NULL DEFAULT '' | 设备 ID，前端可生成并传入 |
| `device_name` | varchar(100) | NOT NULL DEFAULT '' | 设备名称 |
| `login_ip` | varchar(64) | NOT NULL DEFAULT '' | 登录 IP |
| `user_agent` | varchar(255) | NOT NULL DEFAULT '' | UA 摘要或截断值 |
| `status` | tinyint unsigned | NOT NULL DEFAULT 1 | 状态：1 有效，0 撤销，2 过期 |
| `expire_time` | datetime | NOT NULL | 过期时间 |
| `last_refresh_time` | datetime | NULL | 最近刷新时间 |
| `revoke_time` | datetime | NULL | 撤销时间 |
| `revoke_reason` | varchar(100) | NOT NULL DEFAULT '' | 撤销原因 |
| `create_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | datetime | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

索引：

- 唯一索引：`uk_refresh_token_id(token_id)`
- 普通索引：`idx_refresh_user_status(user_id, status)`
- 普通索引：`idx_refresh_expire_time(expire_time)`
- 普通索引：`idx_refresh_device(user_id, device_id)`

#### 1.5 SQL 建表语句

```sql
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录账号',
  email VARCHAR(100) NOT NULL COMMENT '注册邮箱',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
  nick_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称',
  real_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '真实姓名',
  phone VARCHAR(20) NOT NULL DEFAULT '' COMMENT '手机号',
  gender TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
  avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像地址',
  dept_id BIGINT UNSIGNED NULL COMMENT '部门ID',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
  lock_until DATETIME NULL COMMENT '锁定截止时间',
  login_fail_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  last_login_time DATETIME NULL COMMENT '最近登录时间',
  last_login_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '最近登录IP',
  password_update_time DATETIME NULL COMMENT '密码更新时间',
  remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
  create_by BIGINT UNSIGNED NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT UNSIGNED NULL COMMENT '更新人',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username_deleted (username, deleted),
  UNIQUE KEY uk_sys_user_email_deleted (email, deleted),
  KEY idx_sys_user_dept_id (dept_id),
  KEY idx_sys_user_status (status),
  KEY idx_sys_user_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  create_by BIGINT UNSIGNED NULL COMMENT '创建人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关系表';

CREATE TABLE IF NOT EXISTS sys_email_verify_code (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  email VARCHAR(100) NOT NULL COMMENT '邮箱地址',
  scene VARCHAR(32) NOT NULL COMMENT '场景：REGISTER、RESET_PASSWORD',
  code_hash VARCHAR(64) NOT NULL COMMENT '验证码SHA-256摘要',
  salt VARCHAR(32) NOT NULL COMMENT '摘要盐值',
  status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态：0未使用，1已使用，2已过期，3已作废',
  send_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '发送IP',
  user_agent VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求UA摘要或截断值',
  send_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  verify_time DATETIME NULL COMMENT '校验成功时间',
  fail_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '校验失败次数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_email_scene_status (email, scene, status),
  KEY idx_email_expire_time (expire_time),
  KEY idx_email_send_ip_time (send_ip, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

CREATE TABLE IF NOT EXISTS sys_refresh_token (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  token_id VARCHAR(64) NOT NULL COMMENT 'Refresh Token JTI',
  token_hash VARCHAR(128) NOT NULL COMMENT 'Refresh Token SHA-256摘要',
  device_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备ID',
  device_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '设备名称',
  login_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录IP',
  user_agent VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'UA摘要或截断值',
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1有效，0撤销，2过期',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  last_refresh_time DATETIME NULL COMMENT '最近刷新时间',
  revoke_time DATETIME NULL COMMENT '撤销时间',
  revoke_reason VARCHAR(100) NOT NULL DEFAULT '' COMMENT '撤销原因',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token_id (token_id),
  KEY idx_refresh_user_status (user_id, status),
  KEY idx_refresh_expire_time (expire_time),
  KEY idx_refresh_device (user_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新Token会话表';
```

### 2. 注册模块设计

注册仅支持邮箱验证码注册，不支持手机号注册、开放用户名直接注册或第三方登录。

#### 2.1 注册流程

1. 用户在前端输入邮箱，调用 `POST /api/auth/email-code/send`。
2. 后端校验邮箱格式、是否已注册、发送频率、IP 频率、邮箱频率。
3. 后端生成 6 位数字验证码，写入 Redis 和 `sys_email_verify_code`。
4. 邮件服务发送验证码，邮件内容包含有效期和安全提示。
5. 用户输入邮箱、验证码、密码、确认密码，调用 `POST /api/auth/register`。
6. 后端校验邮箱格式、密码强度、验证码有效性、邮箱唯一性。
7. 后端创建 `sys_user`，密码使用 BCrypt，默认角色绑定为普通后台用户或待配置角色。
8. 后端将验证码标记为已使用，清理 Redis 验证码。
9. 返回注册成功。是否自动登录由配置控制，默认不自动登录，前端跳转登录页。

#### 2.2 验证码规则

| 项 | 规则 |
|---|---|
| 长度 | 6 位数字 |
| 有效期 | 10 分钟 |
| 重发间隔 | 同邮箱同场景 60 秒 |
| 邮箱频率 | 同邮箱同场景每小时最多 5 次，每天最多 20 次 |
| IP 频率 | 同 IP 每小时最多 30 次，每天最多 100 次 |
| 校验失败 | 同验证码最多失败 5 次，超过后作废 |
| 存储 | Redis 存明文短期值或摘要，数据库只存 SHA-256 摘要 |
| 消费 | 注册成功后立即消费，验证码不可重复使用 |

Redis Key：

```txt
auth:email-code:{scene}:{email}                 -> code metadata, TTL 10m
auth:email-code:cooldown:{scene}:{email}        -> 60s
auth:email-code:hour:{scene}:{email}            -> counter, TTL 1h
auth:email-code:day:{scene}:{email}             -> counter, TTL 1d
auth:email-code:ip-hour:{scene}:{ip}            -> counter, TTL 1h
auth:email-code:ip-day:{scene}:{ip}             -> counter, TTL 1d
```

#### 2.3 邮箱格式校验

- 使用 `@Email` 作为基础校验。
- 长度限制 6-100。
- 统一转小写并 trim。
- 禁止连续空白和控制字符。
- 可配置临时邮箱域名黑名单。

#### 2.4 密码强度规则

- 长度 8-32。
- 必须包含大写字母、小写字母、数字、特殊字符中的至少 3 类。
- 不允许包含空格和控制字符。
- 不允许与邮箱前缀、用户名完全一致。
- 不允许使用弱密码字典：`12345678`、`password`、`admin123`、`qwerty123` 等。
- 后端永远执行密码强度校验，前端校验只做体验优化。

#### 2.5 防暴力破解与防刷

- 发送验证码接口按邮箱、IP、User-Agent 维度限流。
- 注册接口按邮箱、IP 维度限流。
- 验证码错误次数超过阈值后验证码作废。
- 同邮箱注册成功后禁止重复注册。
- 所有限流命中返回 `42901`，提示不暴露具体风控规则。
- 记录验证码发送、注册成功、注册失败审计日志。
- 邮件发送失败不返回内部异常细节，统一提示“验证码发送失败，请稍后重试”。

#### 2.6 接口定义

##### 发送邮箱验证码

```txt
POST /api/auth/email-code/send
Content-Type: application/json
```

请求：

```json
{
  "email": "user@example.com",
  "scene": "REGISTER"
}
```

响应：

```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": {
    "expireSeconds": 600,
    "resendSeconds": 60
  }
}
```

错误码：

| code | message | 说明 |
|---|---|---|
| `40001` | 邮箱格式不正确 | 参数校验失败 |
| `40901` | 邮箱已注册 | 注册场景邮箱已存在 |
| `42901` | 请求过于频繁 | 命中邮箱或 IP 频率限制 |
| `50001` | 验证码发送失败 | 邮件服务异常 |

##### 邮箱验证码注册

```txt
POST /api/auth/register
Content-Type: application/json
```

请求：

```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "Admin@123456",
  "confirmPassword": "Admin@123456",
  "nickName": "user"
}
```

响应：

```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "userId": "10001",
    "email": "user@example.com",
    "username": "user"
  }
}
```

错误码：

| code | message | 说明 |
|---|---|---|
| `40001` | 参数校验失败 | 邮箱、验证码或密码不合法 |
| `40002` | 两次密码不一致 | `password` 与 `confirmPassword` 不一致 |
| `40003` | 密码强度不足 | 不满足密码规则 |
| `40110` | 验证码错误或已过期 | 验证码不存在、错误、过期、已使用 |
| `40901` | 邮箱已注册 | 注册时邮箱已存在 |
| `42901` | 请求过于频繁 | 注册接口限流 |

### 3. 登录模块设计

#### 3.1 登录流程

1. 前端提交账号和密码到 `POST /api/auth/login`，账号支持 `username` 或 `email`。
2. 后端按 IP 和账号检查登录频率。
3. 后端查询用户，校验用户存在、未删除、状态启用、未锁定。
4. 使用 `BCryptPasswordEncoder.matches` 校验密码。
5. 密码错误时增加登录失败次数，达到阈值后锁定账号。
6. 登录成功后清零失败次数，更新最近登录时间和 IP。
7. 构建 `LoginUser`，加载角色、菜单权限、路由权限、按钮权限。
8. 签发 access token 和 refresh token。
9. Redis 写入 access token 会话和 refresh token 会话；数据库写入 `sys_refresh_token`。
10. 返回 Token、用户摘要和必要权限信息。

#### 3.2 Token 机制

| Token | 用途 | 默认有效期 | 存储 | 说明 |
|---|---|---|---|---|
| access token | 访问业务接口 | 2 小时 | Redis | JWT，短期有效 |
| refresh token | 刷新 access token | 7 天 | Redis + DB | JWT 或随机串，支持撤销 |

JWT Claims：

| claim | 说明 |
|---|---|
| `sub` | 用户 ID |
| `jti` | Token 唯一 ID |
| `typ` | `access` 或 `refresh` |
| `username` | 登录账号 |
| `deviceId` | 设备 ID |
| `iat` | 签发时间 |
| `exp` | 过期时间 |
| `permVersion` | 权限版本，可选 |

Redis Key：

```txt
auth:access:{jti}                 -> LoginUser JSON, TTL 2h
auth:refresh:{jti}                -> Refresh session metadata, TTL 7d
auth:user-sessions:{userId}       -> active jti set, TTL 7d
auth:login-fail:user:{username}   -> counter, TTL 30m
auth:login-fail:ip:{ip}           -> counter, TTL 30m
auth:user-lock:{userId}           -> lock metadata, TTL dynamic
auth:permission:{userId}          -> PermissionBootstrap cache, TTL 30m
```

#### 3.3 Token 存储策略

- access token 不落库，只在 Redis 保存会话态，用于立即撤销和权限版本校验。
- refresh token 落 Redis 和数据库，数据库只保存 token hash，不保存明文。
- Redis 丢失时，refresh token 可通过数据库校验 token hash、状态和过期时间后重新建立 Redis 会话。
- 登出时删除当前 access token、refresh token，并将数据库 refresh token 标记撤销。
- 修改密码、禁用用户、删除用户时撤销该用户所有 refresh token 和 access token。
- 默认允许多设备登录。可通过 `auth.session.mode=single` 切换为单设备，新登录会撤销旧设备会话。

#### 3.4 登录失败限制与锁定

| 维度 | 规则 |
|---|---|
| 单账号 | 30 分钟内连续失败 5 次，锁定 30 分钟 |
| 单 IP | 30 分钟内失败 20 次，触发 IP 级限流 |
| 单账号 + IP | 10 分钟内失败 8 次，要求等待或后续扩展图形验证码 |
| 锁定提示 | 不暴露账号是否存在，统一提示“账号或密码错误，或账号暂不可用” |

登录失败写入登录日志，包含账号、IP、UA、失败原因枚举，但响应不暴露内部原因。

#### 3.5 接口定义

##### 登录

```txt
POST /api/auth/login
Content-Type: application/json
```

请求：

```json
{
  "username": "admin",
  "password": "Admin@123456",
  "deviceId": "web-uuid",
  "deviceName": "Chrome on macOS"
}
```

响应：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "accessToken": "xxx.yyy.zzz",
    "refreshToken": "aaa.bbb.ccc",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": "1",
      "username": "admin",
      "email": "admin@example.com",
      "nickName": "管理员",
      "avatarUrl": ""
    }
  }
}
```

##### 刷新 Token

```txt
POST /api/auth/refresh
Authorization: Bearer <refreshToken>
```

响应：

```json
{
  "code": 0,
  "message": "刷新成功",
  "data": {
    "accessToken": "xxx.yyy.zzz",
    "refreshToken": "aaa.bbb.ccc",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

刷新策略：默认 refresh token 轮换。刷新成功后旧 refresh token 立即撤销，返回新的 refresh token，降低泄露后的复用风险。

##### 登出

```txt
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": 0,
  "message": "退出成功",
  "data": null
}
```

##### 当前用户信息

```txt
GET /api/auth/profile
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "1",
    "username": "admin",
    "email": "admin@example.com",
    "nickName": "管理员",
    "roles": ["admin"],
    "actions": ["system:user:view", "system:user:add"]
  }
}
```

##### 权限 Bootstrap

```txt
GET /api/permission/bootstrap
Authorization: Bearer <accessToken>
```

响应与前端 `PermissionBootstrap` 对齐：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "menus": [
      {
        "id": "1",
        "parentId": null,
        "title": "工作台",
        "path": "/dashboard",
        "icon": "DashboardOutlined",
        "orderNo": 1,
        "permissionCode": "system:dashboard:view",
        "hiddenInMenu": false,
        "children": []
      }
    ],
    "routes": ["dashboard", "users", "roles", "menus"],
    "actions": [
      {
        "code": "system:user:add",
        "name": "新增用户"
      }
    ]
  }
}
```

### 4. 鉴权模块设计

#### 4.1 Spring Security 过滤器链

请求处理顺序：

1. `TraceIdFilter`：生成 traceId，写入 MDC。
2. `CorsFilter`：处理跨域。
3. `RateLimitFilter`：对认证接口和高风险接口限流。
4. `JwtAuthenticationFilter`：解析 `Authorization` 请求头。
5. `SecurityContextPersistenceFilter`：维护 SecurityContext。
6. `ExceptionTranslationFilter`：处理认证和授权异常。
7. `FilterSecurityInterceptor` / 方法级鉴权：执行权限判断。

`JwtAuthenticationFilter` 行为：

- 白名单接口直接放行。
- 缺失 Token 时不设置登录态，交给后续权限规则返回 401。
- Token 格式错误、签名错误、过期、Redis 会话不存在均返回认证失败。
- Redis 命中后构造 `UsernamePasswordAuthenticationToken`。
- 将 `LoginUser` 写入 `SecurityContextHolder`。
- 请求结束后清理上下文。

#### 4.2 Spring Security 配置

核心配置：

- 禁用默认 session：`SessionCreationPolicy.STATELESS`。
- 禁用 Form Login、HTTP Basic。
- 后台 API 默认关闭 CSRF，原因是使用 Bearer Token；如后续使用 Cookie Token，必须启用 CSRF Token。
- 配置 `AuthenticationEntryPoint` 返回 `40101`。
- 配置 `AccessDeniedHandler` 返回 `40301`。
- 开启方法级权限：`@EnableGlobalMethodSecurity(prePostEnabled = true)`。

白名单：

```txt
POST /api/auth/login
POST /api/auth/register
POST /api/auth/email-code/send
POST /api/auth/refresh
GET  /actuator/health
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /doc.html
```

注意：`/api/auth/refresh` 虽在 WebSecurity 层允许进入，但必须由 RefreshTokenService 自行校验 refresh token，不可匿名刷新。

#### 4.3 权限校验注解

Controller 方法使用 `@PreAuthorize`：

```java
@PreAuthorize("hasAuthority('system:user:view')")
@GetMapping("/api/system/users/page")
public ApiResponse<PageResult<UserPageVO>> page(UserPageQuery query) {
    return ApiResponse.success(userService.page(query));
}
```

角色判断只用于少数系统级场景，业务接口优先使用权限编码：

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
```

权限编码规范：

```txt
{domain}:{resource}:{action}
```

示例：

```txt
system:user:view
system:user:add
system:user:edit
system:user:delete
system:user:reset-password
system:role:assign-permission
system:menu:view
system:dict:refresh-cache
```

#### 4.4 当前用户上下文

封装类：

- `LoginUser`：认证主体，包含用户 ID、账号、状态、角色、权限、设备、tokenId。
- `LoginUserContext`：线程内当前用户只读视图。
- `SecurityUtils`：静态工具，提供 `getUserId()`、`getUsername()`、`hasPermission()`。
- `CurrentUserArgumentResolver`：可选，支持 Controller 参数注入当前用户。

使用原则：

- Service 层通过 `SecurityUtils.getUserId()` 获取当前用户。
- 审计字段填充通过 MyBatis-Plus `MetaObjectHandler` 统一处理。
- 异步任务不能直接依赖 `SecurityContextHolder`，必须显式传入用户上下文或使用任务装饰器复制 MDC。

### 5. 安全设计

#### 5.1 密码安全

- 使用 `BCryptPasswordEncoder`，强度默认 10，可配置。
- 密码永不打印日志，永不返回前端。
- 修改密码后撤销该用户所有会话。
- 密码重置必须生成随机临时密码或通过邮箱验证码重置，管理员重置后建议强制首次登录修改。
- 数据库只保存 BCrypt 摘要，不保存盐字段，BCrypt 自带盐。

#### 5.2 Token 安全

- access token 短有效期，默认 2 小时。
- refresh token 长有效期，默认 7 天，并启用轮换。
- refresh token 数据库存 hash，不存明文。
- Token JWT secret 必须来自环境变量或安全配置中心，禁止写死在代码中。
- JWT 签名算法使用 HS256 或更高强度配置；密钥长度至少 256 bit。
- 登出、禁用用户、修改密码、角色权限变更必须清理或刷新相关缓存。
- 单设备/多设备策略通过配置控制，默认多设备。
- 响应中 Token 字段只在登录和刷新时返回，其他接口不返回。

#### 5.3 防 CSRF

- Bearer Token 存储在前端 localStorage 时，CSRF 风险较 Cookie 方案低，但要重点防 XSS。
- 后端关闭 Spring Security 默认 CSRF，仅适用于纯 Bearer Token API。
- 如果未来改成 HttpOnly Cookie 存 Token，必须启用 CSRF Token 或 SameSite 策略。
- CORS 只允许配置的前端域名，不使用生产环境通配 `*`。

#### 5.4 防重放攻击

- HTTPS 是生产环境强制要求。
- access token 过期时间短，降低重放窗口。
- refresh token 轮换，旧 refresh token 立即失效。
- 高风险接口可增加 `X-Request-Id` 幂等键或时间戳 + nonce 校验。
- 对密码修改、权限分配、用户禁用等敏感操作记录审计日志。

#### 5.5 接口限流

限流优先使用 Redis 计数器或 Redisson RateLimiter。

默认规则：

| 接口 | 规则 |
|---|---|
| 发送邮箱验证码 | 邮箱、IP 双维度限流 |
| 注册 | IP 每分钟最多 5 次 |
| 登录 | 账号、IP 双维度限流 |
| 刷新 Token | 单 refresh jti 每分钟最多 10 次 |
| 导出接口 | 用户维度每分钟最多 3 次 |

命中限流统一返回：

```json
{
  "code": 42901,
  "message": "请求过于频繁，请稍后重试",
  "data": null
}
```

#### 5.6 敏感信息脱敏

响应脱敏：

- 邮箱：`u***@example.com`
- 手机号：`138****8000`
- Token：不进入普通响应对象。
- 密码、验证码：永不返回。

日志脱敏：

- 请求参数中的 `password`、`confirmPassword`、`token`、`accessToken`、`refreshToken`、`code` 必须过滤。
- 登录失败日志记录失败原因枚举，不记录密码。
- 异常堆栈仅写服务端日志，前端返回统一错误信息和 traceId。

### 6. 代码结构

#### 6.1 包结构

```txt
com.salty.admin
├── AdminApplication.java
├── common
│   ├── api
│   │   ├── ApiResponse.java
│   │   ├── PageQuery.java
│   │   └── PageResult.java
│   ├── base
│   │   ├── BaseEntity.java
│   │   └── BaseController.java
│   ├── config
│   │   ├── WebMvcConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   └── JacksonConfig.java
│   ├── constant
│   ├── enums
│   │   ├── ErrorCode.java
│   │   ├── UserStatus.java
│   │   └── YesNo.java
│   ├── exception
│   │   ├── BusinessException.java
│   │   ├── UnauthorizedException.java
│   │   ├── ForbiddenException.java
│   │   └── GlobalExceptionHandler.java
│   ├── security
│   │   ├── LoginUser.java
│   │   ├── SecurityUtils.java
│   │   └── CurrentUser.java
│   └── util
├── auth
│   ├── controller
│   │   └── AuthController.java
│   ├── service
│   │   ├── AuthService.java
│   │   ├── EmailCodeService.java
│   │   ├── TokenService.java
│   │   └── LoginAttemptService.java
│   ├── service.impl
│   ├── security
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── JwtAccessDeniedHandler.java
│   ├── dto
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── SendEmailCodeRequest.java
│   │   └── RefreshTokenRequest.java
│   ├── vo
│   │   ├── LoginResponse.java
│   │   ├── RegisterResponse.java
│   │   ├── TokenResponse.java
│   │   └── ProfileVO.java
│   └── support
│       ├── JwtProperties.java
│       ├── JwtTokenProvider.java
│       └── PasswordPolicyValidator.java
├── system
│   ├── user
│   ├── role
│   ├── menu
│   ├── dept
│   ├── dict
│   ├── config
│   └── permission
├── log
├── file
├── message
└── infra
    ├── redis
    ├── mail
    ├── rateLimit
    └── id
```

#### 6.2 关键类说明

| 类 | 职责 |
|---|---|
| `AuthController` | 暴露登录、注册、验证码、刷新、登出、当前用户接口 |
| `AuthService` | 编排注册、登录、登出主流程 |
| `EmailCodeService` | 验证码生成、发送、校验、消费、限流 |
| `TokenService` | 双 Token 签发、解析、刷新、撤销、Redis/DB 同步 |
| `LoginAttemptService` | 登录失败计数、账号锁定、IP 限流 |
| `JwtTokenProvider` | JWT 创建、签名、解析、Claims 校验 |
| `JwtAuthenticationFilter` | 请求级 Token 校验并写入 SecurityContext |
| `SecurityConfig` | Spring Security 白名单、异常处理、过滤器链配置 |
| `UserDetailsServiceImpl` | 加载用户、角色、权限并构建认证主体 |
| `PermissionService` | 聚合菜单、路由、按钮权限并输出 bootstrap |
| `PasswordPolicyValidator` | 密码强度校验 |
| `GlobalExceptionHandler` | 统一异常响应和 traceId 输出 |
| `OperationLogAspect` | 操作日志采集和脱敏 |

#### 6.3 DTO 定义

`SendEmailCodeRequest`：

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `email` | String | `@NotBlank @Email @Size(max=100)` | 邮箱 |
| `scene` | String | `@NotBlank` | 场景，注册为 `REGISTER` |

`RegisterRequest`：

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `email` | String | `@NotBlank @Email @Size(max=100)` | 邮箱 |
| `code` | String | `@Pattern("\\d{6}")` | 邮箱验证码 |
| `password` | String | `@Size(min=8,max=32)` | 密码 |
| `confirmPassword` | String | `@NotBlank` | 确认密码 |
| `nickName` | String | `@Size(max=50)` | 昵称，可选 |

`LoginRequest`：

| 字段 | 类型 | 校验 | 说明 |
|---|---|---|---|
| `username` | String | `@NotBlank @Size(max=100)` | 用户名或邮箱 |
| `password` | String | `@NotBlank @Size(max=64)` | 密码 |
| `deviceId` | String | `@Size(max=64)` | 设备 ID |
| `deviceName` | String | `@Size(max=100)` | 设备名称 |

#### 6.4 VO 定义

`LoginResponse`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `accessToken` | String | Access Token |
| `refreshToken` | String | Refresh Token |
| `tokenType` | String | 固定 `Bearer` |
| `expiresIn` | Long | access token 剩余秒数 |
| `user` | UserProfileVO | 当前用户摘要 |

`PermissionBootstrapVO`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `menus` | List<PermissionMenuVO> | 动态菜单树 |
| `routes` | List<String> | 路由权限码 |
| `actions` | List<PermissionActionVO> | 按钮权限 |

`PermissionMenuVO` 与前端一致：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | String | 菜单 ID |
| `parentId` | String | 父菜单 ID |
| `title` | String | 菜单名称 |
| `path` | String | 路由路径 |
| `icon` | String | Ant Design Icon 名称 |
| `orderNo` | Integer | 排序 |
| `permissionCode` | String | 菜单权限编码 |
| `hiddenInMenu` | Boolean | 是否隐藏 |
| `children` | List<PermissionMenuVO> | 子菜单 |

## 其他后端模块概要设计

### 用户管理模块

用户管理负责后台账号的完整生命周期，包括新增、编辑、分页查询、状态切换、删除、重置密码、角色分配和部门归属。该模块必须与认证模块联动：禁用、删除用户或重置密码后，应撤销用户现有 Token，避免已登录会话继续访问系统。

用户列表接口需要支持前端系统管理页面的筛选和分页，并统一返回 `PageResult<UserPageVO>`。响应中禁止返回密码、Token、验证码等敏感字段，邮箱和手机号可按场景脱敏。

关键接口：

```txt
GET    /api/system/users/page
GET    /api/system/users/{id}
POST   /api/system/users
PUT    /api/system/users/{id}
DELETE /api/system/users/{id}
POST   /api/system/users/{id}/status
POST   /api/system/users/{id}/reset-password
POST   /api/system/users/{id}/roles
GET    /api/system/users/profile
PUT    /api/system/users/profile
```

### 角色与权限管理模块

角色模块是 RBAC 权限模型的核心，负责角色基础信息维护、角色状态控制和权限分配。权限分配结果要覆盖菜单权限、路由权限和按钮权限，分别服务于前端侧边栏展示、路由守卫和按钮控制。

角色权限变更后，需要清理关联用户的权限缓存，或提升权限版本 `permVersion`，确保用户重新获取 bootstrap 时拿到最新权限。系统内置角色如 `SUPER_ADMIN` 应受保护，不允许普通管理员删除或降级。

关键接口：

```txt
GET    /api/system/roles/page
GET    /api/system/roles/{id}
POST   /api/system/roles
PUT    /api/system/roles/{id}
DELETE /api/system/roles/{id}
POST   /api/system/roles/{id}/status
GET    /api/system/roles/{id}/permissions
POST   /api/system/roles/{id}/permissions
```

### 菜单管理模块

菜单模块维护前端动态菜单树和权限编码，是前后端权限对齐的关键数据源。菜单节点应支持目录、菜单、隐藏路由、外链、按钮五种类型，并保存路由路径、组件键、图标、排序、权限编码、显示状态等字段。

后端对外输出时需要转换为前端 `PermissionMenu` 模型；按钮权限不进入侧边栏菜单树，但需要进入 `actions` 集合。菜单删除前必须校验子节点和角色授权关系，避免产生孤儿权限。

关键接口：

```txt
GET    /api/system/menus/tree
GET    /api/system/menus/{id}
POST   /api/system/menus
PUT    /api/system/menus/{id}
DELETE /api/system/menus/{id}
POST   /api/system/menus/{id}/status
POST   /api/system/menus/sort
GET    /api/permission/bootstrap
```

### 部门管理模块

部门模块提供组织架构树，支撑用户部门归属和后续数据权限扩展。部门表建议保存 `parent_id`、`ancestors` 或 `tree_path` 字段，便于查询子树和做数据权限过滤。

部门删除需要校验是否存在子部门或关联用户；禁用部门后不应直接禁用部门下用户，但应限制新增用户绑定禁用部门。所有组织调整都要写操作日志。

关键接口：

```txt
GET    /api/system/depts/tree
GET    /api/system/depts/{id}
POST   /api/system/depts
PUT    /api/system/depts/{id}
DELETE /api/system/depts/{id}
POST   /api/system/depts/{id}/status
```

### 字典管理模块

字典模块维护前后端共用的枚举类基础数据，例如状态、类型、标签、业务分类。字典分为字典类型和字典项，字典项包含 label、value、排序、状态、样式类型等字段。

常用字典应缓存到 Redis，前端或后端业务使用时按 `dictType` 查询。修改字典后需要清理缓存或更新缓存版本，避免旧值长期生效。

关键接口：

```txt
GET    /api/system/dicts/types/page
POST   /api/system/dicts/types
PUT    /api/system/dicts/types/{id}
DELETE /api/system/dicts/types/{id}
GET    /api/system/dicts/items
POST   /api/system/dicts/items
PUT    /api/system/dicts/items/{id}
DELETE /api/system/dicts/items/{id}
POST   /api/system/dicts/refresh-cache
```

### 参数配置模块

参数配置模块用于维护系统运行期可调整的配置项，例如登录策略、上传限制、默认密码策略、前端缓存版本等。参数需要区分公开参数、内部参数和敏感参数，敏感参数不得通过普通接口明文返回。

参数读取应优先走缓存，保存后刷新 Redis。涉及安全策略的参数变更必须记录审计日志，并限制只有高权限角色可操作。

关键接口：

```txt
GET  /api/system/configs/page
GET  /api/system/configs/{id}
POST /api/system/configs
PUT  /api/system/configs/{id}
POST /api/system/configs/{id}/status
GET  /api/system/configs/public
POST /api/system/configs/refresh-cache
```

### 日志审计模块

日志审计模块覆盖登录日志、操作日志、异常日志和接口访问日志。登录日志由认证模块写入，操作日志通过注解和 AOP 采集，异常日志由全局异常处理器统一落库或落日志系统。

日志查询接口应支持时间范围、账号、模块、业务类型、结果状态和 IP 筛选。日志内容必须做脱敏，禁止记录密码、验证码和完整 Token。

关键接口：

```txt
GET    /api/system/logs/login/page
GET    /api/system/logs/operation/page
GET    /api/system/logs/error/page
GET    /api/system/logs/access/page
GET    /api/system/logs/{id}
DELETE /api/system/logs/{id}
POST   /api/system/logs/clean
```

### 文件管理模块

文件管理模块统一处理上传、下载、预览和元数据维护。第一版实现本地存储，抽象 `FileStorageService`，后续可扩展 MinIO、OSS、S3 等对象存储。

上传接口必须校验文件大小、扩展名、MIME 类型和业务类型。下载接口通过文件 ID 获取，不暴露服务器真实路径，并按业务需要校验访问权限。

关键接口：

```txt
POST /api/files/upload
GET  /api/files/{id}
GET  /api/files/{id}/download
GET  /api/files/page
POST /api/files/{id}/delete
```

### 公告通知模块

公告通知模块提供站内公告和用户通知能力。公告由管理员创建、编辑、发布、撤回，用户端按发布时间和状态查询可见公告。

通知建议预留按角色、部门、用户定向能力，并记录用户维度已读状态。第一版只实现站内消息，邮件、短信、企业微信、钉钉作为通道接口预留。

关键接口：

```txt
GET    /api/system/notices/page
GET    /api/system/notices/{id}
POST   /api/system/notices
PUT    /api/system/notices/{id}
DELETE /api/system/notices/{id}
POST   /api/system/notices/{id}/publish
POST   /api/system/notices/{id}/revoke
GET    /api/messages/page
POST   /api/messages/{id}/read
POST   /api/messages/read-all
```

## 工程规范要求

### 统一返回格式

- Controller 统一返回 `ApiResponse<T>`，分页统一返回 `PageResult<T>`。
- 成功 `code=0`，失败使用业务错误码，不直接把 HTTP 状态码当业务码。
- HTTP 状态仍按语义设置：参数错误 400，未认证 401，无权限 403，限流 429，系统错误 500。
- `message` 面向用户，日志中记录更详细的开发诊断信息。

### 全局异常处理

- 使用 `@RestControllerAdvice` 统一处理异常。
- `MethodArgumentNotValidException`、`ConstraintViolationException` 返回 `40001`。
- `BusinessException` 返回自身错误码。
- `AuthenticationException` 返回 `40101`。
- `AccessDeniedException` 返回 `40301`。
- 未知异常返回 `50000`，并输出 traceId。
- 异常处理器必须对敏感字段脱敏后再记录日志。

### 参数校验规范

- DTO 使用 `javax.validation` 注解。
- Controller 类或方法使用 `@Validated`。
- 字符串字段明确 `@NotBlank`、`@Size`、`@Pattern`。
- 分页参数限制 `pageSize <= 100`，避免大分页拖垮数据库。
- 枚举字段使用自定义枚举校验注解或显式转换。
- Service 层保留关键业务校验，不能只依赖前端或 Controller 校验。

### 日志规范

- 使用 Slf4j + Logback，不使用 `System.out.println`。
- 所有请求生成 traceId，并写入 MDC。
- 关键业务操作使用 `@OperationLog` 注解。
- 日志中禁止输出密码、验证码、Token、密钥和完整身份证件信息。
- 生产日志按天滚动，错误日志单独文件。
- 异步日志和审计日志不能阻塞核心业务链路。

### 命名规范

- Java 类名使用大驼峰：`UserService`、`AuthController`。
- 方法名和变量名使用小驼峰：`getUserPage`。
- 常量使用大写下划线：`DEFAULT_PAGE_SIZE`。
- 表名和字段名使用小写下划线：`sys_user`、`create_time`。
- DTO 命名：`XxxRequest`、`XxxQuery`。
- VO 命名：`XxxVO`、`XxxResponse`。
- Mapper 命名：`XxxMapper`。
- 权限编码使用小写英文和冒号分段：`system:user:view`。
- 路由编码使用稳定短码：`dashboard`、`users`、`roles`。

### Git 提交规范

提交信息格式：

```txt
<类型><范围可选>: <描述> <模型后缀>
```

示例：

```txt
feat(auth): 实现邮箱验证码注册 gpt-5.4
fix(security): 修复刷新Token重复使用问题 gpt-5.4
docs: 更新认证鉴权设计 gpt-5.4
```

类型建议：

| 类型 | 说明 |
|---|---|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档 |
| `refactor` | 重构 |
| `test` | 测试 |
| `chore` | 构建、配置、依赖 |
| `perf` | 性能优化 |
| `security` | 安全加固 |

提交要求：

- 每次提交聚焦一个主题。
- 不提交本地密钥、`.env`、临时日志、大体积构建产物。
- 提交前执行 `git status --short` 检查变更范围。
- 认证、权限、数据库结构变更必须在 commit message 或 PR 描述中说明影响面。

## 当前进度

- 2026-04-17：已根据后端设计文档和前端项目 README/TODO 创建后端开发计划。
- 2026-04-17：完成阶段 0 工程骨架初始化（Maven 工程、统一响应/异常、多环境配置、CORS、日志等）。
- 2026-04-17：完成阶段 1 认证鉴权模块（邮箱验证码注册、密码登录、双 Token、JWT 过滤器、Spring Security、权限 bootstrap）。
- 当前已完成阶段 0 和阶段 1，Claude 评审均通过。mvn clean package 构建成功。

## 下一步任务

1. 阶段 2：用户管理模块（用户 CRUD、分页、状态管理、角色分配、密码重置、个人资料）。
2. 阶段 3：角色与权限管理模块。
3. 阶段 4：菜单管理模块。

## 完成记录

- 2026-04-17：创建 `todolist.md`，包含阶段计划、认证鉴权详细设计、其他后端模块概要设计和工程规范。gpt-5.4
