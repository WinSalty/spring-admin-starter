# Spring Admin Starter — 代码审核 TODO

> 审核日期：2026-04-18  
> 优先级：🔴 紧急 / 🟠 高 / 🟡 中 / 🟢 低

---

## 🔴 紧急（上线前必须修复）

### 1. 硬编码凭证泄露
- [ ] `application-dev.yml` — 数据库密码 `SpringAdmin@2026` 明文写死
- [ ] `application-prod.yml` / `application-test.yml` — 同上，JWT secret 明文写死
- [ ] 改用环境变量占位符：`${DB_PASSWORD}`、`${JWT_SECRET}`
- [ ] 将敏感配置文件加入 `.gitignore`，或迁移到 Spring Cloud Config / Vault

### 2. 代码中硬编码 IP 地址
- [ ] `AuthServiceImpl.java:158` — `request.setIpAddress("127.0.0.1")` 应从 `HttpServletRequest` 提取真实客户端 IP
- [ ] `GlobalExceptionHandler.java:87` — 同上
- [ ] 提取公共工具方法 `IpUtils.getClientIp(HttpServletRequest)`，处理 `X-Forwarded-For` 代理头

### 3. 配置文件中硬编码 localhost
- [ ] `application-dev/prod/test.yml` — Redis host `127.0.0.1`、MySQL URL `127.0.0.1:3306` 均应外部化
- [ ] 改为 `${REDIS_HOST:localhost}`、`${DB_HOST:localhost}` 形式，支持容器/K8s 部署

### 4. 文件上传安全漏洞
- [ ] `FileRecordServiceImpl.java:49-60` — 仅校验扩展名，未校验 MIME type 和文件魔数（magic bytes）
- [ ] 补充 MIME type 双重校验，防止伪造扩展名绕过
- [ ] 确认 `StringUtils.cleanPath()` 已足够防止路径穿越，必要时加白名单目录校验

### 5. CSRF 保护全局关闭
- [ ] `SecurityConfig.java:33` — `http.csrf().disable()` 对所有端点生效
- [ ] 对无状态 JWT API 可保持关闭，但需在注释中明确说明原因，避免误解

---

## 🟠 高优先级

### 6. 缺少日志滚动配置
- [ ] 所有 `application*.yml` 只配置了 `logging.level`，无文件输出、无滚动策略
- [ ] 新增 `src/main/resources/logback-spring.xml`，包含：
  - 按日期+大小滚动（`SizeAndTimeBasedRollingPolicy`）
  - 保留天数（如 30 天）
  - 分离 ERROR 日志到独立文件
  - 生产环境使用 JSON 格式（Logstash encoder）
  - 异步 Appender（`AsyncAppender`）减少 I/O 阻塞

### 7. 魔法值 / 死数据
- [ ] `FileRecordServiceImpl.java:33` — `10L * 1024L * 1024L` 应提取为常量 `MAX_FILE_SIZE_BYTES`
- [ ] `AuthServiceImpl.java:127` — 硬编码部门 ID `2L`，应从配置或数据库读取
- [ ] 错误码（4001、4004、4005 等）散落各处，应统一到枚举或常量类 `ErrorCode`
- [ ] 分页默认值（pageNo=1、pageSize=10）应提取为常量

### 8. 分页参数缺少校验
- [ ] `FileRecordServiceImpl.java:87-94` — pageNo 可为负数，pageSize 可为 0 或超大值
- [ ] 补充：`pageNo < 1` 重置为 1；`pageSize` 限制在 `[1, 100]` 范围内

### 9. JWT Secret 强度校验缺失
- [ ] `JwtTokenProvider.java:26-39` — 未校验 secret 长度，当前配置的 secret 强度不足
- [ ] 启动时校验 secret 长度 ≥ 32 字节（256 bit），不满足则拒绝启动
- [ ] 无密钥轮换机制，后续考虑支持多版本 key

### 10. 数据库连接池未配置
- [ ] 所有环境配置均未设置 HikariCP 参数，使用默认值可能在高并发下不足
- [ ] 补充：`maximum-pool-size`、`minimum-idle`、`connection-timeout`、`max-lifetime`

