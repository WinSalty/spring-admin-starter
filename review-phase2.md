# 阶段 2：认证鉴权模块 — 代码评审报告

> 评审人：架构师/评审师 Agent
> 评审日期：2026-04-17
> 评审范围：阶段 2 全部新增代码（Entity / Mapper / Service / Controller / Security / JWT / DTO / VO / Exception / Config / Log）

---

## 评审结论：❌ 不通过

阶段 2 的代码骨架完整、架构清晰、15 条任务均已实现，但存在 **1 个严重安全问题**、**3 个功能性缺陷** 和 **若干中等问题**，未达到验收标准。

---

## 一、通过项（满足的验收标准）

| # | 验收标准 | 状态 | 说明 |
|---|---------|------|------|
| 1 | 注册必须经过邮箱验证码 | ✅ | `AuthService.register()` 在创建用户前调用 `emailCodeService.consumeRegisterCode()`，验证码未消费则抛出异常 |
| 2 | 密码 BCrypt 加密存储 | ✅ | `SecurityConfig` 配置 `BCryptPasswordEncoder`；注册时 `passwordEncoder.encode()`；登录时 `passwordEncoder.matches()`；SQL 初始 admin 密码为 BCrypt 密文 `$2a$10$...` |
| 3 | 登录返回 accessToken、refreshToken、expiresIn、用户信息 | ✅ | `LoginResponseVO` 包含 `accessToken`、`refreshToken`、`tokenType="Bearer"`、`expiresIn`、`user(UserInfoVO)` |
| 4 | access token 过期可 refresh；退出后 Token 失效 | ✅ | `TokenService.refresh()` 校验后撤销旧 refresh token、签发新双 Token；`TokenService.logout()` 删除 Redis 会话并标记 DB revoked=1 |
| 5 | 未登录返回 40101；无权限返回 40301 | ✅ | `SecurityConfig` 的 `AuthenticationEntryPoint` 返回 `ErrorCode.UNAUTHORIZED(40101)`，`AccessDeniedHandler` 返回 `ErrorCode.FORBIDDEN(40301)` |
| 6 | 前端可按 Bearer Token 调用受保护接口 | ✅ | `JwtAuthenticationFilter` 解析 `Authorization: Bearer <token>`，写入 `SecurityContextHolder` 和 `LoginUserContext` |
| 7 | PermissionBootstrap 字段与前端对齐 | ✅ | `PermissionMenuVO` / `PermissionActionVO` / `PermissionBootstrapVO` 字段与前端 `PermissionMenu` / `PermissionAction` / `PermissionBootstrap` 100% 一致 |
| — | 验证码 SHA-256 存储 | ✅ | `HashService.sha256()` 计算哈希，`SysEmailVerifyCode.codeHash` 存储 hash，明文不落库 |
| — | JWT Secret 从环境变量读取 | ✅ | `JwtTokenProvider.init()` 优先读 `ADMIN_JWT_SECRET` 环境变量，其次读 `application.yml`，均未配置则警告并随机生成 |
| — | 登录日志写入 | ✅ | `LoginLogService` 在登录成功/失败/账号禁用时均写入 `sys_log_login` |
| — | 登录失败锁定 | ✅ | `LoginAttemptService` 单账号 5 次失败锁定 30 分钟，单 IP 20 次限流 |
| — | 权限 Bootstrap Redis 缓存 | ✅ | `PermissionService.bootstrap()` 使用 `permission:bootstrap:v1:{userId}` 缓存，TTL 30 分钟 |

---

## 二、不通过项（具体问题与修复建议）

### 🔴 严重（阻塞发布）

| # | 问题 | 文件位置 | 影响 | 修复建议 |
|---|------|---------|------|---------|
| S1 | **验证码明文写入日志** | `EmailCodeService.sendRegisterCode()` 第 54 行：`log.info("Simulated email verification code, email={}, scene={}, code={}", ..., code)` | 明文验证码写入日志文件，任何能读取日志的人都能获取有效验证码，**完全绕过验证码安全机制** | 删除日志中的 `code` 参数，改为仅记录 `email` 和 `scene`，例如：`log.info("Email verification code sent, email={}, scene={}", normalizedEmail, SCENE_REGISTER)` |

### 🟠 功能性缺陷

| # | 问题 | 文件位置 | 影响 | 修复建议 |
|---|------|---------|------|---------|
| F1 | **Refresh Token 并发竞争条件** | `TokenService.refresh()` 第 79-90 行：先 `redisTemplate.hasKey()` 检查，再 `refreshTokenMapper.selectOne()` 查询 DB，最后 `record.setRevoked(1)` 标记撤销。两步操作非原子 | 如果旧 refresh token 被并发使用两次，两个请求可能同时通过 Redis 检查和 DB 检查，导致签发两个新 token 对 | 使用 Redis Lua 脚本原子检查+删除 refresh token，或使用分布式锁（如 `SETNX`）保证同一 refresh token 同时只能被一个请求处理 |
| F2 | **Refresh Token 可匿名调用** | `SecurityConfig.configure()` 将 `/api/auth/refresh` 加入白名单 `permitAll()`；同时 `JwtAuthenticationFilter.shouldNotFilter()` 也放行该路径 | refresh 接口无需任何认证即可调用，攻击者可无限刷 token，不受登录失败限制保护 | 保持白名单配置（因为 refresh 使用的是 refresh token 而非 access token），但建议在 `TokenService.refresh()` 中加入 IP 维度限流，防止滥用 |
| F3 | **单元测试几乎为空** | `SpringAdminApplicationTests.java` 仅有 `contextLoads()` 空测试 | 验收标准第 8 条要求「对发送验证码、注册、登录、刷新 Token、登出均有测试覆盖」，当前 **零覆盖** | 补充至少以下测试：(1) 注册流程：验证码校验失败/成功、重复邮箱拒绝、密码强度校验 (2) 登录流程：正确密码返回 token、错误密码返回 40101、账号锁定拒绝 (3) Token 刷新：有效 refresh 返回新 token、过期/已撤销 refresh 返回 40101 (4) 登出：登出后 token 失效 |

