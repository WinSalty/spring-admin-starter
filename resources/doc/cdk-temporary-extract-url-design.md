# CDK 临时提取链接页面设计方案

创建日期：2026-04-30  
author：sunshengxian

## 1. 背景与目标

当前 `spring-admin-starter` 已在 CDK 模块中支持批次生成、CDK 明细查询、明文 AES-GCM 加密存储、管理员复制 CDK、单码失效/启用和兑换记录查询。`react-admin-starter` 已提供 `/system/cdk/codes` 的 CDK 明细管理页，页面风格基于 Ant Design、`#f5f7fb` 页面底色、8px 卡片圆角和统一的后台管理布局。

本次需求是在 CDK 明细中新增“生成 CDK 提取临时 URL”的能力。管理员可以为单个 CDK 生成一个可配置访问次数的临时提取链接，外部访问者打开链接后进入与现有前端 UI 风格统一的精美提取页面，并可一键复制 CDK。后端需要记录每次访问提取 URL 的设备信息、IP、浏览器指纹等审计数据，便于追踪 CDK 分发过程。

核心目标如下：

| 目标 | 说明 |
| --- | --- |
| 临时链接 | 支持按单个 CDK 生成随机 token URL，token 不可枚举、不暴露 codeId |
| 次数控制 | 管理员创建时可自定义最大访问次数，后端原子扣减或累加访问次数 |
| 有效期控制 | 建议同步支持过期时间，避免长期有效链接扩大泄露风险 |
| 公开提取页 | 不要求登录，访问 token 后展示 CDK 信息和复制按钮 |
| 统一 UI | 页面视觉沿用现有前端主题、品牌、间距、按钮和状态反馈 |
| 访问审计 | 记录访问 IP、UA、浏览器指纹、设备摘要、结果、失败原因、traceId |
| 安全兜底 | token 只保存 hash，CDK 明文仍由既有 AES-GCM 解密能力按需返回 |

## 2. 当前模块现状

### 2.1 后端现状

| 能力 | 当前实现 |
| --- | --- |
| 管理接口 | `AdminCdkController` 挂载在 `/api/admin/cdk`，仅 `ADMIN` 可访问 |
| 明细列表 | `GET /api/admin/cdk/codes` 返回 `CdkCodeVo.cdk`，由 `CdkServiceImpl#listCodes` 解密 |
| 状态变更 | `POST /api/admin/cdk/codes/{id}/status` 支持 active/disabled |
| 明文保护 | `cdk_code.encrypted_code` 保存 AES-GCM 密文，`code_hash` 保存 HMAC |
| 访问审计基础 | 已有 `@AuditLog`、`IpUtils`、`TraceIdFilter`、`operation_log` 和 `risk_alert` |
| 兑换审计 | `cdk_redeem_record` 记录兑换 IP、UA hash、traceId |

### 2.2 前端现状

| 能力 | 当前实现 |
| --- | --- |
| CDK 明细页 | `src/pages/cdk/CdkCodePage.tsx` |
| CDK API | `src/services/cdk.ts` |
| CDK 类型 | `src/types/cdk.ts` |
| 复制能力 | `src/utils/clipboard.ts` 已封装 Clipboard API 和 textarea 降级 |
| 路由 | `src/routes/index.tsx` 使用 `createBrowserRouter` |
| 主题 | `src/theme/index.ts` 主色 `#1677ff`，圆角 `6/8px` |
| 管理组件 | `ListSearchCard`、`ListTableCard`、`SubmitModalForm` 等 |

## 3. 总体方案

新增“CDK 提取链接”子能力，拆分为管理端生成/管理接口、公开提取接口、访问审计、前端管理入口、公开提取页面五部分。

推荐链路：

1. 管理员在 `/system/cdk/codes` 的某一条 CDK 明细中点击“提取链接”。
2. 前端弹出生成表单，填写最大访问次数、过期时间、备注。
3. 后端校验 CDK 状态、批次状态、有效期，生成 256 bit 随机 token。
4. 数据库只保存 `SHA-256(token + serverSecret)` 后的 token hash，返回完整 URL 给管理员。
5. 管理员复制 URL 分发给外部访问者。
6. 外部访问者打开 `/cdk/extract/{token}`，前端采集基础指纹并调用公开接口。
7. 后端原子校验 token、有效期、访问次数、CDK 可提取状态，记录访问流水。
8. 前端展示 CDK、批次名称、有效期、状态提示和“一键复制”按钮。

