# CDK 与卡密凭证模块重构设计和开发计划

## 1. 背景

当前项目按新项目处理，不需要考虑历史数据、旧表结构或旧接口兼容。CDK 最初围绕“兑换当前平台积分”设计，核心对象是批次、CDK 明细、兑换记录。后续新增的临时提取链接、批次批量生成提取链接、一个链接包含多个 CDK 等能力，本质已经从“积分兑换码”扩展到了“可管理、可分发、可审计的凭证/卡密管理”。

如果继续按 CDK 模型堆功能，会出现三个结构性问题：

1. 提取链接生成后没有独立管理入口。管理员只能在 CDK 明细或批次详情的局部弹窗里查看，缺少全局检索、停用、复制、补发、访问审计汇总和批量运营能力。
2. CDK 概念被过度复用。积分兑换码、外部卡密文本、一次性提取链接都被放在 CDK 模型下，后续接入更多凭证类型时会继续堆叠分支。
3. 卡密导入与 CDK 生成有共同底座，但业务动作不同。二者都需要批次、密文存储、哈希去重、状态机、链接分发和审计；积分 CDK 还需要兑换入账，文本卡密更关注导入、提取和交付。

因此建议直接建设“凭证中心”，以“凭证分类 + 凭证批次 + 凭证明细 + 履约类型 + 提取链接”为主抽象。CDK 只是其中一种系统生成凭证类型，卡密是文本导入凭证类型。

## 2. 目标

| 目标 | 说明 |
| --- | --- |
| 统一抽象 | 将积分 CDK 和文本卡密抽象为同一套凭证批次、凭证明细、提取链接和审计能力 |
| 绿地建设 | 不保留旧 `cdk_*` 表和旧接口兼容层，直接按 `credential_*` 模型开发 |
| 分类配置 | 支持凭证分类，分类决定履约类型，例如兑换系统积分或提取文本卡密 |
| 卡密导入 | 支持管理员批量粘贴文本卡密，配置自定义分隔符、预览、去重、错误行提示和确认导入 |
| 链接管理 | 新增提取链接管理页，支持查看、筛选、复制、停用、延期、补发、查看访问记录 |
| 多卡密提取 | 提取页面天然支持一个链接展示多个凭证文本，并提供单个复制和全部复制 |
| 安全审计 | 明文敏感值加密存储，token 和卡密不进日志；所有查看、复制、导出、提取动作可审计 |

## 3. 非目标

1. 本次重构不改变积分账户、充值、账本和积分兑换入账的既有模型。
2. 不在第一阶段引入复杂的第三方发货系统、库存系统或供应商接口。
3. 不支持任意正则分隔符作为默认能力，优先支持安全可控的固定分隔符字符串；正则解析可作为高级选项后置。

## 4. 核心概念

### 4.1 凭证

凭证是可生成、导入、保存、分发、提取或兑换的敏感文本。凭证明细对应一条可交付的值：

- 积分 CDK：系统生成的兑换码，用户登录后兑换为当前平台积分。
- 文本卡密：管理员从外部渠道导入的卡号、兑换码、授权码、会员码等文本，用户通过提取链接获取。
- 未来扩展：优惠券码、授权许可证、外部权益码、指定用户权益凭证。

### 4.2 履约类型

履约类型决定凭证被使用后的业务动作：

| 履约类型 | 业务含义 | 明文交付 | 是否需要登录 | 消费动作 |
| --- | --- | --- | --- | --- |
| `POINTS_REDEEM` | 兑换当前平台积分 | 可通过提取链接展示 CDK | 兑换时需要登录 | 兑换成功后写积分账本 |
| `TEXT_SECRET` | 文本卡密提取 | 通过提取链接展示卡密 | 默认不需要登录 | 首次成功提取后标记已提取 |
| `RESERVED` | 预留扩展 | 按分类配置 | 按分类配置 | 由后续履约适配器实现 |

### 4.3 生成模式

| 生成模式 | 说明 | 适用类型 |
| --- | --- | --- |
| `SYSTEM_GENERATED` | 后端按规则生成随机码并保存 | 积分 CDK、内部券码 |
| `TEXT_IMPORTED` | 管理员批量粘贴或上传文本导入 | 外部卡密、授权码 |
| `MIXED` | 同一分类允许后续混合生成和导入 | 供应商多来源凭证 |

## 5. 建议命名

代码层直接使用 `credential` 作为主模块名。考虑中文业务里“凭证”覆盖 CDK 与卡密，后端包名和前端路由都应围绕 `credential` 建设：

