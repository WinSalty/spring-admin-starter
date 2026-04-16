# 基于 Java 8 的 Spring Boot 管理类系统文档

## 1. 文档概述

### 1.1 文档目的
本文档用于描述基于 **Java 8 + Spring Boot** 技术栈开发的管理类系统整体设计方案，作为项目立项、需求分析、系统设计、开发实现、测试验收与后期运维的依据。

### 1.2 适用范围
本文档面向 **用于快速搭建([docs.spring.io](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/getting-started.html?utm_source=chatgpt.com))工程模板，在此基础上可快速派生出如下系统：
- 企业运营后台
- ERP/WMS/CRM/OA 后台
- 设备管理后台
- 内容管理后台
- 数据管理后台
- 审批与流程后台

本模板的目标不是直接绑定某一行业，而是提供一套 **可复用、可扩展、可快速二次开发** 的后台通用能力。

### 1.3 目标读者
- 产品经理
- 项目经理
- 系统架构师
- 开发工程师
- 测试工程师
- 运维工程师

### 1.4 参考技术版本
| 技术/组件 | 版本 |
|---|---|
| Java | 1.8 |
| Spring Boot | 2.x |
| Spring MVC | 随 Spring Boot |
| Spring Data JPA / MyBatis | 可选 |
| Maven | 3.6+ |
| MySQL | 5.7 / 8.0 |
| Redis | 5.x |
| Tomcat | 内嵌 |
| JWT | 鉴权方案 |
| Swagger / Knife4j | 接口文档 |
| Nginx | 反向代理 |

---

## 2. 项目概述

### 2.1 项目背景
随着企业信息化建设不断推进，传统人工管理方式已难以满足高效率、可追溯、可扩展的业务需求。为提升业务管理效率、规范数据流转、增强权限控制与审计能力，建设一套基于 Spring Boot 的管理类系统具有较高的实际价值。

### 2.2 建设目标
本项目定位为 **Java 8 + Spring Boot 通用后台脚手架**，目标是帮助研发团队在新项目启动时，快速完成基础框架、权限体系、配置体系和通用管理能力的搭建，减少重复造轮子。

建设目标如下：
1. 提供开箱即用的后台基础能力。
2. 提供统一用户、角色、菜单、权限、字典、日志等平台模块。
3. 提供统一接口规范、异常处理、参数校验、日志审计与安全机制。
4. 提供适合二次开发的分层结构与模块划分。
5. 提供统一的数据库设计风格与基础表结构。
6. 作为后续业务模块接入的基础平台。

### 2.3 系统特点
- 面向“快速搭建管理后台”的通用脚手架设计
- 平台模块与业务模块解耦，便于二次开发
- 支持前后端分离，接口风格统一
- 支持 RBAC 权限模型与菜单路由控制
- 支持统一响应封装、统一异常处理、统一审计日志
- 支持分页查询、字典管理、参数管理、文件上传等通用能力
- 适用于中小型后台项目快速启动，也便于后期横向扩展

---

## 3. 需求分析

### 3.1 功能需求

从“快速搭建通用后台项目”的目标出发，系统功能分为 **平台基础模块** 与 **可扩展业务模块** 两大类。

#### 3.1.1 平台基础模块

##### （1）认证与会话模块
- 账号密码登录
- Token/JWT 鉴权
- 当前登录用户信息获取
- 退出登录
- 登录失败限制
- 验证码（可选）
- 单设备/多设备登录策略（可扩展）

##### （2）用户中心模块
- 用户新增、编辑、删除
- 用户分页查询
- 用户状态启用/禁用
- 重置密码
- 绑定角色
- 用户归属部门
- 用户个人资料维护

##### （3）组织架构模块
- 部门管理
- 岗位管理（建议预留）
- 部门树维护
- 人员与组织关系维护

##### （4）权限中心模块
- 角色管理
- 菜单管理
- 按钮权限管理
- 数据权限预留
- 角色授权
- 用户授权
- 动态菜单与前端路由支持

##### （5）系统配置模块
- 参数配置
- 数据字典配置
- 区域/枚举/标签类基础数据配置
- 开关项配置
- 上传配置