## 4. 数据库设计

新增 SQL 脚本建议命名：

`resources/sql/V31__add_cdk_extract_link_schema.sql`

### 4.1 CDK 提取链接表 `cdk_extract_link`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| link_no | VARCHAR(64) | 链接编号，唯一 |
| code_id | BIGINT | CDK ID |
| batch_id | BIGINT | 批次 ID，冗余便于查询 |
| token_hash | VARCHAR(128) | token 摘要，唯一索引 |
| max_access_count | INT | 最大访问次数 |
| accessed_count | INT | 已访问次数 |
| expire_at | DATETIME | 链接过期时间 |
| status | VARCHAR(32) | active、expired、disabled、exhausted |
| created_by | VARCHAR(64) | 创建人 |
| disabled_by | VARCHAR(64) | 停用人 |
| disabled_at | DATETIME | 停用时间 |
| remark | VARCHAR(512) | 备注 |
| last_accessed_at | DATETIME | 最近访问时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

索引建议：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `uk_cdk_extract_link_no` | `link_no` | 管理端定位 |
| `uk_cdk_extract_token_hash` | `token_hash` | 公开访问 token 查询 |
| `idx_cdk_extract_code_status` | `code_id,status` | 明细页查看链接 |
| `idx_cdk_extract_batch_created` | `batch_id,created_at` | 后续批次审计 |

### 4.2 CDK 提取访问记录表 `cdk_extract_access_record`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| access_no | VARCHAR(64) | 访问编号，唯一 |
| link_id | BIGINT | 提取链接 ID |
| code_id | BIGINT | CDK ID |
| batch_id | BIGINT | 批次 ID |
| result | VARCHAR(32) | success、failed |
| failure_code | VARCHAR(64) | 失败码 |
| failure_message | VARCHAR(256) | 失败原因 |
| client_ip | VARCHAR(64) | 客户端 IP |
| user_agent_hash | VARCHAR(128) | UA 摘要 |
| browser_fingerprint | VARCHAR(128) | 前端浏览器指纹摘要 |
| device_snapshot | JSON | 设备信息快照 |
| referer | VARCHAR(512) | 来源页面 |
| trace_id | VARCHAR(64) | 链路 ID |
| created_at | DATETIME | 创建时间 |

`device_snapshot` 建议保存以下字段：

```json
{
  "userAgent": "hash 或脱敏值",
  "platform": "MacIntel",
  "language": "zh-CN",
  "timezone": "Asia/Shanghai",
  "screen": "1440x900",
  "colorDepth": 30,
  "deviceMemory": 8,
  "hardwareConcurrency": 10,
  "touchPoints": 0
}
```

出于隐私和合规考虑，不建议保存完整 `User-Agent` 原文。需要排查时保存 UA hash、浏览器品牌、系统、设备类别即可；如确需 UA 原文，应增加保留期限和访问权限。

## 5. 后端接口设计

### 5.1 管理端生成提取链接

`POST /api/admin/cdk/codes/{id}/extract-links`

权限：

- 复用 `@PreAuthorize("hasRole('ADMIN')")`
- 按现有权限体系新增按钮权限 `cdk:code:extract-link:create`
- 增加 `@AuditLog(logType = "operation", code = "cdk_extract_link_create", name = "生成CDK提取链接")`

请求：

```json
{
  "maxAccessCount": 3,
  "expireAt": "2026-05-01 23:59:59",
  "remark": "线下活动发放"
}
```

响应：

```json
{
  "code": 0,
  "message": "生成成功",
  "data": {
    "id": "1",
    "linkNo": "CEL20260430120000123456",
    "codeId": "1001",
    "batchId": "88",
    "url": "https://admin.example.com/cdk/extract/AbCd...",
    "maxAccessCount": 3,
    "accessedCount": 0,
    "expireAt": "2026-05-01 23:59:59",
    "status": "active",
    "createdBy": "admin",
    "createdAt": "2026-04-30 12:00:00"
  }
}
```

校验规则：

