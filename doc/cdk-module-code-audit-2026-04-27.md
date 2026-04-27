# CDK 模块代码审计报告

- 审计对象：`spring-admin-starter` 项目 `com.winsalty.quickstart.cdk` 模块
- 审计时间：2026-04-27
- 审计人：sunshengxian（Claude Opus 4.7 协助）
- 审计范围：`src/main/java/.../cdk/**`、`src/test/java/.../cdk/**`，并交叉核对积分模块对外兑换入口、SQL 迁移脚本、`ErrorCode` 与 `CLAUDE.md` 工程规范。

---

## 一、模块结构概览

```
cdk
├── config/    CdkProperties               app.cdk.* 配置 + pepper
├── constant/  CdkConstants                状态机、Redis key、格式参数
├── controller/AdminCdkController          /api/admin/cdk/** 管理端 API
├── dto/       CdkBatchCreateRequest 等    入参 DTO
├── entity/    CdkBatchEntity 等           表实体
├── mapper/    CdkBatchMapper 等           MyBatis 注解 SQL
├── service/   CdkService                  服务接口
│   └── impl/  CdkServiceImpl              核心业务实现（≈1080 行）
└── vo/        CdkBatchVo 等               展示对象
```

兑换入口分布：

| 端 | 路径 | 鉴权 | 来源 |
| --- | --- | --- | --- |
| 管理端 | `GET/POST /api/admin/cdk/batches`、`POST /batches/{id}/void`、`GET /codes`、`POST /codes/{id}/status`、`GET /redeem-records` | `@PreAuthorize("hasRole('ADMIN')")` | `AdminCdkController` |
| 用户端 | `POST /points/cdk/redeem` | 登录用户 | `points/PointsController#redeem` |

依赖外部模块：`auth`（`@AuditLog`、`AuthUser`）、`points`（`PointRechargeOrder`、`BenefitGrantService`）、`infra`（`RedisCacheService`、`TransactionOutboxService`、`TraceIdFilter`、`FastJsonUtils`）、`risk`（`RiskAlertService`）。

数据表：`cdk_batch`、`cdk_code`、`cdk_redeem_record`、`cdk_export_audit`（`V22`/`V26`/`V27`/`V28`/`V29` 迁移）。

---

## 二、整体结论

CDK 模块的核心兑换链路 **基本达到生产可用质量**：HMAC + AES-GCM 双层防护、幂等键、`SELECT ... FOR UPDATE` 与条件原子更新、用户/IP 双维限流、连续失败锁定与风控告警、Outbox 事件、审计日志、入参校验都已具备。集成测试覆盖了同幂等键、并发抢兑、余额不足三种关键场景且会校验对账差异不漂移。

但仍存在 **若干安全强度不足、流程闭环缺口与性能/可维护性问题**，建议在投产前完成「高」优先级修复，并将「中」优先级纳入下一迭代。

| 类别 | 高 | 中 | 低 |
| --- | --- | --- | --- |
| 数量 | 4 | 8 | 9 |

---

## 三、安全风险（按优先级）

### H1【高】CDK 随机熵偏低且校验位强度极弱
- 位置：`CdkConstants.RANDOM_BYTE_LENGTH = 8`、`CHECKSUM_LENGTH = 1`，`CdkServiceImpl#generatePlainCode`、`#calculateChecksum`、`#isCodeFormatValid`。
- 现状：码体随机部分仅 64 bit；末尾 1 个十六进制校验位（仅 16 种取值）用于查库前的格式过滤。
- 风险：
  - 大批次场景下 64 bit 抵御在线/离线枚举的安全裕度不足；若同一 pepper 长期不轮换，HMAC 反查表攻击成本可计算。
  - 攻击者只需平均 ≈16 次随机请求即可让一条无效 CDK 通过格式校验进入数据库 `findByCodeHash` 查询，相当于把 IP/用户限流的有效阈值除以 16，实际上把限流压力转嫁到了 DB。
- 建议：
  - 将 `RANDOM_BYTE_LENGTH` 提升到 `≥ 16`（128 bit），同步调整 `RANDOM_HEX_LENGTH`、`CODE_PART_COUNT`（已是派生常量，自动联动）。
  - 把 `CHECKSUM_LENGTH` 提升到 `≥ 4` 个十六进制字符，或改为带前缀 base32（Crockford）形式提升人工录入容错。
  - 旧批次需通过版本字段（建议在 `cdk_code` 增加 `code_version`）兼容兑换。