| 层级 | 建议名称 | 说明 |
| --- | --- | --- |
| 后端模块 | `credential` | 通用凭证核心包 |
| 前端菜单 | 凭证中心 | 面向管理员统一管理 |
| 前端路由 | `/system/credentials/*` | 管理端凭证中心路由 |
| 公开路由 | `/credentials/extract/:token` | 公开提取页路由 |
| 分类示例 | 系统积分兑换码、文本卡密 | 由分类决定业务表现 |

不建议继续新增 `/system/cdk/...` 路由，避免业务语义被 CDK 绑定。用户侧兑换入口可以保留“CDK 兑换”的产品文案，但接口和服务仍使用凭证核心能力。

## 6. 数据模型设计

### 6.1 凭证分类表 `credential_category`

用于定义某类凭证的履约方式、导入规则、安全策略和展示名称。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| category_code | VARCHAR(64) | 分类编码，唯一 |
| category_name | VARCHAR(100) | 分类名称 |
| fulfillment_type | VARCHAR(32) | `POINTS_REDEEM`、`TEXT_SECRET` |
| generation_mode | VARCHAR(32) | `SYSTEM_GENERATED`、`TEXT_IMPORTED`、`MIXED` |
| payload_schema | TEXT | JSON，履约参数结构，例如积分配置 |
| import_config | TEXT | JSON，默认分隔符、是否去重、最大长度、大小写策略 |
| extract_policy | TEXT | JSON，是否允许提取、是否首次提取消费、是否允许复制 URL |
| status | VARCHAR(32) | `active`、`disabled` |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

初始化分类：

1. `POINTS_CDK`：系统积分兑换码，履约类型 `POINTS_REDEEM`，生成模式 `SYSTEM_GENERATED`。
2. `TEXT_CARD_SECRET`：文本卡密，履约类型 `TEXT_SECRET`，生成模式 `TEXT_IMPORTED`。

### 6.2 凭证批次表 `credential_batch`

作为凭证中心的唯一批次主表，承载系统生成 CDK 和文本卡密批次。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| batch_no | VARCHAR(64) | 批次号，唯一 |
| batch_name | VARCHAR(100) | 批次名称 |
| category_id | BIGINT | 凭证分类 ID |
| fulfillment_type | VARCHAR(32) | 冗余分类履约类型，便于查询 |
| generation_mode | VARCHAR(32) | 本批次实际生成模式 |
| payload_config | TEXT | JSON，积分数量或文本卡密展示配置 |
| total_count | INT | 目标总数 |
| available_count | INT | 可用数量 |
| consumed_count | INT | 已兑换或已提取数量 |
| linked_count | INT | 已绑定提取链接的数量 |
| valid_from | DATETIME | 生效时间 |
| valid_to | DATETIME | 失效时间 |
| status | VARCHAR(32) | `draft`、`active`、`disabled`、`finished` |
| created_by | BIGINT | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 6.3 凭证明细表 `credential_item`

统一存储系统生成 CDK 和导入卡密。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| batch_id | BIGINT | 批次 ID |
| category_id | BIGINT | 分类 ID |
| item_no | VARCHAR(64) | 明细编号，唯一 |
| secret_hash | VARCHAR(128) | HMAC 后的敏感文本哈希，用于查重和兑换查询 |
| encrypted_secret | TEXT | AES-GCM 加密后的敏感文本 |
| secret_mask | VARCHAR(128) | 脱敏展示值 |
| checksum | VARCHAR(16) | 校验位或短摘要 |
| payload_snapshot | TEXT | JSON，批次履约配置快照 |
| source_type | VARCHAR(32) | `generated`、`imported` |
| source_line_no | INT | 导入行号，生成模式为空 |
| status | VARCHAR(32) | `active`、`linked`、`extracted`、`redeemed`、`disabled`、`expired` |
| consumed_user_id | BIGINT | 兑换用户，文本卡密为空 |
| consumed_at | DATETIME | 兑换或首次提取时间 |
| consume_biz_no | VARCHAR(64) | 兑换记录号或提取记录号 |
| version | INT | 乐观锁版本 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

状态语义需要区分履约类型：

- 积分 CDK：`active` 可兑换；提取链接只展示明文，不代表已兑换；兑换成功后变为 `redeemed`。
- 文本卡密：生成提取链接后默认变为 `linked`，避免同一条卡密进入多个链接；首次成功公开提取后变为 `extracted`。

### 6.4 提取链接表 `credential_extract_link`