##### （6）审计与日志模块
- 登录日志
- 操作日志
- 异常日志
- 审计追踪
- 接口访问日志（可选）

##### （7）通知与消息模块
- 站内通知（基础版）
- 公告管理
- 消息已读未读管理
- 邮件/短信/企业微信/钉钉扩展接口预留

##### （8）附件与文件模块
- 文件上传
- 文件下载
- 文件预览
- 文件元数据管理
- 本地存储/对象存储扩展预留

##### （9）开发支撑模块
- 统一返回对象
- 全局异常处理
- 参数校验
- 分页对象封装
- 通用枚举
- 通用状态字段
- 代码生成器（建议预留）
- 接口文档
- 定时任务（建议预留）

#### 3.1.2 可扩展业务模块
在平台基础模块之上，可按实际项目快速扩展：
- 商品管理
- 订单管理
- 客户管理
- 设备管理
- 工单管理
- 内容管理
- 审批流管理
- 报表统计模块

#### 3.1.3 模块拆分建议
为便于快速搭建与后期维护，建议按以下思路拆分模块：
- `system`：系统基础模块（用户、角色、菜单、部门、字典、参数）
- `auth`：认证授权模块
- `log`：日志与审计模块
- `file`：文件管理模块
- `message`：通知公告模块
- `common`：公共组件模块
- `biz-xxx`：业务扩展模块

### 3.2 非功能需求

#### 3.2.1 性能需求
- 普通接口响应时间不超过 2 秒
- 支持 200~1000 并发用户访问（视部署资源而定）
- 支持常用热点数据缓存

#### 3.2.2 安全需求
- 用户密码加密存储
- 接口权限校验
- 防 SQL 注入、防 XSS、防 CSRF
- 敏感操作日志留痕
- Token 过期机制

#### 3.2.3 可维护性需求
- 代码结构清晰
- 模块分层明确
- 日志规范统一
- 配置外置化
- 支持环境隔离（dev/test/prod）

#### 3.2.4 可扩展性需求
- 支持新增业务模块
- 支持数据库表结构扩展
- 支持缓存、消息队列、定时任务扩展

---

## 4. 总体设计

### 4.1 系统架构
系统采用典型的分层架构设计：
- 表现层（Controller）
- 业务层（Service）
- 数据访问层（DAO/Mapper/Repository）
- 持久化层（MySQL/Redis）

### 4.2 技术架构说明
- **Spring Boot**：作为项目基础框架，简化配置与启动流程。
- **Spring MVC**：负责 RESTful API 开发。
- **Spring Security / JWT**：实现用户认证与权限校验。
- **MyBatis 或 JPA**：负责数据库操作。
- **MySQL**：持久化存储业务数据。
- **Redis**：缓存热点数据、验证码、Token 会话信息。
- **Swagger/Knife4j**：生成在线接口文档。
- **Maven**：项目依赖与构建管理。

### 4.3 部署架构
部署结构建议如下：
1. 前端系统部署在 Nginx。
2. 后端 Spring Boot 项目以 Jar 包方式运行。
3. MySQL 独立部署。
4. Redis 独立部署。
5. 日志统一落盘，可接入 ELK。

### 4.4 分层说明

#### 4.4.1 Controller 层
负责接收前端请求、参数校验、统一返回结果，不处理复杂业务逻辑。

#### 4.4.2 Service 层
负责核心业务逻辑处理、事务控制、数据校验、业务规则封装。

#### 4.4.3 DAO/Mapper 层
负责数据库访问，实现数据的增删改查。

#### 4.4.4 Entity/DTO/VO 层
- Entity：数据库实体对象
- DTO：数据传输对象
- VO：视图展示对象

---

## 5. 功能模块设计

本项目以“**通用后台脚手架**”为核心定位，模块设计遵循“**平台通用能力沉淀 + 业务模块灵活扩展**”原则。

### 5.1 模块总体划分
建议划分为以下五层：
1. **认证层**：登录、鉴权、Token、会话控制。
2. **平台层**：用户、角色、菜单、部门、字典、参数、日志。
3. **支撑层**：文件、消息、任务、缓存、接口文档。
4. **公共层**：统一响应、异常、工具类、常量、枚举、基础对象。
5. **业务层**：具体项目业务模块，如订单、设备、审批等。

