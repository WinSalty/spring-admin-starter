package com.winsalty.quickstart.points.service.impl;

import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointFreezeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.service.PointCompensationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 积分补偿服务实现。
 * 扫描并取消过期冻结单，保证二阶段扣减失败或超时时积分可自动释放。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class PointCompensationServiceImpl implements PointCompensationService {

    private static final Logger log = LoggerFactory.getLogger(PointCompensationServiceImpl.class);
    private static final String EXPIRED_FREEZE_IDEMPOTENCY_PREFIX = "freeze-expired:";
    private static final String EXPIRED_FREEZE_REMARK = "冻结单过期自动取消";
    private static final String SYSTEM_OPERATOR_ID = "system";

    private final PointFreezeOrderMapper pointFreezeOrderMapper;
    private final PointAccountService pointAccountService;
    private final PointsProperties pointsProperties;

    public PointCompensationServiceImpl(PointFreezeOrderMapper pointFreezeOrderMapper,
                                        PointAccountService pointAccountService,
                                        PointsProperties pointsProperties) {
        this.pointFreezeOrderMapper = pointFreezeOrderMapper;
        this.pointAccountService = pointAccountService;
        this.pointsProperties = pointsProperties;
    }

    @Override
    public int cancelExpiredFreezes() {
        List<PointFreezeOrderEntity> orders = pointFreezeOrderMapper.findExpiredFrozen(pointsProperties.getFreezeCompensationBatchSize());
        int processed = 0;
        for (PointFreezeOrderEntity order : orders) {
            PointChangeCommand command = new PointChangeCommand();
            // 使用冻结单号构造幂等键，补偿任务重复扫描同一冻结单也不会重复返还积分。
            command.setUserId(order.getUserId());
            command.setAmount(order.getAmount());
            command.setBizType(order.getBizType());
            command.setBizNo(order.getBizNo());
            command.setIdempotencyKey(EXPIRED_FREEZE_IDEMPOTENCY_PREFIX + order.getFreezeNo());
            command.setOperatorType(PointsConstants.OPERATOR_TYPE_SYSTEM);
            command.setOperatorId(SYSTEM_OPERATOR_ID);
            command.setRemark(EXPIRED_FREEZE_REMARK);
            // 取消冻结复用账务服务的行锁、余额变更和流水写入，不在补偿服务中直接改余额。
            pointAccountService.cancelFreeze(order.getFreezeNo(), command);
            processed++;
            log.info("expired point freeze cancelled, freezeNo={}, userId={}, amount={}",
                    order.getFreezeNo(), order.getUserId(), order.getAmount());
        }
        return processed;
    }
}
