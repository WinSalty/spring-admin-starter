# CDK 与积分模块开发计划

## 1. 背景与目标

创建日期：2026-04-24  
author：sunshengxian

本计划用于在 `spring-admin-starter` 与配套 `react-admin-starter` 中新增 CDK 通用模块和积分钱包模块。CDK 可兑换不同权益或积分，管理员可批量生成 CDK；积分模块用于后续支持用户使用积分兑换权限、服务或其他业务资源。

## 1.1 当前实施进度

更新时间：2026-04-24

author：sunshengxian

当前已完成阶段一基础能力开发，后续开发应基于以下状态继续推进。

### 已完成内容

| 类型 | 完成情况 |
| --- | --- |
| 后端提交 | `2bcba88 实现CDK与积分阶段一模块 gpt-5.4`、`958062a 补充CDK环境变量配置映射 gpt-5.4` |
| 前端提交 | `d78a6ea 接入CDK与积分钱包管理页面 gpt-5.4` |
| 数据库脚本 | 已新增 `V21__init_points_schema.sql`、`V22__init_cdk_schema.sql`、`V23__seed_points_cdk_permissions.sql` |
| 后端积分模块 | 已新增 `com.winsalty.quickstart.points`，支持账户初始化、充值、扣减、冻结、确认冻结、取消冻结、退款、账本哈希链、管理端账户/流水查询、人工调整申请与审批、对账汇总 |
| 后端 CDK 模块 | 已新增 `com.winsalty.quickstart.cdk`，支持批次创建、提交审批、审批生成、暂停、作废、一次性导出、兑换记录查询和用户兑换 |
| 后端权益抽象 | 已新增 `com.winsalty.quickstart.benefit`，首期实现积分权益发放，后续权限或服务包可扩展独立发放器 |
| 后端对账任务 | 已新增 `PointReconciliationJob`，通过 Quartz 按 `app.points.reconciliation-cron` 定时执行积分账户和流水汇总对账 |
| 后端集成测试 | 已新增 `CdkPointsDevIntegrationTest`，通过 `RUN_DEV_INTEGRATION_TESTS=true` 显式连接本地 MySQL/Redis 验证兑换链路 |
| 安全与审计 | CDK 仅存 HMAC Hash；明文只进入短期 Redis 导出窗口；兑换接口接入用户/IP/连续失败限流；管理操作和兑换操作接入 `@AuditLog` |
| 前端钱包 | 已新增 `/points/wallet`，展示余额、CDK 兑换、流水、充值、消费、冻结记录；工作台已按钱包余额卡片重构，仅保留系统公告、钱包余额和预留图表位 |
| 前端管理页 | 已新增 `/system/cdk/batches`、`/system/cdk/redeem-records`、`/system/points/audit` |
| 权限菜单 | 已新增 `points_wallet`、`points_admin_account`、`points_admin_ledger`、`cdk_batch`、`cdk_redeem_record` 路由码和对应按钮权限；`points_wallet` 已调整到个人中心子菜单 |
| 权限协议 | 权限 bootstrap 菜单已透传 `routeCode`，前端按 `routeCode` 或路径映射过滤菜单，避免路径末段与路由码不一致时隐藏入口 |

### 已验证内容

| 项目 | 结果 |
| --- | --- |
| 后端编译 | `mvn -q -DskipTests compile` 通过 |
| 后端测试 | `mvn -q test` 通过 |
| 后端开发环境集成测试 | `RUN_DEV_INTEGRATION_TESTS=true mvn -q -Dtest=CdkPointsDevIntegrationTest test` 通过，已覆盖同一 CDK 并发兑换、同一幂等键重复兑换、余额不足扣减和对账差异稳定性 |
| 前端类型检查 | `npm run typecheck` 通过 |
| 前端单元测试 | `npm run test:unit` 通过；当前无测试文件 |
| 前端生产构建 | `npm run build` 通过 |
| 前端页面联调 | 已用 `admin / 123456` 登录本地前端，验证工作台钱包余额卡、系统公告、个人中心 > 积分钱包入口和 `/points/wallet` 路由可访问 |

### 部署与联调前置条件