| 规则 | 说明 |
| --- | --- |
| CDK 必须存在 | 不存在返回 `CDK_CODE_NOT_FOUND` |
| CDK 必须 active | 已兑换、已失效不允许生成 |
| 批次必须 active | 批次作废或不可用不允许生成 |
| 批次有效期必须覆盖当前时间 | 已过期或未生效不允许生成 |
| 最大访问次数范围 | 建议 `1 <= maxAccessCount <= 100` |
| 过期时间范围 | 必须晚于当前时间，最长不超过 30 天 |

### 5.2 管理端查询链接列表

`GET /api/admin/cdk/codes/{id}/extract-links`

用于 CDK 明细抽屉或弹窗内查看历史链接、复制 URL、停用链接。

响应字段包含 `linkNo`、`maxAccessCount`、`accessedCount`、`expireAt`、`status`、`createdBy`、`createdAt`、`lastAccessedAt`、`remark`。历史 URL 不建议再次返回 token 明文；前端只在创建成功后展示完整 URL。若业务要求重复复制，应对 token 进行可恢复加密存储，但安全性低于只展示一次。

### 5.3 管理端停用链接

`POST /api/admin/cdk/extract-links/{id}/disable`

权限：

- `cdk:code:extract-link:disable`
- `@AuditLog(logType = "operation", code = "cdk_extract_link_disable", name = "停用CDK提取链接")`

请求：

```json
{
  "reason": "发放渠道变更"
}
```

### 5.4 管理端访问记录

`GET /api/admin/cdk/extract-links/{id}/access-records`

用于审计查看具体访问设备和结果。

筛选项：

| 参数 | 说明 |
| --- | --- |
| `result` | success、failed |
| `fingerprint` | 浏览器指纹摘要 |
| `pageNo` | 页码 |
| `pageSize` | 每页条数 |

### 5.5 公开提取预览接口

`POST /api/public/cdk/extract/{token}`

公开接口不要求登录。使用 POST 是为了携带浏览器指纹和设备快照，同时避免 URL 被中间缓存误处理。

请求：

```json
{
  "browserFingerprint": "sha256 前端指纹摘要",
  "deviceSnapshot": {
    "platform": "MacIntel",
    "language": "zh-CN",
    "timezone": "Asia/Shanghai",
    "screen": "1440x900",
    "colorDepth": 30,
    "deviceMemory": 8,
    "hardwareConcurrency": 10,
    "touchPoints": 0
  }
}
```

响应：

```json
{
  "code": 0,
  "message": "获取成功",
  "data": {
    "cdk": "ABCD-1234-EF56-7890",
    "batchName": "五一活动积分券",
    "benefitType": "points",
    "benefitText": "1,000 积分",
    "validTo": "2026-05-31 23:59:59",
    "remainingAccessCount": 2,
    "status": "active"
  }
}
```

访问次数口径建议：

- 每次公开接口成功返回 CDK 明文都计为一次访问。
- 同一浏览器重复刷新也消耗次数，满足“自定义访问次数”的直观语义。
- 失败访问记录到 `cdk_extract_access_record`，但不消耗 `accessed_count`。

并发控制：

- 使用 `SELECT ... FOR UPDATE` 锁定 `cdk_extract_link`。
- 校验 `accessed_count < max_access_count` 后再更新 `accessed_count = accessed_count + 1`。
- 达到上限后同步置 `status = 'exhausted'`。

安全响应：

| 场景 | 对外提示 |
| --- | --- |
| token 不存在 | 链接无效或已过期 |
| 已过期 | 链接无效或已过期 |
| 次数耗尽 | 链接访问次数已用完 |
| CDK 不可用 | CDK 当前不可提取 |

## 6. 后端代码结构设计

建议新增或调整以下文件：

| 文件 | 说明 |
| --- | --- |
| `cdk/dto/CdkExtractLinkCreateRequest.java` | 管理端生成链接入参 |
| `cdk/dto/CdkExtractAccessRequest.java` | 公开访问设备信息入参 |
| `cdk/dto/CdkExtractLinkDisableRequest.java` | 停用链接入参 |
| `cdk/dto/CdkExtractAccessRecordListRequest.java` | 访问记录分页入参 |
| `cdk/entity/CdkExtractLinkEntity.java` | 提取链接实体 |
| `cdk/entity/CdkExtractAccessRecordEntity.java` | 访问记录实体 |
| `cdk/mapper/CdkExtractLinkMapper.java` | 链接表 MyBatis Mapper |
| `cdk/mapper/CdkExtractAccessRecordMapper.java` | 访问记录 Mapper |
| `cdk/service/CdkExtractService.java` | 提取链接服务接口 |
| `cdk/service/impl/CdkExtractServiceImpl.java` | 提取链接业务实现 |
| `cdk/controller/AdminCdkExtractController.java` | 管理端链接接口 |
| `cdk/controller/PublicCdkExtractController.java` | 公开提取接口 |
| `cdk/vo/CdkExtractLinkVo.java` | 管理端链接展示对象 |
| `cdk/vo/CdkExtractAccessRecordVo.java` | 访问记录展示对象 |
| `cdk/vo/CdkExtractViewVo.java` | 公开提取页展示对象 |

