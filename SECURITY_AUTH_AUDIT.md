# 登录鉴权安全审计报告

## 基本信息

- 审计对象：`react-admin-starter`、`spring-admin-starter`
- 审计范围：登录、注册、JWT 签发与刷新、会话失效、路由守卫、接口鉴权、CORS、生产配置
- 审计日期：2026-04-23
- author：sunshengxian

## 审计结论

当前前后端已经具备基本安全闭环：后端使用 Spring Security + JWT Bearer 鉴权，access token 与 refresh token 区分类型，refresh token 通过 Redis 会话二次校验并轮换；前端具备路由守卫、权限 bootstrap 和 401 自动刷新逻辑。

本次审计发现并修复以下主要风险：

1. 高风险：`prod` profile 存在仓库默认 JWT 密钥兜底，生产误配置时可能使用可预测签名密钥。
2. 高风险：登录和注册验证码匿名入口缺少服务端限流，存在撞库、密码爆破和邮件轰炸风险。
3. 中风险：`/api/common/demo` 被公开放行，生产环境不应保留示例接口匿名访问。
4. 中风险：前端认证 token 长期存放在 `localStorage`，一旦出现 XSS，令牌可被长期复用。
5. 中风险：前端多个接口同时收到 401 时会并发刷新 refresh token，后端已启用 refresh token 轮换，可能导致后续刷新请求误判 session 失效。

## 已实施修复

### 后端

- `JwtTokenProvider` 增加生产密钥强校验：`prod` 环境必须注入独立 `JWT_SECRET`，默认或占位密钥会导致启动失败。
- `application-prod.yml` 移除默认 JWT 密钥兜底，避免生产误用仓库内置密钥。
- 新增 `AuthRateLimitService`，对 `/api/auth/login` 按 IP、账号+IP 双维度限流。
- 新增 `AuthRateLimitService`，对 `/api/auth/register/verify-code` 按 IP、邮箱双维度限流。
- `RedisCacheService` 增加 `expire` 能力，支持限流计数器首次创建后绑定固定窗口。
- `SecurityConfig` 移除 `/api/common/demo` 匿名放行。
- 新增 `AuthRateLimitServiceImplTest` 覆盖限流窗口和超限拒绝逻辑。

### 前端

- `storage.ts` 将认证敏感数据从 `localStorage` 调整为 `sessionStorage`，并清理历史 `localStorage` 残留。
- `request.ts` 增加 refresh token 串行化，多个 401 请求共享同一次刷新结果，避免 refresh token 轮换下的并发失效。

## 剩余风险与后续计划

1. 高优先级：生产部署前必须修改或禁用初始化 SQL 中的默认账号 `admin / 123456`、`viewer / 123456`。
2. 高优先级：建议把 refresh token 改为 `HttpOnly + Secure + SameSite` Cookie，并配套 CSRF 防护；当前前端仍需在 JS 中读取 refresh token，XSS 场景无法彻底防御。
3. 中优先级：Swagger/OpenAPI 建议在生产环境关闭或仅内网可访问，当前仍由安全配置匿名放行。
4. 中优先级：建议在网关或 Nginx 再增加 IP 级全局限流，应用内限流作为第二道防线。
5. 中优先级：建议引入账号锁定、登录失败告警和异常地理位置提醒，提升撞库检测能力。
6. 中优先级：建议将 `X-Forwarded-For` 解析限定为可信代理场景，避免直连请求伪造客户端 IP。
7. 低优先级：前端可增加主动登出接口调用，让服务端 session 立即失效，而不是仅清理本地状态。

## 验证建议

1. 后端执行 `mvn test`，确认限流与既有会话测试通过。
2. 前端执行 `npm run typecheck` 与 `npm run build`，确认请求拦截和存储调整无类型或构建错误。
3. 使用同一账号连续错误登录超过 10 次，确认返回 `操作过于频繁，请稍后再试`。
4. 使用同一邮箱连续发送验证码超过 5 次，确认触发限流。
5. 在生产 profile 不设置 `JWT_SECRET` 或设置默认密钥，确认应用启动失败。
6. 同时触发多个过期 access token 请求，确认前端只发起一次 refresh-token 请求。