1. 必须先执行数据库迁移脚本 `V21`、`V22`、`V23`。
2. 必须配置 Redis，CDK 限流和明文导出窗口依赖 Redis。
3. 必须通过环境变量或外部配置注入 `CDK_PEPPER`，长度不少于 32 字节；仓库内不保存真实密钥。
4. `src/main/resources/application.yml` 已映射 `app.cdk.pepper: ${CDK_PEPPER:}`，本地可通过 `export CDK_PEPPER='...'` 注入。
5. 生产环境需要确认 `CDK_PEPPER`、`JWT_SECRET`、数据库、Redis、CORS 等配置均由外部注入。

### 仍需推进内容

| 优先级 | 待办 | 说明 |
| --- | --- | --- |
| 高 | 完成目标环境联调 | 本地开发 MySQL/Redis 已完成集成测试；仍需在目标部署环境跑迁移并走完整兑换链路 |
| 中 | 二阶段权益发放 | 按阶段二实现权限/服务包权益、冻结确认/取消的业务页面和补偿任务 |
| 中 | 对账差异记录 | 当前已接入 Quartz 日终对账，后续应持久化差异记录并提供处理入口 |
| 中 | 导出文件强化 | 当前返回一次性明文列表，后续可改为加密 ZIP 和受控临时文件下载 |
| 中 | 高价值双人复核 | 当前已有审批流状态，高价值批次双人复核规则尚未落地 |
| 低 | 在线充值扩展 | 按阶段三接入在线充值渠道、支付回调验签和补偿 |

设计原则按金融级企业项目处理：

- 积分只能为整数，所有金额类字段使用 `BIGINT`，禁止浮点数。
- 任何积分变动必须先有业务单据，再有账本流水，账本只追加不物理删除。
- 所有充值、扣减、冻结、解冻、兑换动作必须支持幂等、并发安全、审计追溯和对账。
- CDK 不明文落库，后台生成后只允许在受控流程中一次性导出。
- CDK 兑换必须抗暴力枚举、抗重放、抗并发重复兑换。
- 后续在线充值、第三方支付、权限兑换、服务套餐兑换必须通过统一交易接口扩展，不重写积分核心账务逻辑。

## 2. 模块边界

### 2.1 后端模块

新增包建议：

| 包路径 | 说明 |
| --- | --- |
| `com.winsalty.quickstart.cdk` | CDK 批次、码池、兑换、风控、导出审计 |
| `com.winsalty.quickstart.points` | 积分账户、充值、扣减、冻结、账本、对账 |
| `com.winsalty.quickstart.benefit` | 权益发放抽象，承接 CDK 和积分兑换后的具体权益 |
| `com.winsalty.quickstart.trade` | 可选交易抽象层，为后续在线充值统一订单状态和回调幂等 |

### 2.2 前端模块

新增页面与入口：

| 位置 | 能力 |
| --- | --- |
| 首页工作台 | 展示当前用户积分余额，点击跳转积分详情 |
| 个人中心或钱包中心 | 积分余额、充值入口、消费记录、充值记录、冻结记录 |
| 系统管理 / CDK 管理 | 批次生成、审批、导出、禁用、兑换记录查询 |
| 系统管理 / 积分审计 | 用户积分账户、流水查询、对账差异、人工调整审批 |

## 3. 核心安全模型

### 3.1 CDK 安全要求

CDK 可以兑换重要权益，按高价值凭证处理：

1. 明文 CDK 只在生成任务内短暂存在，数据库只保存 `code_hash`。
2. `code_hash` 使用 `HMAC-SHA256(cdkPlainText, CDK_PEPPER)`，`CDK_PEPPER` 通过环境变量或 KMS 注入，不进入仓库。
3. CDK 明文长度必须具备足够熵，建议随机部分不少于 128 bit，并增加前缀、批次标识和校验位。
4. CDK 展示格式建议：`WSA-202604-XXXX-XXXX-XXXX-XXXX-C`，其中 `C` 为校验位；真实校验算法用后端常量实现。
5. CDK 兑换接口必须限流：用户维度、IP 维度、设备维度、错误次数维度。
6. 连续错误达到阈值后临时锁定兑换能力，避免撞库。
7. CDK 批量导出必须产生审计日志，记录导出人、导出时间、批次号、数量、文件指纹，不记录明文内容。
8. 批次生成建议走审批流：创建人提交，审批人确认后生成；高价值批次必须双人复核。
9. 批次和码支持状态控制：草稿、待审批、已生成、已启用、已暂停、已过期、已作废。
10. 兑换成功后同一 CDK 必须不可再次兑换，数据库唯一索引和事务行锁双重保证。

