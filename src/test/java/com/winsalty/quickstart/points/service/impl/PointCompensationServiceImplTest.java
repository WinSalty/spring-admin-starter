package com.winsalty.quickstart.points.service.impl;

import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointFreezeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 积分冻结补偿服务测试。
 * 覆盖过期冻结单扫描和自动取消命令构建，保证补偿任务使用稳定幂等键释放积分。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class PointCompensationServiceImplTest {

    private static final String FREEZE_NO_ONE = "PF202604250001";
    private static final String FREEZE_NO_TWO = "PF202604250002";
    private static final long USER_ID = 1001L;
    private static final long AMOUNT = 50L;
    private static final int BATCH_SIZE = 20;

    @Test
    void cancelExpiredFreezesShouldCancelEachFrozenOrderWithStableIdempotencyKey() {
        PointFreezeOrderMapper freezeOrderMapper = mock(PointFreezeOrderMapper.class);
        PointAccountService pointAccountService = mock(PointAccountService.class);
        PointsProperties properties = new PointsProperties();
        properties.setFreezeCompensationBatchSize(BATCH_SIZE);
        PointCompensationServiceImpl service = new PointCompensationServiceImpl(freezeOrderMapper, pointAccountService, properties);
        when(freezeOrderMapper.findExpiredFrozen(BATCH_SIZE)).thenReturn(Arrays.asList(freezeOrder(FREEZE_NO_ONE), freezeOrder(FREEZE_NO_TWO)));

        int processed = service.cancelExpiredFreezes();

        assertEquals(2, processed);
        ArgumentCaptor<PointChangeCommand> commandCaptor = ArgumentCaptor.forClass(PointChangeCommand.class);
        verify(pointAccountService).cancelFreeze(eq(FREEZE_NO_ONE), commandCaptor.capture());
        PointChangeCommand firstCommand = commandCaptor.getValue();
        assertEquals(USER_ID, firstCommand.getUserId());
        assertEquals(AMOUNT, firstCommand.getAmount());
        assertEquals(PointsConstants.BIZ_TYPE_BENEFIT_EXCHANGE, firstCommand.getBizType());
        assertEquals("freeze-expired:" + FREEZE_NO_ONE, firstCommand.getIdempotencyKey());
        verify(pointAccountService).cancelFreeze(eq(FREEZE_NO_TWO), commandCaptor.capture());
    }

    private PointFreezeOrderEntity freezeOrder(String freezeNo) {
        PointFreezeOrderEntity entity = new PointFreezeOrderEntity();
        entity.setFreezeNo(freezeNo);
        entity.setUserId(USER_ID);
        entity.setAmount(AMOUNT);
        entity.setBizType(PointsConstants.BIZ_TYPE_BENEFIT_EXCHANGE);
        entity.setBizNo("BE202604250001");
        entity.setStatus(PointsConstants.FREEZE_STATUS_FROZEN);
        return entity;
    }
}