### H2【高】HMAC pepper 与 AES 密钥共用同一来源
- 位置：`CdkServiceImpl#hmacSha256` 用 `cdkProperties.getPepper()` 作 HMAC key；`#codeSecretKey` 直接 `SHA-256(pepper + ":cdk-code:v1")` 作为 AES 密钥。
- 风险：
  - 违反「不同用途使用不同密钥」原则，pepper 一旦泄露既能反推 hash 也能解密明文。
  - 没有 key version：密文体只携带 IV（12B）+ ciphertext，无 keyId。一旦换 pepper，所有历史 `encrypted_code` 不可解。
- 建议：
  - 引入两个独立的 secret：`hmac_pepper`、`code_encryption_key`，分别由配置中心 / KMS 注入；HMAC 使用 raw 256 bit 随机字节（base64 注入），AES 使用 HKDF 派生。
  - 在密文中加入 1 字节 keyId 头部，并在 `cdk_code` 增加 `key_id` 列以支持密钥轮换。
  - 派生改为 HKDF-SHA256（`javax.crypto.spec.SecretKeySpec` 配合 BouncyCastle 的 HKDF）。

### H3【高】管理端列表全量解密返回，缺少导出/查看审计
- 位置：`CdkServiceImpl#listCodes` → `toCodeVo` → `decryptPlainCode`，每页最多 100 条明文经 `CdkCodeVo.cdk` 字段直出到前端。
- 现状：
  - 已有 `cdk_export_audit` 表与 `CdkBatchMapper#incrementExport`，但 service / controller **从未调用**，导出能力与审计完全缺失。
  - 列表查询和明文解密未触发任何审计日志记录（仅依赖 `@AuditLog` 注解，列表接口未注解）。
- 风险：内部人员可通过分页接口批量抓取明文码，无法事后追溯。
- 建议：
  - 列表接口默认 **不返回明文**（mask 为 `****-****-****-XXXX` 等），仅通过专用「在线查看/导出」接口按 `codeId` 单条解密。
  - 该接口必须：① `@AuditLog` 标注；② 写入 `cdk_export_audit`，并 `incrementExport`；③ 校验批次 `riskLevel`，高/严重等级要求二次确认或附加审批人。
  - 列表 `pageSize` 上限从 100 收紧到 50；并发同一管理员 N 分钟内查看明文次数加限流。

### H4【高】高价值批次缺少审批闭环，二次复核字段长期闲置
- 位置：`CdkBatchEntity#approvedBy/approvedAt/secondApprovedBy/secondApprovedAt`、`CdkServiceImpl#createBatch`、`#voidBatch`。
- 现状：批次创建即写入 `BATCH_STATUS_ACTIVE` 并立即生成可兑换码；作废只需 `id` 一个路径参数。两个 `*ApprovedBy` 字段在写入路径上从未被赋值，仅在 VO 中回显。
- 风险：单管理员凭账号即可生成大额积分批次或作废批次（资金 / 名誉损失）。
- 建议：
  - 引入 `BATCH_STATUS_PENDING_APPROVAL` 状态：`createBatch` 仅落库为 pending，单独 `approve` / `secondApprove` 接口推进至 `active` 后再触发码生成。
  - `voidBatch` 增加必填 `reason`、记录到 `redeem_record` 或专门的批次操作日志表。
  - 对积分总额 ≥ 阈值（例如 50000 积分 × 总数）的批次强制双人复核。

---

### M1【中】码生成长事务 + 单条 INSERT，性能与锁占用问题
- 位置：`CdkServiceImpl#createBatch`（`@Transactional`）→ `#generateCodes` 循环逐条 `cdkCodeMapper.insert`，最多 `maxBatchSize=10000`。
- 风险：
  - 整个生成过程持有 `cdk_batch` 行写锁与活跃事务，10000 条 SQL 单事务在 RDS 上易触发慢事务告警，且失败后整体回滚浪费 IO。
  - 期间申请的连接长期占用，影响其他兑换并发。
- 建议：
  - 拆分为「批次落库（短事务）→ 异步批量生成（每批 500 条独立事务）→ 状态置 `active`」三段式；用 `generated_count == total_count` 作为完成标志。
  - `CdkCodeMapper.insert` 增加 `insertBatch(List<CdkCodeEntity>)` 使用 `<foreach>` 单条 SQL 插入。