### 3.2 积分安全要求

积分按照准资金账务设计：

1. 积分账户余额只允许通过积分服务变更，不允许业务模块直接更新余额表。
2. 每次积分变更必须写入单据表和账本表，同一数据库事务内提交。
3. 账本流水包含变更前余额、变更后余额、业务类型、业务单号、幂等号、操作人、traceId。
4. 账本流水建立哈希链：`entry_hash = SHA-256(prev_hash + ledger_fields)`，支持后续审计验证是否被篡改。
5. 扣减积分必须使用原子条件更新或账户行锁，保证余额不为负。
6. 高并发扣减必须有幂等键，重复请求返回首次处理结果。
7. 支持冻结积分，面向后续兑换权限或服务的二阶段扣减。
8. 所有失败、取消、回滚都通过反向流水处理，不直接修改历史流水。
9. 提供日终对账任务，校验账户表余额与流水汇总一致性。
10. 管理员人工调整必须单独审批，并强制填写原因、附件或工单号。

## 4. 数据库设计

SQL 脚本统一放入项目根目录 `resources/sql`，建议按迁移顺序拆分：

- `V21__init_points_schema.sql`
- `V22__init_cdk_schema.sql`
- `V23__seed_points_cdk_permissions.sql`

### 4.1 积分账户表 `point_account`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID，唯一索引 |
| available_points | BIGINT | 可用积分，非负 |
| frozen_points | BIGINT | 冻结积分，非负 |
| total_earned_points | BIGINT | 累计获得 |
| total_spent_points | BIGINT | 累计消耗 |
| version | BIGINT | 乐观锁版本 |
| status | VARCHAR(32) | active、disabled、frozen |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束：

- `available_points >= 0`
- `frozen_points >= 0`
- `user_id` 唯一
- 所有更新必须带 `version` 或原子条件。

### 4.2 积分流水表 `point_ledger`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| ledger_no | VARCHAR(64) | 流水号，唯一 |
| user_id | BIGINT | 用户 ID |
| account_id | BIGINT | 积分账户 ID |
| direction | VARCHAR(32) | earn、spend、freeze、unfreeze、refund |
| amount | BIGINT | 变动积分，正整数 |
| balance_before | BIGINT | 变动前可用余额 |
| balance_after | BIGINT | 变动后可用余额 |
| frozen_before | BIGINT | 变动前冻结余额 |
| frozen_after | BIGINT | 变动后冻结余额 |
| biz_type | VARCHAR(64) | cdk_recharge、online_recharge、benefit_consume、admin_adjust |
| biz_no | VARCHAR(64) | 业务单号 |
| idempotency_key | VARCHAR(128) | 幂等键 |
| operator_type | VARCHAR(32) | user、admin、system |
| operator_id | VARCHAR(64) | 操作人 |
| trace_id | VARCHAR(64) | 链路追踪 ID |
| prev_hash | VARCHAR(128) | 上一条流水哈希 |
| entry_hash | VARCHAR(128) | 当前流水哈希 |
| remark | VARCHAR(512) | 备注 |
| created_at | DATETIME | 创建时间 |

索引：

- `uk_ledger_no`
- `uk_biz_type_biz_no_direction`
- `uk_user_idempotency_key`
- `idx_user_created_at`
- `idx_biz_no`

### 4.3 积分充值单 `point_recharge_order`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| recharge_no | VARCHAR(64) | 充值单号，唯一 |
| user_id | BIGINT | 用户 ID |
| channel | VARCHAR(32) | cdk、online_pay、admin_adjust |
| amount | BIGINT | 充值积分 |
| status | VARCHAR(32) | created、processing、success、failed、closed |
| external_no | VARCHAR(128) | 第三方支付流水或 CDK 兑换记录号 |
| idempotency_key | VARCHAR(128) | 幂等键 |
| request_snapshot | JSON | 请求摘要 |
| result_snapshot | JSON | 结果摘要 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

在线充值扩展时，支付回调只更新充值单状态，再由积分服务按 `recharge_no` 幂等入账。

### 4.4 积分冻结单 `point_freeze_order`