`CdkExtractServiceImpl` 关键常量建议集中定义，避免魔法值：

| 常量 | 建议值 | 说明 |
| --- | --- | --- |
| `TOKEN_RANDOM_BYTES` | `32` | 256 bit token 熵 |
| `MAX_ACCESS_COUNT_LIMIT` | `100` | 单链接最大访问次数 |
| `MAX_EXPIRE_DAYS` | `30` | 单链接最长有效期 |
| `LINK_NO_PREFIX` | `CEL` | CDK Extract Link |
| `ACCESS_NO_PREFIX` | `CEA` | CDK Extract Access |
| `TOKEN_HASH_ALGORITHM` | `SHA-256` | token 摘要 |
| `TOKEN_CONTEXT` | `:cdk-extract-link:v1` | token hash 上下文 |

配置项建议新增：

```yaml
app:
  cdk:
    extract:
      public-base-url: ${CDK_EXTRACT_PUBLIC_BASE_URL:http://localhost:5173}
      token-secret: ${CDK_EXTRACT_TOKEN_SECRET:}
      max-access-count: ${CDK_EXTRACT_MAX_ACCESS_COUNT:100}
      max-expire-days: ${CDK_EXTRACT_MAX_EXPIRE_DAYS:30}
```

`token-secret` 必须外部注入，长度不少于 32 字节。启动期应 fail-fast 或在生成链接时明确报错。

## 7. 前端设计

### 7.1 管理端 CDK 明细页改造

文件：`react-admin-starter/src/pages/cdk/CdkCodePage.tsx`

新增能力：

| 区域 | 改造 |
| --- | --- |
| 表格操作列 | 增加“提取链接”按钮 |
| 弹窗 | 使用 `Modal` 或现有 `SubmitModalForm` 填写访问次数、过期时间、备注 |
| 成功结果 | 展示只读 URL 输入框和复制按钮 |
| 历史记录 | 可在弹窗中展示最近链接状态、访问次数、过期时间 |
| 权限 | 使用 `<Access action="cdk:code:extract-link:create">` 包裹 |

表单字段：

| 字段 | 控件 | 默认值 |
| --- | --- | --- |
| `maxAccessCount` | `InputNumber` | `1` |
| `expireAt` | `DatePicker showTime` | 当前时间 + 24 小时 |
| `remark` | `Input.TextArea` | 空 |

### 7.2 公开 CDK 提取页

新增页面：

`react-admin-starter/src/pages/cdk/CdkExtractPage.tsx`

新增路由：

`/cdk/extract/:token`

该路由不放入 `BasicLayout`，不需要登录。建议在 `routes/index.tsx` 中作为独立公开路由配置，避免 `GuestGuard` 对已登录管理员产生重定向。

页面结构建议：

| 区域 | 说明 |
| --- | --- |
| 顶部品牌 | 使用 `brand-logo.svg` 和 `React Admin Starter` |
| 主内容 | 左侧展示权益和有效期，右侧展示 CDK 复制区；移动端上下排列 |
| 状态反馈 | loading、成功、失效、次数耗尽、网络失败 |
| 复制按钮 | 使用现有 `copyText`，成功后显示 `message.success` |
| 视觉风格 | 背景 `#f5f7fb`，白色内容卡片 8px 圆角，主按钮 `#1677ff` |

页面不使用营销式 hero，不使用纯装饰渐变背景。建议使用紧凑、可信、清晰的凭证提取界面，重点让用户能快速识别 CDK 并复制。

### 7.3 前端设备指纹采集

新增工具：

`react-admin-starter/src/utils/deviceFingerprint.ts`

建议采集浏览器可稳定读取且不需要额外权限的字段：

