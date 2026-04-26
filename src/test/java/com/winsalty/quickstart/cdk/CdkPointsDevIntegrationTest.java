package com.winsalty.quickstart.cdk;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRequest;
import com.winsalty.quickstart.cdk.service.CdkService;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkExportVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemResultVo;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import com.winsalty.quickstart.points.vo.PointReconciliationVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CDK 与积分模块开发环境集成测试。
 * 使用本地 MySQL 和 Redis 验证 CDK 兑换、幂等、并发抢兑、余额不足和对账差异不漂移。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DEV_INTEGRATION_TESTS", matches = "true")
@TestPropertySource(properties = {
        "app.cdk.pepper=0123456789abcdef0123456789abcdef",
        "app.cdk.redeem-user-limit=100000",
        "app.cdk.redeem-ip-limit=100000",
        "app.cdk.redeem-failure-limit=100000",
        "spring.quartz.auto-startup=false"
})
class CdkPointsDevIntegrationTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long ADMIN_USER_ID = 1L;
    private static final long TEST_USER_BASE = 920260424000L;
    private static final long TEST_POINTS = 100L;
    private static final long INITIAL_POINTS = 50L;
    private static final long TOO_MANY_POINTS = 80L;
    private static final int SINGLE_CDK_COUNT = 1;
    private static final int THREAD_POOL_SIZE = 2;
    private static final int CONCURRENT_REDEEM_THREADS = 2;
    private static final int THREAD_WAIT_SECONDS = 10;
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ADMIN_USERNAME = "integration-admin";
    private static final String USERNAME_PREFIX = "integration-user-";
    private static final String BATCH_NAME_PREFIX = "integration-cdk-";
    private static final String SESSION_ID = "integration-session";
    private static final String TEST_CLIENT_IP = "127.0.0.1";
    private static final String TEST_USER_AGENT = "cdk-points-integration-test";
    private static final AtomicLong USER_SEQUENCE = new AtomicLong(TEST_USER_BASE);

    @Autowired
    private CdkService cdkService;

    @Autowired
    private PointAccountService pointAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> createdBatchIds = new ArrayList<Long>();
    private final List<Long> touchedUserIds = new ArrayList<Long>();

    /**
     * 清理本测试创建的 CDK 和积分数据，避免污染开发环境。
     */
    @AfterEach
    void cleanup() {
        AuthContext.clear();
        for (Long userId : touchedUserIds) {
            jdbcTemplate.update("DELETE FROM point_adjustment_order WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM point_freeze_order WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM point_recharge_order WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM point_ledger WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM point_account WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM cdk_redeem_record WHERE user_id = ?", userId);
        }
        for (Long batchId : createdBatchIds) {
            jdbcTemplate.update("DELETE FROM cdk_export_audit WHERE batch_id = ?", batchId);
            jdbcTemplate.update("DELETE FROM cdk_redeem_record WHERE batch_id = ?", batchId);
            jdbcTemplate.update("DELETE FROM cdk_code WHERE batch_id = ?", batchId);
            jdbcTemplate.update("DELETE FROM cdk_batch WHERE id = ?", batchId);
        }
    }

    /**
     * 验证同一幂等键重复兑换返回首次结果，账户余额和对账差异保持一致。
     */
    @Test
    void redeemSameIdempotencyKeyReturnsOriginalResult() {
        PointReconciliationVo before = pointAccountService.reconcile();
        Long userId = nextUserId();
        String code = createActiveCode(TEST_POINTS);
        String idempotencyKey = "redeem-idem-" + UUID.randomUUID();

        CdkRedeemResultVo first = redeem(userId, code, idempotencyKey);
        CdkRedeemResultVo second = redeem(userId, code, idempotencyKey);
        PointAccountVo account = pointAccountService.getOrCreateAccount(userId);

        assertEquals(first.getRedeemNo(), second.getRedeemNo());
        assertEquals(CdkConstants.RECORD_STATUS_SUCCESS, second.getStatus());
        assertEquals(TEST_POINTS, account.getAvailablePoints());
        assertEquals(0L, account.getFrozenPoints());
        assertReconciliationUnchanged(before, pointAccountService.reconcile());
    }

    /**
     * 验证同一 CDK 并发兑换时只有一个请求成功，避免重复入账。
     */
    @Test
    void concurrentRedeemSameCodeOnlySucceedsOnce() throws Exception {
        PointReconciliationVo before = pointAccountService.reconcile();
        Long userId = nextUserId();
        String code = createActiveCode(TEST_POINTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

        for (int index = 0; index < CONCURRENT_REDEEM_THREADS; index++) {
            final String idempotencyKey = "redeem-concurrent-" + index + "-" + UUID.randomUUID();
            futures.add(executorService.submit(newRedeemTask(startLatch, userId, code, idempotencyKey)));
        }
        startLatch.countDown();
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(THREAD_WAIT_SECONDS, TimeUnit.SECONDS));

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get())) {
                successCount++;
            }
        }
        PointAccountVo account = pointAccountService.getOrCreateAccount(userId);

        assertEquals(1, successCount);
        assertEquals(TEST_POINTS, account.getAvailablePoints());
        assertReconciliationUnchanged(before, pointAccountService.reconcile());
    }

    /**
     * 验证余额不足扣减失败不产生负数，也不改变对账差异。
     */
    @Test
    void debitRejectsInsufficientBalanceAndKeepsReconciliationStable() {
        PointReconciliationVo before = pointAccountService.reconcile();
        Long userId = nextUserId();
        pointAccountService.credit(pointCommand(userId, INITIAL_POINTS, "credit-" + UUID.randomUUID()));

        assertThrows(BusinessException.class, () ->
                pointAccountService.debit(pointCommand(userId, TOO_MANY_POINTS, "debit-" + UUID.randomUUID())));
        PointAccountVo account = pointAccountService.getOrCreateAccount(userId);

        assertEquals(INITIAL_POINTS, account.getAvailablePoints());
        assertEquals(0L, account.getFrozenPoints());
        assertReconciliationUnchanged(before, pointAccountService.reconcile());
    }

    /**
     * 构建并发兑换任务，业务异常视为兑换失败。
     */
    private Callable<Boolean> newRedeemTask(CountDownLatch startLatch,
                                            Long userId,
                                            String code,
                                            String idempotencyKey) {
        return () -> {
            startLatch.await();
            try {
                CdkRedeemResultVo result = redeem(userId, code, idempotencyKey);
                return CdkConstants.RECORD_STATUS_SUCCESS.equals(result.getStatus());
            } catch (BusinessException exception) {
                return false;
            } finally {
                AuthContext.clear();
            }
        };
    }

    /**
     * 创建可兑换的测试 CDK，并返回管理端导出的明文码。
     */
    private String createActiveCode(Long points) {
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));
        CdkBatchVo batch = cdkService.createBatch(batchRequest(points));
        Long batchId = Long.valueOf(batch.getId());
        createdBatchIds.add(batchId);
        CdkExportVo exportVo = cdkService.exportBatch(batchId);
        String content = exportVo.getContent();
        List<String> codes = new ArrayList<String>();
        for (String code : content.split("\n")) {
            if (code.trim().length() > 0) {
                codes.add(code.trim());
            }
        }
        assertFalse(codes.isEmpty());
        return codes.get(0);
    }

    /**
     * 构建测试 CDK 批次请求，使用当前时间前后一天作为有效期。
     */
    private CdkBatchCreateRequest batchRequest(Long points) {
        CdkBatchCreateRequest request = new CdkBatchCreateRequest();
        request.setBatchName(BATCH_NAME_PREFIX + UUID.randomUUID());
        request.setBenefitType(CdkConstants.BENEFIT_TYPE_POINTS);
        request.setPoints(points);
        request.setTotalCount(SINGLE_CDK_COUNT);
        request.setValidFrom(LocalDateTime.now().minusDays(1L).format(DATE_TIME_FORMATTER));
        request.setValidTo(LocalDateTime.now().plusDays(1L).format(DATE_TIME_FORMATTER));
        request.setRiskLevel(CdkConstants.RISK_LEVEL_NORMAL);
        request.setRemark("integration test");
        return request;
    }

    /**
     * 使用指定用户上下文兑换 CDK。
     */
    private CdkRedeemResultVo redeem(Long userId, String code, String idempotencyKey) {
        AuthContext.set(new AuthUser(userId, USERNAME_PREFIX + userId, ROLE_USER, SESSION_ID));
        CdkRedeemRequest request = new CdkRedeemRequest();
        request.setCdk(code);
        request.setIdempotencyKey(idempotencyKey);
        return cdkService.redeem(request, mockRequest());
    }

    /**
     * 构建包含 IP 和 UA 的测试请求。
     */
    private MockHttpServletRequest mockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(TEST_CLIENT_IP);
        request.addHeader("User-Agent", TEST_USER_AGENT);
        return request;
    }

    /**
     * 构建积分账务变更命令。
     */
    private PointChangeCommand pointCommand(Long userId, Long amount, String idempotencyKey) {
        PointChangeCommand command = new PointChangeCommand();
        command.setUserId(userId);
        command.setAmount(amount);
        command.setBizType(PointsConstants.BIZ_TYPE_ADMIN_ADJUST);
        command.setBizNo(idempotencyKey);
        command.setIdempotencyKey(idempotencyKey);
        command.setOperatorType(PointsConstants.OPERATOR_TYPE_ADMIN);
        command.setOperatorId(ADMIN_USERNAME);
        command.setRemark("integration test");
        return command;
    }

    /**
     * 分配测试用户 ID，并记录到清理列表。
     */
    private Long nextUserId() {
        Long userId = USER_SEQUENCE.incrementAndGet();
        touchedUserIds.add(userId);
        return userId;
    }

    /**
     * 校验测试前后全局对账差异没有变化。
     */
    private void assertReconciliationUnchanged(PointReconciliationVo before, PointReconciliationVo after) {
        assertEquals(before.getTotalAvailableDiff(), after.getTotalAvailableDiff());
        assertEquals(before.getTotalFrozenDiff(), after.getTotalFrozenDiff());
    }
}