用于后续兑换权限或服务时做二阶段扣减：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| freeze_no | VARCHAR(64) | 冻结单号 |
| user_id | BIGINT | 用户 ID |
| amount | BIGINT | 冻结积分 |
| biz_type | VARCHAR(64) | 权益兑换、服务购买等 |
| biz_no | VARCHAR(64) | 外部业务单号 |
| status | VARCHAR(32) | frozen、confirmed、cancelled、expired |
| expire_at | DATETIME | 冻结过期时间 |
| idempotency_key | VARCHAR(128) | 幂等键 |

冻结成功后下游服务执行权益发放；成功则确认扣减，失败则取消解冻。

### 4.5 CDK 批次表 `cdk_batch`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| batch_no | VARCHAR(64) | 批次号，唯一 |
| batch_name | VARCHAR(128) | 批次名称 |
| benefit_type | VARCHAR(32) | points、permission、service_package |
| benefit_config | JSON | 权益配置，后端按类型校验 |
| total_count | INT | 总数量 |
| generated_count | INT | 已生成数量 |
| redeemed_count | INT | 已兑换数量 |
| valid_from | DATETIME | 生效时间 |
| valid_to | DATETIME | 失效时间 |
| status | VARCHAR(32) | draft、pending_approval、active、paused、expired、voided |
| risk_level | VARCHAR(32) | normal、high、critical |
| created_by | VARCHAR(64) | 创建人 |
| approved_by | VARCHAR(64) | 审批人 |
| approved_at | DATETIME | 审批时间 |
| export_count | INT | 导出次数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

`benefit_config` 示例：

```json
{
  "points": 1000,
  "expireDays": 365,
  "grantReason": "activity_reward"
}
```

配置必须由后端 DTO 强校验，禁止前端提交任意 JSON 后直接执行。

### 4.6 CDK 码表 `cdk_code`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| batch_id | BIGINT | 批次 ID |
| code_hash | VARCHAR(128) | HMAC 后的码值，唯一 |
| code_prefix | VARCHAR(32) | 明文前缀，用于运营排查 |
| checksum | VARCHAR(16) | 校验位 |
| status | VARCHAR(32) | active、redeemed、disabled、expired |
| redeemed_user_id | BIGINT | 兑换用户 |
| redeemed_at | DATETIME | 兑换时间 |
| redeem_record_no | VARCHAR(64) | 兑换记录号 |
| version | BIGINT | 乐观锁 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

约束：

- `code_hash` 唯一。
- `batch_id + status` 索引用于批次统计。
- 更新 `active -> redeemed` 必须带状态条件。

### 4.7 CDK 兑换记录表 `cdk_redeem_record`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| redeem_no | VARCHAR(64) | 兑换记录号 |
| user_id | BIGINT | 用户 ID |
| batch_id | BIGINT | 批次 ID |
| code_id | BIGINT | CDK ID |
| benefit_type | VARCHAR(32) | 权益类型 |
| benefit_snapshot | JSON | 发放快照 |
| status | VARCHAR(32) | processing、success、failed |
| failure_code | VARCHAR(64) | 失败码 |
| failure_message | VARCHAR(256) | 失败原因 |
| client_ip | VARCHAR(64) | 客户端 IP |
| user_agent_hash | VARCHAR(128) | UA 摘要 |
| trace_id | VARCHAR(64) | 链路 ID |
| created_at | DATETIME | 创建时间 |

## 5. 后端接口设计

所有接口必须走统一认证和权限控制。管理员接口只允许 `ROLE_ADMIN` 或后续细粒度权限码访问。

### 5.1 用户积分接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/points/account` | GET | 查询当前用户积分账户 |
| `/api/points/ledger` | GET | 查询当前用户积分流水 |
| `/api/points/recharge/orders` | GET | 查询充值记录 |
| `/api/points/consume/orders` | GET | 查询消费记录 |
| `/api/points/cdk/redeem` | POST | 使用 CDK 充值或兑换权益 |

`/api/points/cdk/redeem` 入参：

```json
{
  "cdk": "WSA-202604-XXXX-XXXX-XXXX-XXXX-C",
  "idempotencyKey": "client-generated-uuid"
}
```

响应只返回业务结果，不返回 CDK 明文或敏感内部字段。

### 5.2 积分服务内部接口

封装 `PointAccountService`，禁止业务直接操作 Mapper：

