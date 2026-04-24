package com.winsalty.quickstart.benefit.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.winsalty.quickstart.benefit.dto.BenefitGrantCommand;
import com.winsalty.quickstart.benefit.dto.BenefitGrantResult;
import com.winsalty.quickstart.benefit.service.BenefitGrantService;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 积分权益发放服务实现。
 * 首期仅支持 CDK 兑换积分，后续权限或服务包权益可在此扩展独立适配器。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class PointBenefitGrantServiceImpl implements BenefitGrantService {

    private static final Logger log = LoggerFactory.getLogger(PointBenefitGrantServiceImpl.class);
    private static final String CONFIG_POINTS = "points";
    private static final String SNAPSHOT_GRANTED_POINTS = "grantedPoints";
    private static final String SNAPSHOT_AVAILABLE_POINTS = "availablePoints";
    private static final String SNAPSHOT_FROZEN_POINTS = "frozenPoints";

    private final PointAccountService pointAccountService;

    public PointBenefitGrantServiceImpl(PointAccountService pointAccountService) {
        this.pointAccountService = pointAccountService;
    }

    /**
     * 发放权益。积分类型调用积分账务 credit，保证同一事务内写账户和流水。
     */
    @Override
    public BenefitGrantResult grant(BenefitGrantCommand command) {
        if (!CdkConstants.BENEFIT_TYPE_POINTS.equals(command.getBenefitType())) {
            throw new BusinessException(ErrorCode.CDK_BENEFIT_UNSUPPORTED);
        }
        JSONObject config = JSON.parseObject(command.getBenefitConfig());
        long points = config.getLongValue(CONFIG_POINTS);
        if (points <= 0L) {
            throw new BusinessException(ErrorCode.POINT_AMOUNT_INVALID);
        }
        PointChangeCommand changeCommand = new PointChangeCommand();
        changeCommand.setUserId(command.getUserId());
        changeCommand.setAmount(points);
        changeCommand.setBizType(PointsConstants.BIZ_TYPE_CDK_RECHARGE);
        changeCommand.setBizNo(command.getBizNo());
        changeCommand.setIdempotencyKey(command.getIdempotencyKey());
        changeCommand.setOperatorType(command.getOperatorType());
        changeCommand.setOperatorId(command.getOperatorId());
        changeCommand.setRemark(command.getRemark());
        pointAccountService.credit(changeCommand);
        PointAccountVo account = pointAccountService.getOrCreateAccount(command.getUserId());
        JSONObject snapshot = new JSONObject();
        snapshot.put(SNAPSHOT_GRANTED_POINTS, points);
        snapshot.put(SNAPSHOT_AVAILABLE_POINTS, account.getAvailablePoints());
        snapshot.put(SNAPSHOT_FROZEN_POINTS, account.getFrozenPoints());
        BenefitGrantResult result = new BenefitGrantResult();
        result.setBenefitType(command.getBenefitType());
        result.setGrantedPoints(points);
        result.setAvailablePoints(account.getAvailablePoints());
        result.setFrozenPoints(account.getFrozenPoints());
        result.setSnapshot(snapshot.toJSONString());
        log.info("point benefit granted, userId={}, points={}, bizNo={}", command.getUserId(), points, command.getBizNo());
        return result;
    }
}