作为凭证中心的唯一提取链接主表，提供全局管理能力。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| link_no | VARCHAR(64) | 链接编号 |
| category_id | BIGINT | 分类 ID |
| batch_id | BIGINT | 批次 ID |
| token_hash | VARCHAR(128) | token HMAC |
| encrypted_token | TEXT | 可选，token 密文；用于后续复制 URL |
| token_key_id | VARCHAR(64) | token 加密密钥版本 |
| item_count | INT | 链接包含凭证数量 |
| max_access_count | INT | 最大成功访问次数 |
| accessed_count | INT | 已成功访问次数 |
| expire_at | DATETIME | 过期时间 |
| status | VARCHAR(32) | `active`、`disabled`、`expired`、`exhausted` |
| created_by | BIGINT | 创建人 |
| disabled_by | BIGINT | 停用人 |
| disabled_at | DATETIME | 停用时间 |
| last_accessed_at | DATETIME | 最近成功访问时间 |
| remark | VARCHAR(255) | 备注 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

`encrypted_token` 的取舍：

- 如果业务要求“链接生成后仍可复制”，需要保存 token 密文，并对每次复制 URL 写审计。
- 如果安全要求极高，可以不保存 token 明文，只允许首次生成后复制，后续通过“补发链接”重新生成。管理页仍可查看状态、停用、访问记录和覆盖明细。

本项目偏运营后台场景，建议默认启用 `encrypted_token`，使用独立环境变量 `CREDENTIAL_EXTRACT_TOKEN_ENCRYPTION_KEY` 加密保存，且所有复制 URL 操作必须审计。

### 6.5 提取链接明细表 `credential_extract_link_item`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| link_id | BIGINT | 提取链接 ID |
| item_id | BIGINT | 凭证明细 ID |
| batch_id | BIGINT | 批次 ID |
| sort_no | INT | 展示顺序 |
| created_at | DATETIME | 创建时间 |

### 6.6 提取访问记录表 `credential_extract_access_record`

记录公开提取页每次访问结果、失败原因和设备维度。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| access_no | VARCHAR(64) | 访问流水号 |
| link_id | BIGINT | 链接 ID |
| batch_id | BIGINT | 批次 ID |
| item_count | INT | 本次返回凭证数量 |
| success | TINYINT | 是否成功返回明文 |
| failure_reason | VARCHAR(64) | 失败原因 |
| client_ip | VARCHAR(64) | 访问 IP |
| user_agent_hash | VARCHAR(128) | UA 摘要 |
| browser_fingerprint | VARCHAR(128) | 浏览器指纹 |
| device_snapshot | TEXT | 前端采集的设备摘要 JSON |
| trace_id | VARCHAR(64) | 链路追踪 ID |
| created_at | DATETIME | 创建时间 |

### 6.7 导入任务表 `credential_import_task`

用于文本卡密批量导入的可追踪、可复核和可失败重试。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| task_no | VARCHAR(64) | 导入任务号 |
| batch_id | BIGINT | 批次 ID |
| category_id | BIGINT | 分类 ID |
| delimiter | VARCHAR(32) | 分隔符展示值，例如 `\\n`、`,`、`;`、`\\t` |
| total_rows | INT | 解析总行数 |
| valid_rows | INT | 有效行数 |
| duplicate_rows | INT | 重复行数 |
| invalid_rows | INT | 无效行数 |
| import_hash | VARCHAR(128) | 原始导入内容摘要，不保存原文 |
| result_summary | TEXT | JSON，错误行号、重复示例、截断后的提示 |
| status | VARCHAR(32) | `previewed`、`importing`、`success`、`failed` |
| created_by | BIGINT | 创建人 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

导入原始文本不落库、不进日志；如需排查，只保存摘要、行号和脱敏示例。

### 6.8 凭证兑换记录表 `credential_redeem_record`

用于记录 `POINTS_REDEEM` 类型凭证的兑换流水，和积分账本形成业务闭环。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| record_no | VARCHAR(64) | 兑换记录号 |
| item_id | BIGINT | 凭证明细 ID |
| batch_id | BIGINT | 批次 ID |
| category_id | BIGINT | 分类 ID |
| user_id | BIGINT | 兑换用户 |
| points | BIGINT | 入账积分 |
| idempotency_key | VARCHAR(128) | 幂等键 |
| client_ip | VARCHAR(64) | 兑换 IP |
| user_agent_hash | VARCHAR(128) | UA 摘要 |
| device_fingerprint | VARCHAR(128) | 设备指纹 |
| ledger_no | VARCHAR(64) | 积分账本流水号 |
| status | VARCHAR(32) | `success`、`failed` |
| failure_reason | VARCHAR(64) | 失败原因 |
| created_at | DATETIME | 创建时间 |