| 方法 | 说明 |
| --- | --- |
| `getOrCreateAccount(userId)` | 获取或初始化账户 |
| `credit(command)` | 充值入账 |
| `debit(command)` | 直接扣减 |
| `freeze(command)` | 冻结积分 |
| `confirmFreeze(command)` | 确认冻结扣减 |
| `cancelFreeze(command)` | 取消冻结并解冻 |
| `refund(command)` | 退款或反向补偿 |

所有 command 必须包含：

- `userId`
- `amount`
- `bizType`
- `bizNo`
- `idempotencyKey`
- `operatorType`
- `operatorId`
- `remark`

### 5.3 管理员 CDK 接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/admin/cdk/batches` | GET | 批次列表 |
| `/api/admin/cdk/batches` | POST | 创建批次草稿 |
| `/api/admin/cdk/batches/{id}/submit` | POST | 提交审批 |
| `/api/admin/cdk/batches/{id}/approve` | POST | 审批并生成 |
| `/api/admin/cdk/batches/{id}/pause` | POST | 暂停批次 |
| `/api/admin/cdk/batches/{id}/void` | POST | 作废批次 |
| `/api/admin/cdk/batches/{id}/export` | POST | 导出明文 CDK 文件 |
| `/api/admin/cdk/redeem-records` | GET | 兑换记录查询 |

批量生成要求：

- 单批次数量必须有上限，建议默认 10000，可通过配置控制。
- 高价值批次必须异步任务生成，避免请求超时。
- 导出文件建议使用加密 ZIP，并将下载权限限定在审批通过后的短窗口。
- 导出后只保存文件指纹和操作审计，不保存明文 CDK。

### 5.4 管理员积分接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/admin/points/accounts` | GET | 用户积分账户查询 |
| `/api/admin/points/ledger` | GET | 全量流水审计查询 |
| `/api/admin/points/adjustments` | POST | 发起人工调整申请 |
| `/api/admin/points/adjustments/{id}/approve` | POST | 审批人工调整 |
| `/api/admin/points/reconciliation` | GET | 对账结果查询 |

人工调整必须先落调整单，审批通过后再调用积分服务入账或扣减。

## 6. 关键交易流程

### 6.1 CDK 兑换积分流程

1. 前端提交 CDK 和幂等键。
2. 后端标准化 CDK 格式，先校验校验位。
3. 限流服务检查用户、IP、设备、错误次数。
4. 使用 `CDK_PEPPER` 计算 `code_hash`。
5. 查询 `cdk_code` 和 `cdk_batch`，校验状态、生效时间、过期时间、权益配置。
6. 开启数据库事务。
7. 使用条件更新将 `cdk_code.status` 从 `active` 更新为 `redeemed`，失败则说明已被兑换或状态变化。
8. 写入 `cdk_redeem_record`，状态为 `processing`。
9. 根据 `benefit_type` 调用权益发放器。积分类型调用 `PointAccountService.credit`。
10. 写入积分充值单和积分流水，更新账户余额。
11. 更新兑换记录为 `success`。
12. 事务提交。
13. 返回兑换结果。

失败策略：

- CDK 已被兑换、过期、暂停：返回明确业务错误。
- 积分入账失败：事务回滚，CDK 不变为已兑换。
- 外部权益发放失败：如无法同事务，使用 outbox 事件和补偿任务处理。

### 6.2 积分扣减流程

直接扣减适用于纯本地事务业务：

1. 业务生成消费单。
2. 调用 `PointAccountService.debit`。
3. 服务检查幂等键。
4. 原子扣减：`UPDATE point_account SET available_points = available_points - ?, version = version + 1 WHERE user_id = ? AND available_points >= ?`
5. 查询扣减后账户，写入账本流水。
6. 返回扣减成功。

二阶段扣减适用于需要调用外部权益或服务的业务：

1. `freeze` 冻结积分。
2. 下游服务发放权益。
3. 成功则 `confirmFreeze`，将冻结积分转为实际消费。
4. 失败或超时则 `cancelFreeze`，返还可用积分。

## 7. 分布式事务与并发控制

建议优先使用本地事务 + Outbox + 幂等补偿，不引入重量级 XA。

### 7.1 本地强一致范围

同库事务内必须强一致：

- CDK 状态变更
- CDK 兑换记录
- 积分账户余额
- 积分充值单或消费单
- 积分账本

### 7.2 跨模块最终一致范围

跨服务或未来微服务拆分时使用：