| 字段 | 来源 |
| --- | --- |
| `userAgent` | `navigator.userAgent`，前端本地参与 hash，不建议原文传输 |
| `platform` | `navigator.platform` |
| `language` | `navigator.language` |
| `timezone` | `Intl.DateTimeFormat().resolvedOptions().timeZone` |
| `screen` | `window.screen.width/height` |
| `colorDepth` | `window.screen.colorDepth` |
| `deviceMemory` | `navigator.deviceMemory` |
| `hardwareConcurrency` | `navigator.hardwareConcurrency` |
| `touchPoints` | `navigator.maxTouchPoints` |

浏览器指纹摘要生成：

```ts
const fingerprint = await crypto.subtle.digest('SHA-256', encodedStableDeviceString);
```

如果 `crypto.subtle` 不可用，则传空字符串，后端仍按 IP 和 UA hash 记录。

### 7.4 前端 API 与类型

`src/services/cdk.ts` 新增：

| 方法 | 路径 |
| --- | --- |
| `createCdkExtractLink` | `POST /api/admin/cdk/codes/{id}/extract-links` |
| `fetchCdkExtractLinks` | `GET /api/admin/cdk/codes/{id}/extract-links` |
| `disableCdkExtractLink` | `POST /api/admin/cdk/extract-links/{id}/disable` |
| `fetchCdkExtractAccessRecords` | `GET /api/admin/cdk/extract-links/{id}/access-records` |
| `fetchPublicCdkExtract` | `POST /api/public/cdk/extract/{token}` |

`src/types/cdk.ts` 新增：

| 类型 | 说明 |
| --- | --- |
| `CdkExtractLink` | 管理端链接对象 |
| `CdkExtractLinkCreateParams` | 创建入参 |
| `CdkExtractAccessRecord` | 访问记录对象 |
| `CdkExtractView` | 公开页展示对象 |
| `DeviceSnapshot` | 设备快照 |

## 8. 权限与菜单

本需求不需要新增菜单页，只在 `CDK管理` 明细页增加按钮。SQL 权限脚本建议追加：

```sql
INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'cdk:code:extract-link:create', '生成CDK提取链接'),
(1, 'cdk:code:extract-link:disable', '停用CDK提取链接'),
(1, 'cdk:code:extract-link:access-record:view', '查看CDK提取访问记录');
```

如果后续需要独立审计页，可再新增隐藏路由或菜单，不建议本次扩大范围。

## 9. 安全与合规设计

### 9.1 token 安全

- token 使用 `SecureRandom` 生成 32 字节随机数，并使用 URL Safe Base64 编码。
- 数据库只保存 token hash，不保存 token 明文。
- token hash 建议使用 `SHA-256(token + tokenSecret + TOKEN_CONTEXT)`。
- 创建成功后只返回一次完整 URL，历史列表不再返回 token。
- 公开接口对所有失败场景做模糊提示，避免枚举 token 状态。

### 9.2 CDK 明文保护

- 公开接口只在 token 校验成功且次数扣减成功后解密 CDK。
- 访问记录和日志不得打印 CDK 明文、token 明文。
- 日志中可记录 `linkNo`、`codeId`、`batchId`、`clientIpDigest`、`fingerprint`。

### 9.3 访问次数与并发

- 成功返回明文才消耗访问次数。
- 使用数据库行锁或条件更新保证并发下不会超发。
- 达到上限时将链接置为 `exhausted`。

### 9.4 IP 与设备信息

- IP 统一使用 `IpUtils.getClientIp`，上线前必须确认反向代理可信链。
- UA 保存 hash，完整设备信息保存结构化快照。
- 浏览器指纹由前端采集并 hash 后传输。
- 访问记录需纳入数据保留策略，建议默认保留 180 天。

### 9.5 风控扩展

本次建议先记录访问数据，不强制拦截。同一链接出现以下情况时可写入 `risk_alert`：

| 风险 | 条件 |
| --- | --- |
| 高频访问 | 1 分钟内失败访问超过阈值 |
| 多设备访问 | 单链接不同指纹数量超过 `maxAccessCount` |
| 多 IP 访问 | 单链接不同 IP 数量异常 |
| 过期后访问 | 过期链接仍被频繁请求 |

## 10. 错误码建议

在 `ErrorCode` 中追加：