### 6.9 凭证操作审计表 `credential_operation_audit`

用于记录管理员查看明文、复制 URL、导出 CSV、停用、延期、补发等高敏操作。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| audit_no | VARCHAR(64) | 审计编号 |
| operator_id | BIGINT | 操作人 |
| operation_type | VARCHAR(64) | 操作类型 |
| target_type | VARCHAR(32) | `batch`、`item`、`extract_link`、`import_task` |
| target_id | BIGINT | 目标 ID |
| before_snapshot | TEXT | 操作前脱敏快照 |
| after_snapshot | TEXT | 操作后脱敏快照 |
| client_ip | VARCHAR(64) | 管理端 IP |
| success | TINYINT | 是否成功 |
| failure_reason | VARCHAR(64) | 失败原因 |
| created_at | DATETIME | 创建时间 |

## 7. 后端架构设计

### 7.1 分层

建议新增 `com.winsalty.quickstart.credential` 包：

| 包 | 职责 |
| --- | --- |
| `credential.entity` | 分类、批次、明细、链接、访问记录、导入任务实体 |
| `credential.mapper` | MyBatis-Plus Mapper |
| `credential.service` | 凭证生成、导入、提取链接、公开提取、履约接口 |
| `credential.fulfillment` | 履约适配器，积分兑换和文本提取按类型分派 |
| `credential.controller.admin` | 管理端分类、批次、明细、链接、导入任务接口 |
| `credential.controller.publics` | 公开提取接口 |
| `credential.dto` | 请求 DTO |
| `credential.vo` | 前端展示 VO |
| `credential.constant` | 常量、状态、错误码 |

积分兑换接口也由 `credential` 模块提供。前端可以继续使用“CDK 兑换”文案，但后端 API 建议统一为 `/api/credentials/redeem`，通过凭证分类和履约类型决定是否走积分入账。

### 7.2 履约适配器

定义 `CredentialFulfillmentHandler`：

| 方法 | 说明 |
| --- | --- |
| `support(fulfillmentType)` | 判断是否处理该履约类型 |
| `validateBatchConfig(payloadConfig)` | 校验批次配置 |
| `onGenerated(batch, items)` | 批次生成后钩子 |
| `consume(request)` | 实际消费凭证 |
| `buildPublicView(items)` | 公开提取页展示转换 |

实现：

- `PointsRedeemFulfillmentHandler`：基于凭证明细和积分账本实现兑换入账事务。
- `TextSecretFulfillmentHandler`：公开提取成功后按策略标记卡密已提取。

### 7.3 导入解析服务

`CredentialImportParseService` 负责：

1. 接收粘贴文本、分隔符、去重策略、大小写策略。
2. 按固定分隔符解析，统一 trim。
3. 过滤空值，校验单条长度、总条数、非法字符策略。
4. 使用 HMAC 计算去重哈希，检查批次内和全库重复。
5. 返回预览结果：总数、有效数、重复数、错误数、前 50 条脱敏预览。
6. 确认导入时批量写入 `credential_item`，不保存原始粘贴文本。

默认分隔符选项：

| 展示 | 实际值 |
| --- | --- |
| 换行 | `\n`，兼容 `\r\n` |
| 逗号 | `,` |
| 分号 | `;` |
| Tab | `\t` |
| 自定义 | 管理员输入 1 到 8 个字符的固定字符串 |

不建议第一版开放任意正则，避免 ReDoS 和不可预期解析结果。若后续开放，需要限制表达式长度、解析耗时和权限。

## 8. API 设计

### 8.1 分类管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/credentials/categories` | 分类列表 |
| POST | `/api/admin/credentials/categories` | 新增分类 |
| PUT | `/api/admin/credentials/categories/{id}` | 更新分类 |
| POST | `/api/admin/credentials/categories/{id}/disable` | 停用分类 |

### 8.2 批次管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/credentials/batches` | 批次分页 |
| POST | `/api/admin/credentials/batches/generated` | 创建系统生成批次 |
| POST | `/api/admin/credentials/batches/imported/preview` | 文本卡密导入预览 |
| POST | `/api/admin/credentials/batches/imported/confirm` | 确认导入并创建批次 |
| GET | `/api/admin/credentials/batches/{id}` | 批次详情 |
| POST | `/api/admin/credentials/batches/{id}/disable` | 停用批次 |