### 🟡 中等问题

| # | 问题 | 文件位置 | 影响 | 修复建议 |
|---|------|---------|------|---------|
| M1 | **JWT Secret 未配置时每次重启生成新密钥** | `JwtTokenProvider.init()` 第 34-38 行 | 生产环境未配置 `ADMIN_JWT_SECRET` 时，所有已签发 token 在重启后全部失效，用户被迫重新登录 | 生产环境必须强制配置 `ADMIN_JWT_SECRET`，可在 `application-prod.yml` 中设置必填校验，启动时检查并抛出异常阻止启动 |
| M2 | **登录日志同步写入阻塞主链路** | `AuthService.login()` 中 `loginLogService.record()` 同步调用 | 登录接口响应时间增加一次数据库写入耗时（通常 5-20ms），高并发时影响吞吐量 | 使用 `@Async` 异步写入，或使用 `CompletableFuture.runAsync()` 提交到独立线程池 |
| M3 | **登录接口字段名与前端 mock 不一致** | `LoginResponseVO` 使用 `accessToken`/`refreshToken`/`expiresIn`；前端 `LoginResult` 只有 `{ token: string }` | 前端对接时需要修改 `LoginResult` 类型和 `login()` 调用逻辑 | todolist 已标注此差异（⚠️ 对齐），但应在接入真实后端时优先处理。后端接口设计本身合理 |
| M4 | **refresh 接口同时支持 Header 和 Body 传 token** | `AuthController.refresh()` 第 53-58 行 | 增加 API 不确定性，客户端可能混淆 | 建议仅保留 Header 方式（与 todolist 设计一致），移除 `@RequestBody RefreshTokenRequest` |
| M5 | **ErrorCode.BUSINESS_CONFLICT 缺少细分** | `ErrorCode` 枚举 | 注册时「邮箱已注册」和「用户名已存在」都返回 40901，前端无法区分具体冲突类型 | 建议拆分为 `EMAIL_REGISTERED(40901, "邮箱已注册")` 和 `USERNAME_EXISTS(40902, "用户名已存在")` |

---

## 三、建议改进（非阻塞性优化）

### 3.1 日志脱敏增强
- `GlobalExceptionHandler` 的 `handleException` 中 `log.error("Unhandled system exception", ex)` 可能打印包含密码/token 的完整堆栈
- 建议在 `GlobalExceptionHandler` 中对请求参数进行脱敏后再记录到错误日志

### 3.2 refresh token 存储优化
- `SysRefreshToken` 表存储了 `tokenHash`，但 `TokenService.refresh()` 和 `TokenService.logout()` 都是通过 `tokenId` 查询而非 `tokenHash`
- 建议对 `tokenId` 字段加唯一索引，提升查询效率

### 3.3 验证码过期清理
- `sys_email_verify_code` 表中过期验证码会持续积累，建议增加定时清理任务或使用 Redis TTL 替代 DB 存储

### 3.4 PermissionService 性能
- `TokenService.toUserInfo()` 和 `TokenService.authenticateAccessToken()` 都会调用 `permissionService.listRoleCodes()` 和 `permissionService.listPermissionCodes()`
- `listRoleCodes` / `listPermissionCodes` 内部再调用 `listRoles` / `listMenus`，每次查询都走 DB
- 建议：在 `TokenService.issue()` 中一次性查询角色和权限，避免重复 DB 调用；或者在 PermissionService 中增加本地缓存

### 3.5 Spring Security 配置更新
- `@EnableGlobalMethodSecurity` 在 Spring Security 5.7+ 已标记为 `@Deprecated`，建议升级为 `@EnableMethodSecurity`
- `WebSecurityConfigurerAdapter` 在 Spring Security 5.7+ 已废弃，建议改用 `SecurityFilterChain` Bean 方式

### 3.6 邮件发送模拟
- `EmailCodeService.sendRegisterCode()` 当前是 `log.info` 模拟发送，应预留 `MailSender` 接口，方便后续接入真实邮件服务

### 3.7 默认角色硬编码
- `AuthService.register()` 中默认角色 `"USER"` 硬编码在代码中，建议提取为配置项 `admin.security.default-role-code`

---

## 四、代码质量评价

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 分层清晰（Controller → Service → Mapper），依赖注入合理，职责分离好 |
| 安全设计 | ⭐⭐⭐ | BCrypt、SHA-256、JWT 签名、登录锁定、Token 双签均正确实现；但验证码日志泄露是严重问题 |
| 代码规范 | ⭐⭐⭐⭐ | 命名规范、包结构合理、异常体系完整、统一响应格式正确 |
| 测试覆盖 | ⭐ | 几乎无单元测试，不符合验收标准 |
| 前后端对齐 | ⭐⭐⭐⭐ | PermissionBootstrap 字段 100% 对齐；LoginResponse 需前端配合调整 |

---

## 五、总结

阶段 2 的实现质量整体良好，核心认证鉴权链路（注册→登录→刷新→登出→权限 Bootstrap）架构完整，安全机制（BCrypt、SHA-256、JWT、登录锁定、Token 双签）设计到位。

**阻塞发布的问题**：验证码明文写日志（S1），必须修复。
**建议优先处理**：Refresh Token 并发问题（F1）、补充单元测试（F3）。
**可在阶段 3 一并优化**：日志异步化（M2）、错误码细分（M5）、Spring Security 配置升级（建议 3.5）。