### 11. Redis 连接池未配置
- [ ] 未设置 Redis 连接池和超时参数
- [ ] 补充：`lettuce.pool` 或 `jedis.pool` 配置，以及 `timeout`

---

## 🟡 中优先级

### 12. 缺少请求链路追踪（Correlation ID）
- [ ] 无统一的 Request ID / Trace ID 注入机制
- [ ] 实现 `Filter` 或 `HandlerInterceptor`，在请求入口生成 UUID 写入 MDC，响应头返回
- [ ] 所有日志自动携带 `traceId`，便于排查问题

### 13. 缺少接口限流
- [ ] 登录接口无防暴力破解限制
- [ ] 文件上传无频率限制
- [ ] 建议基于 Redis + Lua 实现令牌桶限流，或引入 Resilience4j RateLimiter

### 14. Actuator 暴露端点不足
- [ ] `application.yml` 仅暴露 `health,info`
- [ ] 补充 `metrics`、`prometheus`（如有监控需求）
- [ ] `health` 端点补充 `show-details: when-authorized`，避免未授权泄露内部状态

### 15. 健康检查不完整
- [ ] `HealthController.java` 只有 ping 接口
- [ ] 实现自定义 `HealthIndicator`，检查数据库、Redis 连通性

### 16. 事务管理不完整
- [ ] 部分 Service 方法缺少 `@Transactional` 注解
- [ ] 查询方法应加 `@Transactional(readOnly = true)` 优化性能
- [ ] 写操作应加 `@Transactional(rollbackFor = Exception.class)`

### 17. 错误处理不一致
- [ ] 部分方法返回 `null`，部分抛异常，风格不统一
- [ ] 统一使用 `Optional` 或自定义 `BusinessException`，避免 NPE 风险

### 18. API 文档不完整
- [ ] `OpenApiConfig.java` 已配置 Swagger，但各 Controller 缺少 `@Operation`、`@Parameter`、`@ApiResponse` 注解
- [ ] 补充所有接口的入参、出参、错误码说明

### 19. 生产环境配置缺失
- [ ] `application-prod.yml` 缺少：
  - HTTPS / SSL 配置
  - 响应压缩（`server.compression.enabled`）
  - 安全响应头（Content-Security-Policy 等，可通过 Spring Security 配置）

---

## 🟢 低优先级

### 20. 缺少单元测试 / 集成测试
- [ ] `src/test` 目录无任何测试文件
- [ ] 至少补充核心 Service 的单元测试（JUnit 5 + Mockito）
- [ ] 关键接口补充集成测试（Spring Boot Test + TestContainers）

### 21. 依赖管理不规范
- [ ] `pom.xml` 无 `<dependencyManagement>` 统一管理版本
- [ ] 建议引入 OWASP Dependency-Check 插件，定期扫描已知 CVE

### 22. 异步处理缺失
- [ ] 文件操作、通知发送等耗时操作均为同步调用
- [ ] 考虑 `@Async` + 线程池，或引入消息队列（如 RabbitMQ）解耦

### 23. 代码整洁度
- [ ] 检查并清理各文件中的未使用 import
- [ ] 清理遗留的 TODO / FIXME 注释，转为 Issue 跟踪

---

## 参考文件位置

| 文件 | 问题 |
|------|------|
| `src/main/resources/application-dev.yml` | 硬编码密码、JWT secret、localhost |
| `src/main/resources/application-prod.yml` | 同上 |
| `src/main/resources/application-test.yml` | 同上 |
| `src/main/java/.../auth/AuthServiceImpl.java:127,158` | 硬编码部门ID、IP地址 |
| `src/main/java/.../common/GlobalExceptionHandler.java:87` | 硬编码IP地址 |
| `src/main/java/.../file/FileRecordServiceImpl.java:33,49-60,87-94` | 魔法值、文件校验、分页校验 |
| `src/main/java/.../infra/security/SecurityConfig.java:33` | CSRF全局关闭 |
| `src/main/java/.../infra/security/JwtTokenProvider.java:26-39` | JWT secret强度 |
| `src/main/resources/application.yml` | Actuator配置不足 |