| 错误码 | 文案 |
| --- | --- |
| `CDK_EXTRACT_LINK_NOT_FOUND` | CDK 提取链接不存在 |
| `CDK_EXTRACT_LINK_EXPIRED` | CDK 提取链接已过期 |
| `CDK_EXTRACT_LINK_EXHAUSTED` | CDK 提取链接访问次数已用完 |
| `CDK_EXTRACT_LINK_DISABLED` | CDK 提取链接已停用 |
| `CDK_EXTRACT_LINK_LIMIT_INVALID` | CDK 提取链接访问次数不合法 |
| `CDK_EXTRACT_SECRET_MISSING` | CDK 提取链接安全密钥未配置 |

公开接口可统一返回“链接无效或已过期”，管理端接口保留精确错误，便于排查。

## 11. 测试方案

### 11.1 后端单元测试

| 测试 | 重点 |
| --- | --- |
| token 生成 | 长度、随机性、hash 不含明文 |
| 创建链接 | CDK 不存在、非 active、批次不可用、次数越界、过期时间越界 |
| 公开访问 | 成功返回 CDK、次数递增、剩余次数正确 |
| 次数耗尽 | 达到上限后拒绝访问，并置为 exhausted |
| 并发访问 | 多线程访问不会超过 `maxAccessCount` |
| 审计记录 | 成功/失败都写访问记录，不保存明文 |

### 11.2 前端测试

| 测试 | 重点 |
| --- | --- |
| 管理端弹窗 | 表单校验、创建成功展示 URL、复制成功提示 |
| 公开页 | loading、成功、失效、次数耗尽、网络失败状态 |
| 指纹工具 | `crypto.subtle` 可用/不可用的降级 |
| 响应式 | 375px、768px、1440px 下无重叠，CDK 文本可完整展示 |

### 11.3 联调验证

1. 管理员生成访问次数为 1 的链接。
2. 首次访问公开页能看到 CDK 并复制。
3. 刷新后提示次数已用完。
4. 管理端访问记录出现两条记录：一次 success，一次 failed。
5. 访问记录包含 IP、UA hash、浏览器指纹、设备快照和 traceId。
6. 停用链接后公开页不可再提取。

## 12. 实施步骤

建议按以下顺序开发：

1. 新增 `V31__add_cdk_extract_link_schema.sql`，创建链接表、访问记录表和权限 action。
2. 后端新增 DTO、Entity、Mapper、VO、Service 和 Controller。
3. 接入 `CdkProperties` 或新增 `CdkExtractProperties` 配置。
4. 实现管理端创建、列表、停用和访问记录接口。
5. 实现公开提取接口，完成次数控制、CDK 解密、访问审计。
6. 补充后端单元测试和必要的集成测试。
7. 前端新增类型、API、设备指纹工具。
8. 改造 `CdkCodePage`，增加提取链接弹窗和复制能力。
9. 新增 `CdkExtractPage` 和公开路由。
10. 运行后端 `mvn -q test`，前端 `npm run typecheck`、`npm run build`。
11. 用本地浏览器验证公开页 UI 和访问次数行为。
12. 更新 `README.md` 中 CDK 模块说明和新增环境变量。

## 13. 风险与取舍

| 风险 | 方案 |
| --- | --- |
| 链接泄露 | token 高熵、可停用、短有效期、次数限制 |
| 管理端历史 URL 无法重复复制 | 创建后只展示一次 URL，换取更小泄露面 |
| 设备指纹不完全准确 | 指纹仅用于审计辅助，不作为唯一身份依据 |
| 公开接口被刷 | token 不可枚举，失败记录审计；后续可加 IP 限流 |
| CDK 明文再次扩大暴露面 | 成功访问才解密，访问全量落审计，日志禁止明文 |

## 14. 验收标准

| 验收项 | 标准 |
| --- | --- |
| 生成链接 | 管理员可在 CDK 明细中生成自定义访问次数的临时 URL |
| 提取页面 | 外部访问 URL 可看到统一 UI 风格的提取页，并一键复制 CDK |
| 次数控制 | 成功访问次数达到上限后，链接不可继续提取 |
| 访问审计 | 每次访问都记录 IP、UA hash、浏览器指纹、设备快照、结果和 traceId |
| 安全 | 数据库不保存 token 明文，日志不打印 token/CDK 明文 |
| 权限 | 生成、停用、查看访问记录受按钮权限控制 |
| 测试 | 后端测试、前端类型检查和构建通过 |