### 8.3 明细管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/credentials/items` | 明细分页 |
| GET | `/api/admin/credentials/items/{id}` | 明细详情 |
| POST | `/api/admin/credentials/items/{id}/disable` | 停用明细 |
| POST | `/api/admin/credentials/items/{id}/enable` | 启用明细 |
| POST | `/api/admin/credentials/items/{id}/reveal` | 管理端查看明文，强审计 |

### 8.4 提取链接管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/credentials/extract-links` | 提取链接分页，解决生成后无处查看问题 |
| POST | `/api/admin/credentials/batches/{id}/extract-links` | 按批次批量生成链接 |
| POST | `/api/admin/credentials/items/{id}/extract-links` | 按单个明细生成链接 |
| GET | `/api/admin/credentials/extract-links/{id}` | 链接详情 |
| GET | `/api/admin/credentials/extract-links/{id}/items` | 链接包含的凭证明细 |
| GET | `/api/admin/credentials/extract-links/{id}/access-records` | 访问记录 |
| POST | `/api/admin/credentials/extract-links/{id}/copy-url` | 解密 token 并返回 URL，写审计 |
| POST | `/api/admin/credentials/extract-links/{id}/disable` | 停用链接 |
| POST | `/api/admin/credentials/extract-links/{id}/extend` | 延长过期时间 |
| POST | `/api/admin/credentials/extract-links/{id}/reissue` | 补发链接，默认停用旧链接 |

批次批量生成链接请求建议：

```json
{
  "itemsPerLink": 3,
  "maxAccessCount": 3,
  "expireAt": "2026-05-08 23:59:59",
  "itemScope": "UNLINKED_ACTIVE",
  "outputMode": "URL_AND_COUNT",
  "remark": "渠道A发放"
}
```

关键规则：

1. `itemsPerLink` 表示一个链接包含几个凭证明细。
2. `maxAccessCount` 表示同一个链接可以成功打开几次，不表示可以领取几份新卡密。
3. 文本卡密默认只选择 `active` 且未绑定链接的明细，避免同一卡密出现在多个链接。
4. 积分 CDK 生成提取链接不改变兑换状态。

### 8.5 公开提取

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/public/credentials/extract/{token}` | 公开提取凭证 |

公开返回按履约类型区分：

```json
{
  "linkNo": "CEL202605010001",
  "categoryName": "文本卡密",
  "fulfillmentType": "TEXT_SECRET",
  "batchName": "5月会员卡",
  "remainingAccessCount": 2,
  "items": [
    {
      "itemNo": "CI202605010001",
      "secretText": "CARD-AAAA-BBBB",
      "copyLabel": "卡密 1"
    }
  ]
}
```

公开接口成功返回明文前必须：

1. 校验 token hash。
2. 锁定链接行。
3. 校验状态、过期时间和访问次数。
4. 原子扣减剩余访问次数。
5. 读取关联明细并校验状态。
6. 解密明文。
7. 写访问记录。
8. 按履约策略更新明细状态。

### 8.6 用户兑换

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/credentials/redeem` | 用户兑换积分 CDK |

请求：

```json
{
  "secretText": "CDK-AAAA-BBBB",
  "idempotencyKey": "redeem-20260501-001",
  "deviceFingerprint": "browser-fp"
}
```

处理规则：

1. 标准化输入并校验校验位。
2. 使用 `CREDENTIAL_SECRET_PEPPER` 计算 HMAC 后查询 `POINTS_REDEEM` 类型凭证。
3. 校验凭证、批次、分类状态和有效期。
4. 使用行锁或乐观锁保证同一凭证只兑换一次。
5. 在同一事务内更新凭证状态、写兑换记录、写积分账本。
6. 失败尝试进入用户、IP、指纹维度限流。

## 9. 前端设计

### 9.1 菜单结构

建议新增“凭证中心”菜单：

| 菜单 | 路由 | 说明 |
| --- | --- | --- |
| 凭证批次 | `/system/credentials/batches` | 批次列表、创建生成批次、导入卡密批次 |
| 凭证明细 | `/system/credentials/items` | 查询、查看、停用、生成单条链接 |
| 提取链接 | `/system/credentials/extract-links` | 全局链接管理页 |
| 分类配置 | `/system/credentials/categories` | 凭证分类和履约配置 |
| 导入任务 | `/system/credentials/import-tasks` | 查看卡密导入结果和错误摘要 |
| 兑换记录 | `/system/credentials/redeem-records` | 积分 CDK 兑换记录 |

不新增 CDK 专用管理菜单。所有生成、导入、链接和记录能力统一放在凭证中心，避免管理员在 CDK 和卡密两套入口之间切换。

