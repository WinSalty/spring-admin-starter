# CDK 与积分模块功能测试代码评审修复报告

创建日期：2026-04-25

author：sunshengxian

## 1. 评审范围

本次评审依据 `resources/doc/cdk-points-module-development-plan.md` 中的后续测试计划，重点检查后端 CDK、积分、权益兑换、在线充值和补偿任务相关功能测试代码。

评审覆盖模块：

- `com.winsalty.quickstart.cdk`
- `com.winsalty.quickstart.points`
- `com.winsalty.quickstart.benefit`
- `com.winsalty.quickstart.trade`

## 2. 发现问题

| 序号 | 问题 | 影响 | 修复 |
| --- | --- | --- | --- |
| 1 | `CdkPointsDevIntegrationTest` 仍调用旧版 `exportBatch(Long)` 和 `CdkExportVo#getCodes()` | 当前服务已改为带导出密码的加密 ZIP 导出，导致 `mvn test` 在测试编译阶段失败 | 改为调用 `exportBatch(Long, CdkExportRequest)`，测试内解析 ZIP manifest 并用 PBKDF2 + AES-GCM 解密获取 CDK 明文 |
| 2 | 常规单元测试未覆盖 CDK 高价值双人复核和加密导出 | 双人复核、导出审计、明文不直出等安全链路缺少回归保护 | 新增 `CdkServiceImplTest` |
| 3 | 常规单元测试未覆盖在线充值重复成功回调 | 支付渠道重复推送可能造成重复入账风险，缺少幂等测试 | 新增 `OnlineRechargeServiceImplTest` |
| 4 | 常规单元测试未覆盖权益兑换冻结确认链路 | “冻结积分-发放权益-确认扣减-outbox”核心链路缺少回归保护 | 新增 `BenefitExchangeServiceImplTest` |
| 5 | 常规单元测试未覆盖过期冻结单补偿 | 冻结单自动取消任务缺少命令参数和幂等键校验 | 新增 `PointCompensationServiceImplTest` |

## 3. 本次新增测试

| 测试类 | 覆盖点 |
| --- | --- |
| `CdkServiceImplTest` | 高价值批次首次审批进入二次复核、同一审批人不能二次复核、加密导出返回 ZIP 且消费 Redis 明文缓存 |
| `OnlineRechargeServiceImplTest` | 回调 HMAC 验签、成功回调入账、重复成功回调不重复调用积分入账 |
| `BenefitExchangeServiceImplTest` | 权益兑换成功链路、库存不足时不冻结积分 |
| `PointCompensationServiceImplTest` | 扫描过期冻结单并使用稳定幂等键调用取消冻结 |
| `CdkPointsDevIntegrationTest` | 适配新版加密导出，保留开发环境完整 CDK 兑换链路测试能力 |

## 4. 执行结果

已执行：

```bash
mvn -q -f /Users/salty/codeProject/ai/spring-admin-starter/pom.xml test
```

结果：通过。

## 5. 后续建议

目标环境联调仍需在真实 MySQL、Redis 和外部配置下执行：

```bash
RUN_DEV_INTEGRATION_TESTS=true mvn -q -f /Users/salty/codeProject/ai/spring-admin-starter/pom.xml -Dtest=CdkPointsDevIntegrationTest test
```

执行前需确保目标环境已完成 `V21` 至 `V26` 数据库迁移，并显式配置 `CDK_PEPPER`、Redis 和测试账号权限。