### M2【中】客户端 IP 信任链未明确
- 位置：`CdkServiceImpl#redeem` → `IpUtils.getClientIp(servletRequest)`，结果同时用于 `client_ip` 入库、IP 限流摘要 key。
- 风险：若 `IpUtils` 信任 `X-Forwarded-For` / `X-Real-IP` 而网关层未做白名单/覆盖，则攻击者可伪造 IP 绕过 IP 维度限流。
- 建议：
  - 审计 `IpUtils` 实现，确认仅在受信代理白名单内才采用 XFF；否则改为 `request.getRemoteAddr()`。
  - 文档化部署假设：必须由可信网关注入 `X-Forwarded-For`，应用侧只读取最右一跳。

### M3【中】risk_level 字段在业务流程中无作用
- 现状：`CdkBatchCreateRequest.riskLevel` → `CdkBatchEntity.riskLevel` → `CdkBatchVo`，无消费方。
- 风险：业务上声明的「高风险」并不会带来实际限流、审批或脱敏增强，是 **safety theater**。
- 建议：与 H3、H4 联动；至少令 `riskLevel = high/critical` 触发：① 必须双人复核；② 列表查看明文需 `@PreAuthorize` 升级到 `ADMIN_RISK`；③ 兑换链路按批次维度收紧限流。

### M4【中】同步 Redis 限流计数 + expire 非原子
- 位置：`CdkServiceImpl#checkLimit`、`#recordRedeemFailure`：`redisCacheService.increment` 后 `if (current == 1) expire(...)`。
- 风险：极端时序下「INCR 完成但 EXPIRE 未及」窗口短暂存在；进程崩溃可能导致 key 永不过期（取决于 `RedisCacheService` 实现）。
- 建议：使用 Lua 脚本 `INCR + EXPIRE`，或改为 `set(key, 0, ttl, NX)` + `incr` 两段。

### M5【中】valid_from / valid_to 日期解析错误未友好化
- 位置：`CdkServiceImpl#validateRedeemable`：`LocalDateTime.parse(batch.getValidFrom(), DATE_TIME_FORMATTER)`。
- 现状：`CdkBatchCreateRequest` 仅 `@NotBlank`，未限制日期格式，也未校验 `validFrom < validTo`。若 DBA 人工修改或脏数据写入，兑换时抛 `DateTimeParseException`，未被 `BusinessException` 包装，最终 500。
- 建议：
  - DTO 层增加 `@Pattern` 校验 ISO 格式或改为 `LocalDateTime` 字段。
  - `createBatch` 校验 `validFrom < validTo` 且 `validTo > now`。
  - `validateRedeemable` 加 `try/catch` 包装为 `CDK_CODE_UNAVAILABLE`，写错误日志含 `batchId`。

### M6【中】benefitConfig 反序列化弱校验，依赖 fastjson2
- 位置：`CdkServiceImpl#buildRechargeOrder`：`JSON.parseObject(batch.getBenefitConfig()).getLongValue(CONFIG_POINTS)`。
- 风险：
  - `benefitConfig` 是受信内部数据，问题不大；但若 DB 数据损坏或字段缺失，`getLongValue` 返回 0，会导致积分订单金额为 0 静默写入 + 权益发放成功，**资损静默**。
  - 项目使用 fastjson2，需关注 `autoType` 默认开关与最新 CVE 修复（建议升级到 `2.0.51+`）。
- 建议：
  - 反序列化为强类型 `BenefitPointsConfig`，校验 `points > 0`，否则抛 `CDK_BENEFIT_UNSUPPORTED`。
  - 全局统一禁用 fastjson2 autoType（在 `FastJsonUtils` 侧确认）。

### M7【中】码状态变更缺少有效期 / 数量护栏
- 位置：`CdkServiceImpl#updateCodeStatus`。
- 现状：管理员可以把 `disabled` 重新切回 `active`（前提批次 active），但没有限制重新启用次数 / 时间窗。
- 建议：记录每次状态变更到操作日志，单码状态变更次数纳入告警阈值。