### 9.2 提取链接管理页

该页面是本次用户反馈的核心补齐项。

列表字段：

- 链接编号
- 分类
- 批次
- 包含凭证数量
- 成功访问次数 / 最大访问次数
- 状态
- 过期时间
- 最近访问时间
- 创建人
- 创建时间
- 备注

筛选项：

- 链接编号
- 分类
- 批次号或批次名称
- 状态
- 创建人
- 过期时间范围
- 最近访问时间范围
- IP 或浏览器指纹

行操作：

- 复制 URL：调用后端 `copy-url`，记录审计。
- 查看明细：抽屉展示链接包含的凭证，默认脱敏，按权限可查看明文。
- 访问记录：抽屉展示 IP、UA 摘要、指纹、设备快照、成功/失败原因。
- 停用链接：二次确认。
- 延期：仅 active 且未过期链接可操作。
- 补发：生成新链接并默认停用旧链接。

交互细节：

1. 批量生成链接后展示结果弹窗，包含生成数量、覆盖凭证数、失败原因和“下载 CSV”。
2. CSV 建议包含链接编号、URL、凭证数量、过期时间、备注，不包含卡密明文。
3. 如果系统配置为“不保存 token 密文”，复制 URL 按钮置灰并提示只能补发。

### 9.3 卡密导入向导

建议在批次页提供“导入卡密批次”按钮，使用四步向导：

1. 基础信息：选择分类、批次名称、有效期、备注。
2. 粘贴卡密：文本域、分隔符选择、自定义分隔符、去空值、批内去重、全库去重策略。
3. 解析预览：展示总行数、有效数、重复数、错误数和前 50 条脱敏预览。
4. 确认导入：提交后显示导入任务结果，可选择导入成功后立即批量生成提取链接。

易用性要求：

- 默认分隔符为换行，适合一行一卡密。
- 自定义分隔符输入旁提供实时解析数量。
- 对重复和错误行给出行号，不展示完整明文。
- 大文本粘贴时前端先做长度提示，但最终以后端校验为准。
- 确认前明确提示：卡密属于敏感信息，导入后查看和复制会被审计。

### 9.4 公开提取页面

公开提取页直接按通用凭证提取页建设：

- 支持单个或多个凭证文本。
- 根据分类展示“CDK”“卡密”“兑换码”等 copy label。
- 提供单个复制和全部复制。
- 展示剩余访问次数、过期时间、批次名称。
- 对 `TEXT_SECRET` 展示“已提取”语义，对 `POINTS_REDEEM` 展示“复制后到平台兑换”语义。
- 保持与现有 React 后台视觉风格统一，页面应简洁可信，不做营销页。

## 10. 权限设计

建议新增权限 action：

| 权限 | 说明 |
| --- | --- |
| `credential:category:view` | 查看分类 |
| `credential:category:manage` | 管理分类 |
| `credential:batch:view` | 查看批次 |
| `credential:batch:create` | 创建生成批次 |
| `credential:batch:import` | 导入卡密批次 |
| `credential:batch:disable` | 停用批次 |
| `credential:item:view` | 查看明细 |
| `credential:item:reveal` | 查看明文 |
| `credential:item:disable` | 停用明细 |
| `credential:extract-link:view` | 查看提取链接 |
| `credential:extract-link:create` | 生成提取链接 |
| `credential:extract-link:copy` | 复制提取 URL |
| `credential:extract-link:disable` | 停用提取链接 |
| `credential:extract-link:extend` | 延期提取链接 |
| `credential:extract-link:reissue` | 补发提取链接 |
| `credential:access-record:view` | 查看访问记录 |
| `credential:import-task:view` | 查看导入任务 |

权限只初始化 `credential:*`，不再新增 `cdk:*` 管理权限。

## 11. 安全设计

1. 凭证明文使用 AES-GCM 加密保存，查询和去重使用 HMAC，不允许明文进日志。
2. token 查询使用 HMAC hash；如保存 `encrypted_token`，必须使用独立密钥，不复用凭证明文 HMAC pepper。
3. 管理员查看明文、复制 URL、导出链接 CSV、停用或补发链接都写审计日志。
4. 公开提取接口按 token、IP、指纹限流，避免链接被撞库或刷次数。
5. 访问记录保存 IP、UA 摘要、浏览器指纹和设备快照，设备快照只保存排查必要字段。
6. 卡密导入原文不落库，只保存导入摘要和脱敏预览。
7. 链接访问次数只在成功返回明文后消耗，失败访问记录审计但不扣次数。
8. 文本卡密默认绑定链接后不再参与其他链接生成，补发必须先停用旧链接并重新绑定。
9. 管理端批量复制和导出需要二次确认，并限制单次最大数量。
10. 环境变量缺失时涉及生成、导入、提取、复制 URL 的接口必须拒绝执行。