### 5.2 通用模块设计说明

#### 5.2.1 认证授权模块（auth）
**模块定位：** 提供所有后台项目都需要的统一登录认证与权限校验能力。

**建议功能：**
- 登录/退出
- JWT Token 签发与解析
- 用户会话续期
- 当前用户上下文获取
- 权限注解控制
- 接口白名单配置
- 登录验证码（可选）
- 登录失败次数限制（可选）

**输出能力：**
- 统一登录接口
- 统一认证过滤器
- 统一鉴权拦截器
- 当前登录用户工具类

#### 5.2.2 用户与组织模块（system-user / system-org）
**模块定位：** 提供后台系统最基础的人员与组织管理能力。

**建议功能：**
- 用户管理
- 部门管理
- 岗位管理（预留）
- 用户与部门关系维护
- 用户状态管理
- 用户密码管理
- 用户资料管理

**设计建议：**
- 用户表与组织表独立维护
- 岗位表可作为可选模块
- 预留数据权限字段，如 `dept_id`、`creator_id`

#### 5.2.3 权限中心模块（system-permission）
**模块定位：** 为所有后台项目提供统一的访问控制体系。

**建议功能：**
- 角色管理
- 菜单管理
- 按钮权限管理
- 用户角色绑定
- 角色菜单绑定
- 菜单树构建
- 数据权限策略预留

**设计建议：**
- 采用 RBAC 模型
- 菜单分为目录、菜单、按钮三类
- 接口权限与前端按钮权限统一使用权限标识控制

#### 5.2.4 配置中心模块（system-config）
**模块定位：** 沉淀后台项目常见的可配置能力，避免将可变内容写死在代码中。

**建议功能：**
- 参数配置
- 字典配置
- 字典明细配置
- 开关配置
- 上传配置
- 第三方配置项预留

**设计建议：**
- 参数与字典建议做缓存
- 字典适合前后端共用
- 可预留配置刷新机制

#### 5.2.5 日志审计模块（system-log）
**模块定位：** 为后台系统提供问题追踪、行为留痕与安全审计能力。

**建议功能：**
- 登录日志
- 操作日志
- 异常日志
- 审计查询
- 按用户/模块/时间范围筛选

**设计建议：**
- 操作日志建议采用注解+AOP采集
- 异常日志建议与全局异常处理打通
- 生产环境注意脱敏处理

#### 5.2.6 文件中心模块（infra-file）
**模块定位：** 统一处理后台管理系统中的上传、下载、预览等能力。

**建议功能：**
- 文件上传
- 文件下载
- 文件预览
- 文件分类管理
- 文件元数据记录
- 本地/MinIO/OSS 扩展预留

#### 5.2.7 消息公告模块（system-message）
**模块定位：** 作为通用后台项目的轻量通知中心。

**建议功能：**
- 公告发布
- 通知列表
- 已读/未读管理
- 消息模板预留
- 第三方通知渠道扩展预留

#### 5.2.8 开发支撑模块（common / infra）
**模块定位：** 提供可复用的基础代码能力，降低每个业务模块的重复开发成本。

**建议包含：**
- 统一返回对象 `Result<T>`
- 分页对象 `PageQuery` / `PageResult<T>`
- 全局异常处理
- 参数校验
- 基础实体 `BaseEntity`
- 常量类、枚举类
- ID 生成策略
- 时间与用户审计字段自动填充
- Swagger/OpenAPI 接口文档
- 代码生成器（可选）
- 定时任务（可选）

### 5.3 模块落地建议
对于“快速启动项目”的场景，推荐采用以下初始化组合：
- 必选模块：认证授权、用户组织、权限中心、配置中心、日志审计、公共支撑
- 常用模块：文件中心、消息公告
- 按需扩展模块：定时任务、代码生成器、报表统计、工作流