### M8【中】审计日志注解未覆盖关键查询接口
- 位置：`AdminCdkController` 中 `batches`、`codes`、`redeemRecords` 三个 GET 没有 `@AuditLog`，而这些接口可获取大量明文/隐私数据。
- 建议：所有读取 CDK 明文 / 用户兑换记录的接口都加 `@AuditLog(logType="query", ...)`，至少记录操作人、批次范围、分页参数。

---

### L1【低】流水号唯一性兜底
- `CdkServiceImpl#createNo` 使用 `prefix + 时间戳 + UUID 前 12 位`，碰撞概率极低，但建议在 `cdk_batch.batch_no`、`cdk_redeem_record.redeem_no`、`point_recharge_order.recharge_no` 上加 `UNIQUE` 索引（迁移脚本中确认）。

### L2【低】`CdkCodeStatusRequest.status` 缺少枚举级校验
- 仅 `@NotBlank`，service 层手工判断；建议加 `@Pattern(regexp="active|disabled")` 提前在 web 层拒绝。

### L3【低】`CdkBatchListRequest`/`CdkRedeemRecordListRequest` 无分页参数 `@Min/@Max` 校验
- 当前依赖 `pageNo()` / `pageSize()` 兜底；建议 DTO 上加 `@Min(1) @Max(100)` 让 4xx 错误前置。

### L4【低】`recordRedeemFailure` 在「找到码但批次缺失」时不计失败
- 位置：`#redeem` 中 `code != null && batch == null` 仅打 error 日志，最终在 `validateRedeemable` 抛 `CDK_CODE_UNAVAILABLE`，但未走 `recordRedeemFailure`，理论上可被攻击者用作枚举判断（响应特征区分）。
- 建议：所有失败分支统一调用 `recordRedeemFailure`。

### L5【低】Outbox payload 缺少 `idempotencyKey`
- `#buildRedeemOutboxPayload` 已包含 `redeemNo`、`userId`、`batchNo`，下游消费者去重已可基于 `redeemNo`，但加上 `idempotencyKey` 便于排查。

### L6【低】`CdkProperties.pepper` 默认空字符串
- 启动期不会 fail-fast：`ensurePepperConfigured` 仅在 `createBatch`/`redeem` 时校验。建议改为 `@PostConstruct` 启动时校验，避免线上"应用启起来但所有 CDK 操作都报错"。

### L7【低】MDC traceId 未在异步 Outbox 消费链路传递
- 当前同步事务内尚未异步调用，未来若 Outbox 投递切到独立线程池，需 `TaskDecorator` 透传 MDC。

### L8【低】列表 LIKE 查询使用 `LOWER()` 包裹列
- `CdkBatchMapper#findPage`、`CdkCodeMapper#findPage` 中 `LOWER(batch_no) LIKE '%xxx%'` 阻断索引，最大 10000 批次/批次 10000 码场景压力可控，但建议改为：① 入库统一小写副本列；② 或弃用左 `%` 模糊匹配。

### L9【低】`CdkRedeemRecordVo.userId` 用 String 暴露
- 已脱字段 friendly，但管理员列表大量明文用户 ID + IP 对外，需配合 H3 整体脱敏策略。

---

## 四、流程闭环差距

| 流程 | 当前实现 | 缺口 |
| --- | --- | --- |
| 批次审批 | 字段就绪，逻辑缺失 | H4：缺 `pending → active` 审批接口 |
| 批次作废 | 实现 | H4：缺审批理由、双人复核 |
| 批次导出/打印 | DB 字段就绪，业务接口缺失 | H3：缺 `export` 接口与审计；缺单条「在线查看明文」接口 |
| 异常补偿 | `RECORD_STATUS_FAILED` 常量预留但未使用 | 兑换路径所有失败均走 `BusinessException` 回滚，未落 `failed` 兑换记录；下游对账难以基于失败记录排查 |
| 风险等级联动 | 字段就绪，无消费 | M3 |
| 限流降级 | 静态阈值 | 缺批次/活动维度的「热点保护」（同一批次秒级 QPS） |
| 监控指标 | 仅日志 | 建议补 Micrometer：兑换 QPS / 失败率 / 锁定次数 / 批次生成耗时 |

---

## 五、测试覆盖评估

- **集成测试**（`CdkPointsDevIntegrationTest`，`@EnabledIfEnvironmentVariable`）：
  - 同幂等键重放：✅
  - 同 CDK 并发抢兑只允许一次成功：✅
  - 积分余额不足扣减：✅（积分模块）
  - 测试通过 `app.cdk.redeem-user-limit=100000` 等关闭限流，能保证基础正确性，但 **未覆盖限流/锁定路径**。