- `transaction_outbox` 事件表
- 事件唯一键 `event_no`
- 消费方幂等表
- 定时补偿任务
- 失败告警和人工处理台

### 7.3 并发控制策略

| 场景 | 控制方式 |
| --- | --- |
| 同一 CDK 并发兑换 | `code_hash` 唯一 + 状态条件更新 |
| 同一用户高并发扣减 | 账户行条件更新或 `SELECT FOR UPDATE` |
| 重复充值回调 | `channel + external_no` 唯一 |
| 重复业务扣减 | `biz_type + biz_no + direction` 唯一 |
| 重复客户端请求 | `user_id + idempotency_key` 唯一 |

## 8. 审计与风控

### 8.1 审计日志

必须接入现有 `@AuditLog`：

- CDK 批次创建、审批、导出、暂停、作废
- CDK 兑换成功和失败
- 积分人工调整申请和审批
- 积分扣减失败、余额不足、幂等命中
- 对账任务执行结果

### 8.2 风控规则

首期至少支持：

- 单用户每分钟 CDK 尝试次数限制。
- 单 IP 每分钟 CDK 尝试次数限制。
- 连续错误 CDK 兑换临时锁定。
- 高价值 CDK 批次强制审批。
- 管理员导出 CDK 频率限制。
- 异常兑换告警：同 IP 多账号、同账号短时间多次高价值兑换。

### 8.3 对账任务

新增定时任务：

- 每日汇总 `point_ledger`，校验与 `point_account` 一致。
- 抽样验证流水哈希链。
- 检查 `processing` 状态超时单据。
- 检查冻结单过期未处理。
- 检查 CDK 批次统计和码表统计是否一致。

## 9. 前端设计

### 9.1 首页积分卡片

工作台首页新增钱包卡片：

- 展示可用积分。
- 展示冻结积分。
- 展示最近一笔积分变动。
- 点击卡片跳转 `/points/wallet`。
- 无账户时展示 0，不暴露后端初始化细节。

### 9.2 积分钱包页

路径建议：`/points/wallet`

页面结构：

- 顶部余额区：可用积分、冻结积分、累计获得、累计消费。
- 充值入口：CDK 输入框、兑换按钮、兑换结果提示。
- 记录 Tab：
  - 积分流水
  - 充值记录
  - 消费记录
  - 冻结记录
- 每条记录展示业务类型、积分变动、前后余额、时间、状态、业务单号。

交互要求：

- CDK 输入本地做格式化展示，但提交前必须以后端校验为准。
- 兑换按钮需要 loading 和防重复点击。
- 兑换失败不展示敏感判断细节，例如不区分“格式撞库”和“不存在”的内部原因。

### 9.3 管理后台 CDK 页面

页面建议：

- 批次列表：批次号、名称、权益类型、数量、已兑换、有效期、状态、风险等级。
- 批次创建表单：权益类型、积分数量或权益配置、有效期、数量、备注。
- 审批弹窗：展示风险摘要和权益总价值。
- 导出弹窗：二次确认，提示明文 CDK 只在导出文件中出现。
- 兑换记录页：支持按用户、批次、状态、时间范围查询。

### 9.4 菜单与权限码

新增路由码：

- `points_wallet`
- `points_admin_account`
- `points_admin_ledger`
- `cdk_batch`
- `cdk_redeem_record`

新增按钮权限：

- `cdk:batch:create`
- `cdk:batch:approve`
- `cdk:batch:export`
- `cdk:batch:pause`
- `cdk:batch:void`
- `points:adjust:apply`
- `points:adjust:approve`
- `points:ledger:view`

## 10. 配置项

新增配置建议：

```yaml
app:
  cdk:
    pepper: ${CDK_PEPPER:}
    max-batch-size: ${CDK_MAX_BATCH_SIZE:10000}
    redeem-user-window-seconds: ${CDK_REDEEM_USER_WINDOW_SECONDS:60}
    redeem-user-limit: ${CDK_REDEEM_USER_LIMIT:10}
    redeem-ip-window-seconds: ${CDK_REDEEM_IP_WINDOW_SECONDS:60}
    redeem-ip-limit: ${CDK_REDEEM_IP_LIMIT:30}
    redeem-lock-seconds: ${CDK_REDEEM_LOCK_SECONDS:900}
  points:
    reconciliation-enabled: ${POINTS_RECONCILIATION_ENABLED:true}
    freeze-default-expire-seconds: ${POINTS_FREEZE_DEFAULT_EXPIRE_SECONDS:1800}
```