### 5.4 推荐的后台脚手架包结构
```text
com.example.admin
├── common                # 公共基础层
│   ├── api               # 统一返回与分页对象
│   ├── config            # 通用配置
│   ├── constant          # 常量
│   ├── enums             # 枚举
│   ├── exception         # 异常体系
│   ├── utils             # 工具类
│   └── base              # 基础实体/基础VO
├── auth                  # 认证授权模块
├── system
│   ├── user              # 用户模块
│   ├── role              # 角色模块
│   ├── menu              # 菜单模块
│   ├── dept              # 部门模块
│   ├── dict              # 字典模块
│   ├── config            # 参数配置模块
│   └── notice            # 公告通知模块
├── log                   # 日志审计模块
├── file                  # 文件管理模块
└── biz                   # 业务扩展模块
```

## 6. 数据库设计

### 6.1 数据库设计原则
1. 满足第三范式要求，必要时适度冗余。
2. 表字段命名规范统一。
3. 必须包含主键、创建时间、更新时间等通用字段。
4. 关键业务表必须设置索引。

### 6.2 核心数据表设计

#### 6.2.1 用户表 sys_user
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| username | varchar(50) | 用户名 |
| password | varchar(100) | 密码 |
| nick_name | varchar(50) | 昵称 |
| real_name | varchar(50) | 真实姓名 |
| phone | varchar(20) | 手机号 |
| email | varchar(100) | 邮箱 |
| gender | tinyint | 性别 |
| dept_id | bigint | 部门ID |
| status | tinyint | 状态 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 6.2.2 角色表 sys_role
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| role_name | varchar(50) | 角色名称 |
| role_code | varchar(50) | 角色编码 |
| remark | varchar(255) | 备注 |
| status | tinyint | 状态 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 6.2.3 权限表 sys_permission
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| permission_name | varchar(100) | 权限名称 |
| permission_code | varchar(100) | 权限标识 |
| type | varchar(20) | 类型 |
| path | varchar(200) | 路径 |
| component | varchar(200) | 组件 |
| parent_id | bigint | 父级ID |
| sort | int | 排序 |
| status | tinyint | 状态 |

#### 6.2.4 用户角色关系表 sys_user_role
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 用户ID |
| role_id | bigint | 角色ID |

#### 6.2.5 角色权限关系表 sys_role_permission
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| role_id | bigint | 角色ID |
| permission_id | bigint | 权限ID |

#### 6.2.6 部门表 sys_dept
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| dept_name | varchar(100) | 部门名称 |
| parent_id | bigint | 父部门ID |
| leader | varchar(50) | 负责人 |
| phone | varchar(20) | 联系电话 |
| status | tinyint | 状态 |
| create_time | datetime | 创建时间 |

#### 6.2.7 操作日志表 sys_oper_log
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| module | varchar(100) | 模块名称 |
| business_type | varchar(50) | 业务类型 |
| method | varchar(200) | 请求方法 |
| request_url | varchar(255) | 请求地址 |
| oper_name | varchar(50) | 操作人 |
| oper_ip | varchar(50) | 操作IP |
| oper_param | text | 请求参数 |
| json_result | text | 返回结果 |
| status | tinyint | 操作状态 |
| error_msg | text | 错误信息 |
| oper_time | datetime | 操作时间 |

### 6.3 表关系说明
- sys_user 与 sys_role 通过 sys_user_role 关联
- sys_role 与 sys_permission 通过 sys_role_permission 关联
- sys_user 与 sys_dept 为多对一关系
- sys_permission 支持父子级菜单结构

---

## 7. 接口设计

### 7.1 接口规范
- 请求方式：GET/POST/PUT/DELETE
- 数据格式：JSON
- 字符编码：UTF-8
- 返回结果统一封装

### 7.2 统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 7.3 示例接口