- **单元测试**：`src/test/java/.../cdk/service/impl/CdkServiceImplTest.java` 存在但未列入本次审计阅读清单（建议补审）。
- **建议补充**：
  - `isCodeFormatValid` 的边界（缺校验位、错误分组、含空格小写、含非法字符）。
  - `hmacSha256` 一致性（同一明文相同摘要、不同 pepper 不同摘要）。
  - `ensurePepperConfigured` pepper < 32 字节抛错。
  - 批次过期 / 批次 voided / 码 disabled 兑换均拒绝。
  - 失败次数累积到阈值触发 `CDK_REDEEM_LOCKED` 并写 `riskAlertService`。
  - `voidBatch` 同步把所有未兑换码置 `disabled`，已兑换码不变。

---

## 六、规范一致性检查（对照 `CLAUDE.md`）

| 规范项 | 状态 |
| --- | --- |
| 类/方法注释含基本功能、创建日期、`author=sunshengxian` | ✅ 已覆盖 |
| 使用 `info` / `error` 日志级别 | ✅ 仅见这两个级别 |
| 关键流程进度日志 | ✅ `cdk code generation progress` 每 1000 条 |
| 不在主体写 demo / 测试代码 | ✅ |
| 仅使用官方依赖 | ⚠️ 使用 `com.alibaba.fastjson2` 与 `lombok`，均为主流官方包；需确认 `fastjson2` 已锁定到无 CVE 版本（M6） |
| SQL 集中在 `resources/` | ✅ `resources/sql/V22~V29` |
| 测试代码在 `src/test/` | ✅ |

---

## 七、修复建议优先级清单

**Sprint-0（投产前必做）**
1. H1 提升随机熵到 16 字节、校验位到 ≥ 4 hex（含旧批次兼容字段）。
2. H2 拆分 HMAC pepper 与加密 key，引入 keyId 与密钥轮换。
3. H3 列表默认脱敏 + 单条解密接口 + 写 `cdk_export_audit`。
4. H4 增加批次审批与作废理由审计。

**Sprint-1**
5. M1 异步生成 + 批量 INSERT。
6. M2 IP 信任链确认与文档化。
7. M3 risk_level 业务联动。
8. M5 / M6 入参强校验与强类型 `benefitConfig`。
9. 列表/查询接口补 `@AuditLog`（M8）。

**Sprint-2**
10. 失败记录落 `RECORD_STATUS_FAILED`、Micrometer 指标、Lua 限流脚本（M4）。
11. 启动期 fail-fast pepper（L6）、所有 `_no` UNIQUE 索引兜底（L1）、DTO 校验前置（L2/L3）。
12. 单元测试补强。

---

## 八、附：关键代码位置索引

| 关注点 | 文件:行 |
| --- | --- |
| 控制器入口 | `cdk/controller/AdminCdkController.java:34` |
| 用户兑换入口 | `points/controller/PointsController.java:75` |
| 兑换主流程 | `cdk/service/impl/CdkServiceImpl.java:234` |
| 风控限流 | `cdk/service/impl/CdkServiceImpl.java:554` |
| 失败计数与锁定 | `cdk/service/impl/CdkServiceImpl.java:590` |
| 码生成 | `cdk/service/impl/CdkServiceImpl.java:416` |
| HMAC | `cdk/service/impl/CdkServiceImpl.java:740` |
| AES-GCM 加解密 | `cdk/service/impl/CdkServiceImpl.java:757` `:779` |
| 密钥派生 | `cdk/service/impl/CdkServiceImpl.java:807` |
| 格式校验 | `cdk/service/impl/CdkServiceImpl.java:653` |
| 配置项 | `cdk/config/CdkProperties.java:13` |
| 状态机常量 | `cdk/constant/CdkConstants.java:9` |
| 错误码 | `common/constant/ErrorCode.java:79-98` |
| 集成测试 | `src/test/.../cdk/CdkPointsDevIntegrationTest.java` |

---

> 本报告供模块负责人评审。所有修复建议落地后请补充回归测试，并在 `resources/doc/cdk-points-module-development-plan.md` 中追加变更说明。