新增环境变量建议：

```bash
export CREDENTIAL_SECRET_PEPPER='replace-with-at-least-32-bytes-random-secret'
export CREDENTIAL_SECRET_ENCRYPTION_KEY='replace-with-32-bytes-aes-key'
export CREDENTIAL_EXTRACT_TOKEN_SECRET='replace-with-at-least-32-bytes-random-secret'
export CREDENTIAL_EXTRACT_TOKEN_ENCRYPTION_KEY='replace-with-32-bytes-aes-key'
export CREDENTIAL_EXTRACT_PUBLIC_BASE_URL='http://localhost:5173'
export CREDENTIAL_IMPORT_MAX_ITEMS=10000
export CREDENTIAL_IMPORT_MAX_TEXT_BYTES=1048576
export CREDENTIAL_EXTRACT_MAX_ITEMS_PER_LINK=50
export CREDENTIAL_EXTRACT_MAX_ACCESS_COUNT=100
export CREDENTIAL_EXTRACT_MAX_EXPIRE_DAYS=30
```

## 12. 绿地实施策略

本项目按新开发项目处理，直接建设 `credential_*` 表、`credential` 后端包和 `/system/credentials/*` 前端页面，不设计旧表兼容、历史数据回填、旧路由跳转或灰度迁移。

实施原则：

1. 数据库只创建通用凭证表，不再创建 `cdk_batch`、`cdk_code`、`cdk_extract_link` 等 CDK 专用表。
2. 管理端 API 统一使用 `/api/admin/credentials/*`。
3. 公开提取 API 统一使用 `/api/public/credentials/extract/{token}`。
4. 用户兑换 API 统一使用 `/api/credentials/redeem`，通过履约类型执行积分入账。
5. 前端菜单统一使用“凭证中心”，CDK 只是分类和页面文案，不作为模块边界。
6. SQL 从一个完整初始化脚本开始，权限、菜单和默认分类随脚本一次性初始化。

## 13. 开发计划

### 阶段一：设计落地和数据库基础

后端：

1. 新增 `V33__init_credential_schema.sql`，创建分类、批次、明细、链接、链接明细、访问记录、导入任务、兑换记录和操作审计表。
2. 新增 `V34__seed_credential_permissions.sql`，初始化凭证中心菜单、权限和默认分类。
3. 新增 `CredentialProperties`，映射密钥、导入上限、提取上限等配置。
4. 新增状态枚举、错误码和常量。
5. 执行 SQL 到本地 `spring_admin` 数据库。

前端：

1. 新增凭证中心路由、菜单常量和权限映射。
2. 新增基础类型 `CredentialCategory`、`CredentialBatch`、`CredentialItem`、`CredentialExtractLink`。

验收：

- 本地数据库存在新表、索引和权限。
- 默认分类、菜单和权限初始化完整。
- `mvn test` 和前端构建通过。

### 阶段二：提取链接管理页

后端：

1. 实现 `CredentialExtractLinkService` 的分页、详情、明细、访问记录、复制 URL、停用、延期、补发。
2. 增加 `encrypted_token` 复制能力和审计。
3. 实现批次批量生成链接和单凭证明细生成链接。
4. 完成访问次数并发测试，确保成功访问才扣次数。

前端：

1. 新增 `/system/credentials/extract-links` 页面。
2. 支持筛选、分页、复制 URL、停用、延期、补发。
3. 详情抽屉展示凭证明细和访问记录。
4. 批次详情和明细页生成链接后跳转或提示可在链接管理页查看。

验收：

- 生成后的链接能在全局列表检索。
- 管理员能复制、停用、查看访问记录。
- token 不可复制模式下能通过补发解决运营问题。

### 阶段三：卡密导入和批次管理

后端：

1. 实现分类列表和分类配置接口。
2. 实现文本卡密导入预览和确认导入。
3. 实现导入任务记录、错误摘要和去重检查。
4. 支持导入成功后按 `itemsPerLink` 批量生成链接。

前端：

1. 改造批次页为凭证批次页，支持分类筛选。
2. 新增卡密导入向导。
3. 解析预览展示脱敏卡密、重复行和错误行。
4. 导入完成后提供“生成提取链接”快捷动作。

验收：