#### 7.3.1 登录接口
- URL：`/api/auth/login`
- 方法：POST
- 请求参数：
```json
{
  "username": "admin",
  "password": "123456"
}
```
- 返回示例：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxx.yyy.zzz"
  }
}
```

#### 7.3.2 用户列表接口
- URL：`/api/user/page`
- 方法：GET
- 参数：pageNum、pageSize、username、status

#### 7.3.3 新增用户接口
- URL：`/api/user`
- 方法：POST

#### 7.3.4 修改用户接口
- URL：`/api/user/{id}`
- 方法：PUT

#### 7.3.5 删除用户接口
- URL：`/api/user/{id}`
- 方法：DELETE

---

## 8. 安全设计

### 8.1 认证机制
采用 **JWT + Spring Security** 方案实现认证授权。

### 8.2 权限控制
采用 **RBAC** 模型：
- 用户 -> 角色 -> 权限
- 接口权限控制
- 按钮级权限控制

### 8.3 密码安全
- 使用 BCrypt 加密存储密码
- 禁止明文传输与存储
- 支持密码复杂度校验

### 8.4 接口安全
- 统一 Token 拦截校验
- 参数合法性校验
- 防重放、防暴力破解
- 限制非法访问频率

### 8.5 日志审计
对以下行为进行审计：
- 登录
- 登出
- 新增
- 修改
- 删除
- 导入导出
- 权限变更

---

## 9. 异常处理设计

### 9.1 全局异常处理
使用 `@RestControllerAdvice` 实现统一异常处理。

### 9.2 异常分类
- 参数校验异常
- 业务异常
- 权限异常
- 系统异常
- 数据库异常

### 9.3 异常返回示例
```json
{
  "code": 500,
  "message": "系统内部异常",
  "data": null
}
```

---

## 10. 缓存设计

### 10.1 缓存场景
- 用户登录信息缓存
- 验证码缓存
- 字典数据缓存
- 菜单权限缓存
- 热点业务数据缓存

### 10.2 Redis Key 设计建议
- `login:token:{token}`
- `login:user:{userId}`
- `captcha:{uuid}`
- `dict:{dictType}`
- `permission:user:{userId}`

---

## 11. 日志设计

### 11.1 日志分类
- 系统日志
- 业务日志
- 错误日志
- 审计日志

### 11.2 日志规范
- 使用 Slf4j + Logback
- 不输出敏感信息
- 关键操作必须记录
- 生产环境日志按天滚动

---

## 12. 项目结构设计

```text
project-name
├── src/main/java
│   ├── com.example.system
│   │   ├── controller
│   │   ├── service
│   │   ├── service/impl
│   │   ├── mapper
│   │   ├── entity
│   │   ├── dto
│   │   ├── vo
│   │   ├── config
│   │   ├── security
│   │   ├── common
│   │   ├── exception
│   │   └── utils
├── src/main/resources
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── application-prod.yml
│   ├── mapper
│   └── logback-spring.xml
└── pom.xml
```

---

## 13. 关键开发规范

### 13.1 命名规范
- 类名：大驼峰，如 `UserService`
- 方法名：小驼峰，如 `getUserList`
- 常量名：全大写下划线分隔，如 `DEFAULT_PAGE_SIZE`
- 数据表名：小写下划线，如 `sys_user`

### 13.2 代码规范
- Controller 仅处理请求与响应
- Service 处理业务逻辑
- 避免重复代码，提炼公共组件
- 使用统一响应对象
- 关键方法添加注释

### 13.3 接口规范
- 统一前缀 `/api`
- 使用 RESTful 风格
- 参数校验使用 `@Validated`
- 统一异常码与提示信息

---

## 14. 部署设计

### 14.1 运行环境要求
- JDK 1.8
- Maven 3.6+
- MySQL 5.7+
- Redis 5.x
- Linux CentOS / Ubuntu

### 14.2 打包部署方式
```bash
mvn clean package -DskipTests
java -jar system.jar --spring.profiles.active=prod
```

### 14.3 Nginx 代理示例
```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 15. 测试方案

### 15.1 测试类型
- 单元测试
- 接口测试
- 集成测试
- 性能测试
- 安全测试

### 15.2 测试重点
- 登录鉴权是否有效
- 权限控制是否准确
- 核心 CRUD 是否正常
- 异常处理是否完整
- 并发场景是否稳定

---

## 16. 风险与建议

### 16.1 项目风险
- 权限设计复杂导致后期维护困难
- 接口规范不统一导致前后端联调困难
- 日志与审计不足影响问题排查
- 数据库设计不合理影响性能

### 16.2 建议
- 前期统一规范与技术选型
- 核心表结构评审后再开发
- 建立代码评审机制
- 引入接口文档与测试工具
- 核心模块优先保障安全与可维护性

---

## 17. 后续扩展方向