生产环境要求：

- `CDK_PEPPER` 必须显式注入，长度不少于 32 字节。
- 批量导出目录必须为受控临时目录，定期清理。
- 高价值权益阈值通过配置或系统参数维护。

## 11. 实施阶段

### 阶段一：基础账务和 CDK 兑换积分

后端：

- 新建积分账户、充值单、流水表。
- 新建 CDK 批次、码表、兑换记录表。
- 实现积分账户初始化、充值、扣减、冻结核心服务。
- 实现 CDK 生成、审批、兑换积分。
- 接入限流、审计、幂等、并发测试。

前端：

- 首页积分卡片。
- 用户钱包页。
- CDK 兑换入口。
- 管理员 CDK 批次列表和创建审批基础页面。

验收：

- 同一 CDK 并发兑换只有一次成功。
- 同一幂等键重复兑换返回同一结果。
- 积分余额不能扣成负数。
- 账本流水和账户余额可对账。

### 阶段二：权益发放和二阶段扣减

后端：

- 新增权益发放抽象和适配器。
- 支持积分兑换权限或服务。
- 实现冻结、确认扣减、取消冻结。
- 新增 outbox 事件和补偿任务。

前端：

- 权益兑换页面。
- 消费订单和冻结记录展示。

验收：

- 权益发放失败可自动解冻。
- 重复确认、重复取消具备幂等。
- 超时冻结单能被补偿任务处理。

### 阶段三：在线充值扩展

后端：

- 新增在线充值渠道抽象。
- 支持支付创建、回调验签、充值入账。
- 增加第三方流水号唯一约束和回调重放防护。

前端：

- 在线充值入口。
- 充值状态轮询或通知。

验收：

- 支付回调重复推送不重复入账。
- 支付成功但入账失败可补偿。
- 充值订单、流水、账户余额一致。

### 阶段四：高级审计和运营风控

后端：

- 账本哈希链校验任务。
- 对账报表和差异处理。
- 高价值 CDK 双人复核。
- 异常兑换告警。

前端：

- 对账结果页。
- 审批中心。
- 风控告警列表。

验收：

- 人工调整全链路可回溯。
- 导出 CDK 可审计。
- 对账差异可定位到具体单据和流水。

## 12. 测试计划

### 12.1 单元测试

- CDK 生成格式、校验位、HMAC Hash。
- CDK 状态机转换。
- 积分充值、扣减、冻结、确认、取消。
- 幂等键重复请求。
- 账本哈希计算。

### 12.2 集成测试

- CDK 兑换积分完整链路。
- CDK 过期、暂停、作废、已兑换。
- 高并发同码兑换。
- 高并发余额扣减。
- 余额不足扣减失败。
- 充值单重复回调。

### 12.3 安全测试

- 暴力枚举 CDK 限流。
- 非管理员访问管理接口。
- 批次导出审计。
- 请求参数越权，例如给其他用户充值或查询流水。
- CDK 明文不落库、不进日志。

### 12.4 对账测试

- 随机生成积分流水，校验账户余额。
- 人工篡改流水后哈希链校验失败。
- 处理中单据超时补偿。

## 13. 上线与运维

上线前检查：

- `CDK_PEPPER` 已在环境中配置。
- 数据库迁移已执行。
- 管理员权限码已初始化。
- 限流 Redis 可用。
- 审计日志写入正常。
- 定时对账任务开启。
- CDK 批量导出临时目录具备清理策略。

上线后观察：

- CDK 兑换失败率。
- CDK 限流触发次数。
- 积分充值和扣减成功率。
- `point_account` 与 `point_ledger` 对账差异数。
- `processing` 状态单据积压数量。
- 冻结单超时数量。

## 14. 开放问题

实施前需要确认：

1. 积分是否允许过期；如允许，需要增加积分批次有效期和先进先出扣减策略。
2. 积分是否允许管理员扣减；如允许，必须走人工调整审批。
3. CDK 是否允许指定用户、指定角色、指定渠道使用。
4. 权益兑换权限或服务时，权益是否有有效期、次数限制、等级叠加规则。
5. 高价值 CDK 的审批阈值和单批次上限。
6. 是否需要短信、邮件或站内信通知用户积分到账和消费。