- 粘贴 1000 条换行卡密可成功预览、去重和导入。
- 自定义分隔符能正确解析。
- 重复卡密不会重复入库。
- 导入原文不写数据库和日志。

### 阶段四：公开提取页通用化

后端：

1. 实现 `/api/public/credentials/extract/{token}`。
2. 支持 `POINTS_REDEEM` 和 `TEXT_SECRET` 两种公开展示模型。
3. 文本卡密首次成功提取后更新明细状态为 `extracted`。
4. 访问审计写入新表。

前端：

1. 新增 `/credentials/extract/:token`。
2. 页面支持多卡密、一键复制全部、分类化文案和异常状态。

验收：

- 单链接多卡密展示正常。
- 复制全部格式清晰，移动端无布局重叠。
- 过期、停用、次数用尽均有明确提示。

### 阶段五：积分兑换履约

后端：

1. 实现 `/api/credentials/redeem`。
2. 按 `POINTS_REDEEM` 分类查询 `credential_item`，校验有效期、状态、校验位和限流。
3. 复用积分账户、账本、充值流水或权益履约能力完成入账。
4. 新增 `credential_redeem_record`，记录用户、凭证、积分数量、幂等键、IP 和设备信息。
5. 更新对账、风险告警和审计字段。

前端：

1. 用户侧提供“CDK 兑换”入口，调用 `/api/credentials/redeem`。
2. 管理端兑换记录并入凭证兑换记录页。
3. README 更新新模块说明。

验收：

- 系统生成的积分 CDK 可以兑换积分。
- 同一 CDK 并发兑换只有一次成功。
- 兑换记录、积分账本和凭证状态一致。

### 阶段六：清理和强化

1. 补充批量导出链接 CSV。
2. 增加导入任务异步化，支持大批量卡密。
3. 增加管理员操作审计报表。
4. 增加提取链接异常访问风险告警。
5. 增加分类级提取策略配置，例如是否允许公开提取、是否首次提取消费、是否允许重复复制 URL。

## 14. 测试计划

后端单元测试：

- 分隔符解析：换行、逗号、分号、Tab、自定义分隔符。
- 去重策略：批内重复、全库重复、大小写敏感配置。
- 密钥缺失：生成、导入、提取、复制 URL 拒绝执行。
- 链接次数：成功扣次数，失败不扣次数，并发下不超发。
- 文本卡密状态：生成链接后 `linked`，首次成功提取后 `extracted`。
- 积分 CDK 状态：提取不兑换，兑换后 `redeemed`。

后端集成测试：

- 默认分类、菜单、权限和环境变量校验。
- 批次批量生成多凭证链接。
- 公开提取多卡密。
- 管理端复制 URL、停用、延期、补发。
- 访问记录 IP、UA、指纹、设备快照落库。

前端测试：

- 批次列表、明细列表、链接列表筛选和分页。
- 卡密导入向导在 375px、768px、1440px 下无重叠。
- 提取页多卡密复制全部。
- 链接管理页抽屉信息完整。

## 15. 风险和处理

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| 保存 token 密文扩大泄露面 | 管理员可重复复制 URL | 独立密钥加密、权限控制、复制审计、可配置关闭 |
| 文本卡密明文泄露 | 外部卡密可能具有现金价值 | AES-GCM 加密、HMAC 查询、日志脱敏、查看审计 |
| 同一卡密进入多个链接 | 用户重复领取 | 文本卡密生成链接时默认置为 `linked`，只选未绑定明细 |
| 访问次数语义误解 | 以为 3 次代表 3 份卡密 | UI 明确区分“链接可打开次数”和“每个链接包含数量” |
| 自定义分隔符解析异常 | 导入结果不可控 | 第一版仅固定字符串分隔符，限制长度和文本大小 |
| 大批量导入阻塞请求 | 管理端超时 | 第一版限制数量，后续导入任务异步化 |

## 16. 优先级建议

新项目可以直接按完整凭证中心建设，但开发顺序仍建议先打通最小闭环：

1. 先完成 `credential_*` 数据库、默认分类、权限和菜单。
2. 完成文本卡密导入、凭证明细、批次生成提取链接和链接管理页。
3. 完成公开提取页，验证多卡密复制、访问次数、设备审计和补发链路。
4. 完成积分 CDK 生成和 `/api/credentials/redeem`，接入积分账本。
5. 最后补齐异步导入、报表、风险告警和更细的分类策略。

该顺序能优先验证凭证中心的核心抽象：同一套批次、明细、链接和审计能力同时服务文本卡密提取和积分 CDK 兑换。