1. 集成工作流引擎
2. 集成消息通知模块
3. 集成文件中心
4. 集成数据报表模块
5. 集成多租户能力
6. 集成操作留痕与审批流
7. 集成 Docker / CI-CD 自动部署

---

## 18. 结论
本系统基于 Java 8 与 Spring Boot 构建，采用标准分层架构和模块化设计方式，具备良好的可维护性、可扩展性与安全性。通过统一的用户、角色、菜单、权限、日志与配置管理能力，可满足大多数通用后台管理类系统建设需求，并为后续业务扩展提供稳定基础。

---

## 19. 附录

### 19.1 依赖版本选型说明（Java 8）
面向 **Java 8** 的快速搭建后台项目，推荐以 **Spring Boot 2.7.18** 为基础版本。Spring 官方文档明确说明：Spring Boot 2.7.18 需要 Java 8，并且兼容至 Java 21；同时 2.7 支持周期后来得到延长。对于必须坚持 Java 8 的项目，这仍是当前较稳妥的 Spring Boot 主线。  

同时需要注意：Spring Framework 5.3.x 与 Spring Security 5.8.x 的开源支持已在 2024 年 8 月结束，因此该技术栈更适合作为 **现阶段 Java 8 项目的稳定延续方案**；如果未来允许升级，建议尽早规划迁移到 Java 17 + Spring Boot 3.x。  

### 19.2 推荐依赖清单（适合 Java 8 的通用后台脚手架）
以下版本以 **兼容 Java 8、社区使用广、适合 Spring Boot 2.7.x** 为主要原则：

#### 方案一：使用 Spring Boot Parent 统一管理版本（推荐）
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>

<properties>
    <java.version>1.8</java.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <jjwt.version>0.9.1</jjwt.version>
    <hutool.version>5.8.37</hutool.version>
    <knife4j.version>4.5.0</knife4j.version>
    <springdoc.version>1.8.0</springdoc.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <easyexcel.version>3.3.4</easyexcel.version>
    <redisson.version>3.40.2</redisson.version>
</properties>

<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- AOP：操作日志/审计常用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- JDBC / MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL 驱动：8.0.33 是 MySQL Connector/J 8.0 系列最后一个 GA 版本 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：更适合分布式锁、延迟队列等扩展场景 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>${redisson.version}</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt</artifactId>
        <version>${jjwt.version}</version>
    </dependency>

    <!-- OpenAPI 文档：1.8.0 是支持 Spring Boot 2.x 的最新开源版本 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>

    <!-- Knife4j，适合中文项目的接口文档增强 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi2-jakarta-spring-boot-starter</artifactId>
        <version>${knife4j.version}</version>
    </dependency>

    <!-- Bean 映射 -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- 常用工具包 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Excel 导入导出 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>${easyexcel.version}</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 19.3 依赖选型建议
1. **优先使用 Spring Boot Parent 管理 Spring 生态版本**，避免手工指定 Spring Framework、Spring MVC、Spring Security 的版本而产生冲突。
2. **不建议新项目继续使用 Springfox Swagger 2/3**，对于 Spring Boot 2.x 更推荐 `springdoc-openapi 1.8.0`。
3. **MyBatis-Plus 推荐固定到 3.5.7 左右的成熟版本**，兼顾 Java 8、Spring Boot 2.x 和社区实践稳定性。
4. **MySQL 驱动推荐 8.0.33**，它是 8.0 系列最后一个 GA 版本，适合 Java 8 项目保持稳定。
5. **JWT 如果追求少改动与 Java 8 兼容，`jjwt 0.9.1` 仍然是常见选择**；若准备做更细的安全加固，可在后续升级时切到更新拆分版 API。
6. **接口文档建议二选一**：
   - 偏标准 OpenAPI：`springdoc-openapi-ui`
   - 偏中文增强体验：在 springdoc 基础上叠加 Knife4j
7. **如果项目只做单体后台，Redisson 不是必选项**；如果涉及分布式锁、延时任务、限流等，可保留。

### 19.4 推荐补充文档
- 后台脚手架初始化说明
- 模块开发规范文档
- 数据库建表规范
- 接口设计规范
- 权限设计说明书
- 部署手册
- 测试用例文档
- 二次开发指南

