package com.winsalty.quickstart.points.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.web.TraceIdFilter;
import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.AdminPointAccountListRequest;
import com.winsalty.quickstart.points.dto.AdminPointLedgerListRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentApproveRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentRequest;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.dto.PointLedgerListRequest;
import com.winsalty.quickstart.points.entity.PointAccountEntity;
import com.winsalty.quickstart.points.entity.PointAdjustmentOrderEntity;
import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import com.winsalty.quickstart.points.entity.PointLedgerEntity;
import com.winsalty.quickstart.points.entity.PointReconciliationRecordEntity;
import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointAccountMapper;
import com.winsalty.quickstart.points.mapper.PointAdjustmentOrderMapper;
import com.winsalty.quickstart.points.mapper.PointFreezeOrderMapper;
import com.winsalty.quickstart.points.mapper.PointLedgerMapper;
import com.winsalty.quickstart.points.mapper.PointReconciliationRecordMapper;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import com.winsalty.quickstart.points.vo.PointAdjustmentOrderVo;
import com.winsalty.quickstart.points.vo.PointFreezeOrderVo;
import com.winsalty.quickstart.points.vo.PointLedgerVo;
import com.winsalty.quickstart.points.vo.PointRechargeOrderVo;
import com.winsalty.quickstart.points.vo.PointReconciliationVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 积分账户服务实现。
 * 使用账户行锁和账本哈希链保证积分变更具备并发安全、幂等和审计追溯能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class PointAccountServiceImpl extends BaseService implements PointAccountService {

    private static final Logger log = LoggerFactory.getLogger(PointAccountServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LEDGER_NO_PREFIX = "PL";
    private static final String FREEZE_NO_PREFIX = "PF";
    private static final String ADJUST_NO_PREFIX = "PA";
    private static final String RECONCILE_NO_PREFIX = "PC";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String HASH_SEPARATOR = "|";
    private static final String ADMIN_ADJUST_REMARK_PREFIX = "人工调整：";
    private static final String RECONCILE_STATUS_MATCHED = "matched";
    private static final String RECONCILE_STATUS_DIFFERENT = "different";
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final PointAccountMapper pointAccountMapper;
    private final PointLedgerMapper pointLedgerMapper;
    private final PointRechargeOrderMapper pointRechargeOrderMapper;
    private final PointFreezeOrderMapper pointFreezeOrderMapper;
    private final PointAdjustmentOrderMapper pointAdjustmentOrderMapper;
    private final PointReconciliationRecordMapper pointReconciliationRecordMapper;
    private final PointsProperties pointsProperties;

    public PointAccountServiceImpl(PointAccountMapper pointAccountMapper,
                                   PointLedgerMapper pointLedgerMapper,
                                   PointRechargeOrderMapper pointRechargeOrderMapper,
                                   PointFreezeOrderMapper pointFreezeOrderMapper,
                                   PointAdjustmentOrderMapper pointAdjustmentOrderMapper,
                                   PointReconciliationRecordMapper pointReconciliationRecordMapper,
                                   PointsProperties pointsProperties) {
        this.pointAccountMapper = pointAccountMapper;
        this.pointLedgerMapper = pointLedgerMapper;
        this.pointRechargeOrderMapper = pointRechargeOrderMapper;
        this.pointFreezeOrderMapper = pointFreezeOrderMapper;
        this.pointAdjustmentOrderMapper = pointAdjustmentOrderMapper;
        this.pointReconciliationRecordMapper = pointReconciliationRecordMapper;
        this.pointsProperties = pointsProperties;
    }

    /**
     * 获取或初始化用户积分账户。账户创建使用 INSERT IGNORE，应对并发首次访问。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointAccountVo getOrCreateAccount(Long userId) {
        PointAccountEntity entity = getOrCreateAccountEntity(userId);
        log.info("point account loaded, userId={}, accountId={}", userId, entity.getId());
        return toAccountVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo credit(PointChangeCommand command) {
        validateCommand(command);
        // 入账只增加可用余额和累计获得，冻结余额不变。
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_EARN,
                command.getAmount(), 0L, command.getAmount(), 0L);
        log.info("points credited, userId={}, amount={}, bizType={}, bizNo={}",
                command.getUserId(), command.getAmount(), command.getBizType(), command.getBizNo());
        return toLedgerVo(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo debit(PointChangeCommand command) {
        validateCommand(command);
        // 直接扣减只减少可用余额，适用于无需冻结确认的消费场景。
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_SPEND,
                -command.getAmount(), 0L, 0L, command.getAmount());
        log.info("points debited, userId={}, amount={}, bizType={}, bizNo={}",
                command.getUserId(), command.getAmount(), command.getBizType(), command.getBizNo());
        return toLedgerVo(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo freeze(PointChangeCommand command) {
        validateCommand(command);
        // 冻结阶段只把可用积分转入冻结积分，不计入累计消费，等确认冻结时再记消费。
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_FREEZE,
                -command.getAmount(), command.getAmount(), 0L, 0L);
        PointFreezeOrderEntity order = new PointFreezeOrderEntity();
        order.setFreezeNo(createNo(FREEZE_NO_PREFIX));
        order.setUserId(command.getUserId());
        order.setAmount(command.getAmount());
        order.setBizType(command.getBizType());
        order.setBizNo(command.getBizNo());
        order.setStatus(PointsConstants.FREEZE_STATUS_FROZEN);
        order.setExpireAt(LocalDateTime.now().plusSeconds(pointsProperties.getFreezeDefaultExpireSeconds()).format(DATE_TIME_FORMATTER));
        order.setIdempotencyKey(command.getIdempotencyKey());
        pointFreezeOrderMapper.insert(order);
        log.info("points frozen, userId={}, amount={}, freezeNo={}", command.getUserId(), command.getAmount(), order.getFreezeNo());
        return toLedgerVo(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo confirmFreeze(String freezeNo, PointChangeCommand command) {
        PointFreezeOrderEntity order = loadFreezeOrder(freezeNo);
        if (PointsConstants.FREEZE_STATUS_CONFIRMED.equals(order.getStatus())) {
            // 确认冻结重复调用按幂等键返回原流水，保证权益发放回调可安全重试。
            return toLedgerVo(pointLedgerMapper.findByUserIdAndIdempotencyKey(order.getUserId(), command.getIdempotencyKey()));
        }
        if (!PointsConstants.FREEZE_STATUS_FROZEN.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.POINT_FREEZE_ORDER_NOT_FOUND, "冻结单状态不可确认");
        }
        fillCommandFromFreezeOrder(command, order);
        // 确认冻结时只减少冻结余额，并把金额计入累计消费。
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_SPEND,
                0L, -order.getAmount(), 0L, order.getAmount());
        pointFreezeOrderMapper.updateStatus(freezeNo, PointsConstants.FREEZE_STATUS_CONFIRMED);
        log.info("points freeze confirmed, userId={}, amount={}, freezeNo={}", order.getUserId(), order.getAmount(), freezeNo);
        return toLedgerVo(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo cancelFreeze(String freezeNo, PointChangeCommand command) {
        PointFreezeOrderEntity order = loadFreezeOrder(freezeNo);
        if (PointsConstants.FREEZE_STATUS_CANCELLED.equals(order.getStatus())) {
            // 取消冻结重复调用按幂等键返回原流水，支撑补偿任务重复扫描。
            return toLedgerVo(pointLedgerMapper.findByUserIdAndIdempotencyKey(order.getUserId(), command.getIdempotencyKey()));
        }
        if (!PointsConstants.FREEZE_STATUS_FROZEN.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.POINT_FREEZE_ORDER_NOT_FOUND, "冻结单状态不可取消");
        }
        fillCommandFromFreezeOrder(command, order);
        // 取消冻结把冻结积分退回可用积分，不影响累计获得或累计消费。
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_UNFREEZE,
                order.getAmount(), -order.getAmount(), 0L, 0L);
        pointFreezeOrderMapper.updateStatus(freezeNo, PointsConstants.FREEZE_STATUS_CANCELLED);
        log.info("points freeze cancelled, userId={}, amount={}, freezeNo={}", order.getUserId(), order.getAmount(), freezeNo);
        return toLedgerVo(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointLedgerVo refund(PointChangeCommand command) {
        validateCommand(command);
        PointLedgerEntity ledger = applyChange(command, PointsConstants.DIRECTION_REFUND,
                command.getAmount(), 0L, command.getAmount(), 0L);
        log.info("points refunded, userId={}, amount={}, bizType={}, bizNo={}",
                command.getUserId(), command.getAmount(), command.getBizType(), command.getBizNo());
        return toLedgerVo(ledger);
    }

    @Override
    public PageResponse<PointLedgerVo> listCurrentUserLedger(Long userId, PointLedgerListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        int offset = offset(pageNo, pageSize);
        List<PointLedgerEntity> entities = pointLedgerMapper.findUserPage(userId, request.getDirection(), request.getBizType(), offset, pageSize);
        long total = pointLedgerMapper.countUserPage(userId, request.getDirection(), request.getBizType());
        log.info("current user point ledger loaded, userId={}, total={}", userId, total);
        return new PageResponse<PointLedgerVo>(toLedgerVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public PageResponse<PointRechargeOrderVo> listCurrentUserRechargeOrders(Long userId, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = pageNo(pageNo);
        int normalizedPageSize = pageSize(pageSize);
        List<PointRechargeOrderEntity> entities = pointRechargeOrderMapper.findUserPage(userId, offset(normalizedPageNo, normalizedPageSize), normalizedPageSize);
        long total = pointRechargeOrderMapper.countUserPage(userId);
        return new PageResponse<PointRechargeOrderVo>(toRechargeOrderVoList(entities), normalizedPageNo, normalizedPageSize, total);
    }

    @Override
    public PageResponse<PointLedgerVo> listCurrentUserConsumeOrders(Long userId, Integer pageNo, Integer pageSize) {
        PointLedgerListRequest request = new PointLedgerListRequest();
        request.setDirection(PointsConstants.DIRECTION_SPEND);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return listCurrentUserLedger(userId, request);
    }

    @Override
    public PageResponse<PointFreezeOrderVo> listCurrentUserFreezeOrders(Long userId, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = pageNo(pageNo);
        int normalizedPageSize = pageSize(pageSize);
        List<PointFreezeOrderEntity> entities = pointFreezeOrderMapper.findUserPage(userId, offset(normalizedPageNo, normalizedPageSize), normalizedPageSize);
        long total = pointFreezeOrderMapper.countUserPage(userId);
        return new PageResponse<PointFreezeOrderVo>(toFreezeOrderVoList(entities), normalizedPageNo, normalizedPageSize, total);
    }

    @Override
    public PageResponse<PointAccountVo> listAccounts(AdminPointAccountListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<PointAccountEntity> entities = pointAccountMapper.findPage(request.getKeyword(), request.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = pointAccountMapper.countPage(request.getKeyword(), request.getStatus());
        return new PageResponse<PointAccountVo>(toAccountVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public PageResponse<PointLedgerVo> listAdminLedger(AdminPointLedgerListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<PointLedgerEntity> entities = pointLedgerMapper.findAdminPage(request.getUserId(), request.getDirection(),
                request.getBizType(), request.getBizNo(), offset(pageNo, pageSize), pageSize);
        long total = pointLedgerMapper.countAdminPage(request.getUserId(), request.getDirection(), request.getBizType(), request.getBizNo());
        return new PageResponse<PointLedgerVo>(toLedgerVoList(entities), pageNo, pageSize, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointAdjustmentOrderVo createAdjustment(PointAdjustmentRequest request) {
        validateAdjustmentDirection(request.getDirection());
        PointAdjustmentOrderEntity entity = new PointAdjustmentOrderEntity();
        entity.setAdjustNo(createNo(ADJUST_NO_PREFIX));
        entity.setUserId(request.getUserId());
        entity.setDirection(request.getDirection());
        entity.setAmount(request.getAmount());
        entity.setStatus(PointsConstants.ADJUST_STATUS_PENDING);
        entity.setReason(request.getReason());
        entity.setTicketNo(request.getTicketNo());
        entity.setIdempotencyKey(request.getIdempotencyKey());
        entity.setApplicant(currentUsername());
        pointAdjustmentOrderMapper.insert(entity);
        log.info("point adjustment created, adjustNo={}, userId={}, direction={}, amount={}",
                entity.getAdjustNo(), entity.getUserId(), entity.getDirection(), entity.getAmount());
        return toAdjustmentOrderVo(pointAdjustmentOrderMapper.findById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointAdjustmentOrderVo approveAdjustment(Long id, PointAdjustmentApproveRequest request) {
        PointAdjustmentOrderEntity entity = pointAdjustmentOrderMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.POINT_ADJUSTMENT_NOT_FOUND);
        }
        if (!PointsConstants.ADJUST_STATUS_PENDING.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.POINT_ADJUSTMENT_STATUS_INVALID);
        }
        String nextStatus = Boolean.TRUE.equals(request.getApproved())
                ? PointsConstants.ADJUST_STATUS_APPROVED
                : PointsConstants.ADJUST_STATUS_REJECTED;
        int updated = pointAdjustmentOrderMapper.updateApproval(id, nextStatus, currentUsername());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.POINT_ADJUSTMENT_STATUS_INVALID);
        }
        if (Boolean.TRUE.equals(request.getApproved())) {
            PointChangeCommand command = new PointChangeCommand();
            // 审批通过后才真正调用账务服务，调整单创建本身不改变余额。
            command.setUserId(entity.getUserId());
            command.setAmount(entity.getAmount());
            command.setBizType(PointsConstants.BIZ_TYPE_ADMIN_ADJUST);
            command.setBizNo(entity.getAdjustNo());
            command.setIdempotencyKey(entity.getIdempotencyKey());
            command.setOperatorType(PointsConstants.OPERATOR_TYPE_ADMIN);
            command.setOperatorId(currentUsername());
            command.setRemark(ADMIN_ADJUST_REMARK_PREFIX + entity.getReason());
            if (PointsConstants.DIRECTION_EARN.equals(entity.getDirection())) {
                credit(command);
            } else {
                // 扣减类人工调整仍复用 debit 的余额校验，避免管理员扣成负数。
                debit(command);
            }
        }
        log.info("point adjustment approved, adjustId={}, approved={}", id, request.getApproved());
        return toAdjustmentOrderVo(pointAdjustmentOrderMapper.findById(id));
    }

    @Override
    public PointReconciliationVo reconcile() {
        PointReconciliationVo vo = new PointReconciliationVo();
        // 对账按账户表余额与流水汇总结果比较，发现差异只记录，不自动修复。
        Long accountAvailable = pointAccountMapper.sumAvailable();
        Long accountFrozen = pointAccountMapper.sumFrozen();
        Long ledgerAvailable = pointLedgerMapper.sumAvailableByLedger();
        Long ledgerFrozen = pointLedgerMapper.sumFrozenByLedger();
        long availableDiff = safeLong(accountAvailable) - safeLong(ledgerAvailable);
        long frozenDiff = safeLong(accountFrozen) - safeLong(ledgerFrozen);
        vo.setCheckedAccounts(pointAccountMapper.countAccounts());
        vo.setDifferentAccounts((availableDiff == 0L && frozenDiff == 0L) ? 0L : 1L);
        vo.setTotalAvailableDiff(availableDiff);
        vo.setTotalFrozenDiff(frozenDiff);
        vo.setCheckedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        persistReconciliationRecord(vo);
        log.info("points reconciliation completed, checkedAccounts={}, availableDiff={}, frozenDiff={}",
                vo.getCheckedAccounts(), availableDiff, frozenDiff);
        return vo;
    }

    private void persistReconciliationRecord(PointReconciliationVo vo) {
        PointReconciliationRecordEntity entity = new PointReconciliationRecordEntity();
        entity.setReconcileNo(createNo(RECONCILE_NO_PREFIX));
        entity.setCheckedAccounts(vo.getCheckedAccounts());
        entity.setDifferentAccounts(vo.getDifferentAccounts());
        entity.setTotalAvailableDiff(vo.getTotalAvailableDiff());
        entity.setTotalFrozenDiff(vo.getTotalFrozenDiff());
        entity.setStatus(vo.getDifferentAccounts() == 0L ? RECONCILE_STATUS_MATCHED : RECONCILE_STATUS_DIFFERENT);
        entity.setCheckedAt(vo.getCheckedAt());
        pointReconciliationRecordMapper.insert(entity);
    }

    private PointLedgerEntity applyChange(PointChangeCommand command,
                                          String direction,
                                          Long availableDelta,
                                          Long frozenDelta,
                                          Long earnedDelta,
                                          Long spentDelta) {
        PointLedgerEntity existed = pointLedgerMapper.findByUserIdAndIdempotencyKey(command.getUserId(), command.getIdempotencyKey());
        if (existed != null) {
            // 幂等先在加锁前查一次，常见重复请求可以避免不必要的账户行锁。
            log.info("point idempotency hit, userId={}, idempotencyKey={}, ledgerNo={}",
                    command.getUserId(), command.getIdempotencyKey(), existed.getLedgerNo());
            return existed;
        }
        // 账户行锁保护余额计算和版本更新，避免并发扣减时出现超扣。
        PointAccountEntity account = getOrCreateAccountEntityForUpdate(command.getUserId());
        if (!PointsConstants.ACCOUNT_STATUS_ACTIVE.equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.POINT_ACCOUNT_DISABLED);
        }
        existed = pointLedgerMapper.findByUserIdAndIdempotencyKey(command.getUserId(), command.getIdempotencyKey());
        if (existed != null) {
            // 加锁后再查一次幂等，处理两个相同请求同时通过第一次幂等检查的并发场景。
            return existed;
        }
        long nextAvailable = account.getAvailablePoints() + availableDelta;
        long nextFrozen = account.getFrozenPoints() + frozenDelta;
        if (nextAvailable < 0L || nextFrozen < 0L) {
            // 可用或冻结余额任何一个小于 0 都视为账务非法，直接回滚事务。
            throw new BusinessException(ErrorCode.POINT_BALANCE_NOT_ENOUGH);
        }
        PointAccountEntity update = new PointAccountEntity();
        update.setId(account.getId());
        update.setAvailablePoints(nextAvailable);
        update.setFrozenPoints(nextFrozen);
        update.setTotalEarnedPoints(account.getTotalEarnedPoints() + earnedDelta);
        update.setTotalSpentPoints(account.getTotalSpentPoints() + spentDelta);
        update.setVersion(account.getVersion());
        if (pointAccountMapper.updateBalance(update) == 0) {
            // updateBalance 带 version 条件，返回 0 表示并发修改或余额条件不满足。
            throw new BusinessException(ErrorCode.POINT_BALANCE_NOT_ENOUGH, "积分账户并发更新失败");
        }
        PointLedgerEntity ledger = buildLedger(command, account, direction, nextAvailable, nextFrozen);
        pointLedgerMapper.insert(ledger);
        return ledger;
    }

    private PointLedgerEntity buildLedger(PointChangeCommand command,
                                          PointAccountEntity account,
                                          String direction,
                                          long nextAvailable,
                                          long nextFrozen) {
        PointLedgerEntity lastLedger = pointLedgerMapper.findLastByAccountId(account.getId());
        PointLedgerEntity ledger = new PointLedgerEntity();
        // 流水记录变更前后余额和冻结余额，用于审计和后续对账。
        ledger.setLedgerNo(createNo(LEDGER_NO_PREFIX));
        ledger.setUserId(command.getUserId());
        ledger.setAccountId(account.getId());
        ledger.setDirection(direction);
        ledger.setAmount(command.getAmount());
        ledger.setBalanceBefore(account.getAvailablePoints());
        ledger.setBalanceAfter(nextAvailable);
        ledger.setFrozenBefore(account.getFrozenPoints());
        ledger.setFrozenAfter(nextFrozen);
        ledger.setBizType(command.getBizType());
        ledger.setBizNo(command.getBizNo());
        ledger.setIdempotencyKey(command.getIdempotencyKey());
        ledger.setOperatorType(command.getOperatorType());
        ledger.setOperatorId(command.getOperatorId());
        ledger.setTraceId(currentTraceId());
        ledger.setPrevHash(lastLedger == null ? PointsConstants.HASH_GENESIS : lastLedger.getEntryHash());
        ledger.setRemark(command.getRemark());
        // entryHash 串联上一条流水 hash，形成账户维度哈希链，便于发现历史流水被篡改。
        ledger.setEntryHash(calculateLedgerHash(ledger));
        return ledger;
    }

    private PointAccountEntity getOrCreateAccountEntity(Long userId) {
        pointAccountMapper.insertIgnore(userId);
        return pointAccountMapper.findByUserId(userId);
    }

    private PointAccountEntity getOrCreateAccountEntityForUpdate(Long userId) {
        // insertIgnore 处理首次访问并发创建账户，随后 FOR UPDATE 锁定账户行。
        pointAccountMapper.insertIgnore(userId);
        return pointAccountMapper.findByUserIdForUpdate(userId);
    }

    private PointFreezeOrderEntity loadFreezeOrder(String freezeNo) {
        PointFreezeOrderEntity order = pointFreezeOrderMapper.findByFreezeNo(freezeNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.POINT_FREEZE_ORDER_NOT_FOUND);
        }
        return order;
    }

    private void fillCommandFromFreezeOrder(PointChangeCommand command, PointFreezeOrderEntity order) {
        // 冻结单是确认/取消的事实来源，调用方传入的用户、金额、业务单号不参与最终计算。
        command.setUserId(order.getUserId());
        command.setAmount(order.getAmount());
        command.setBizType(order.getBizType());
        command.setBizNo(order.getBizNo());
        validateCommand(command);
    }

    private void validateCommand(PointChangeCommand command) {
        if (command == null || command.getUserId() == null || command.getAmount() == null || command.getAmount() <= 0L) {
            throw new BusinessException(ErrorCode.POINT_AMOUNT_INVALID);
        }
        if (!StringUtils.hasText(command.getIdempotencyKey())) {
            throw new BusinessException(ErrorCode.POINT_IDEMPOTENCY_REQUIRED);
        }
    }

    private void validateAdjustmentDirection(String direction) {
        if (!PointsConstants.DIRECTION_EARN.equals(direction) && !PointsConstants.DIRECTION_SPEND.equals(direction)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "调整方向只允许 earn 或 spend");
        }
    }

    private String calculateLedgerHash(PointLedgerEntity ledger) {
        // hash payload 固定字段顺序，保证不同节点计算结果一致。
        String payload = ledger.getPrevHash() + HASH_SEPARATOR
                + ledger.getLedgerNo() + HASH_SEPARATOR
                + ledger.getUserId() + HASH_SEPARATOR
                + ledger.getAccountId() + HASH_SEPARATOR
                + ledger.getDirection() + HASH_SEPARATOR
                + ledger.getAmount() + HASH_SEPARATOR
                + ledger.getBalanceBefore() + HASH_SEPARATOR
                + ledger.getBalanceAfter() + HASH_SEPARATOR
                + ledger.getFrozenBefore() + HASH_SEPARATOR
                + ledger.getFrozenAfter() + HASH_SEPARATOR
                + ledger.getBizType() + HASH_SEPARATOR
                + ledger.getBizNo() + HASH_SEPARATOR
                + ledger.getIdempotencyKey();
        return sha256(payload);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("point ledger hash failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            chars[index * 2] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            chars[index * 2 + 1] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(chars);
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return StringUtils.hasText(traceId) ? traceId : "";
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    private int pageNo(Integer value) {
        return value == null || value < PointsConstants.DEFAULT_PAGE_NO ? PointsConstants.DEFAULT_PAGE_NO : value;
    }

    private int pageSize(Integer value) {
        if (value == null || value < PointsConstants.DEFAULT_PAGE_NO) {
            return PointsConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, PointsConstants.MAX_PAGE_SIZE);
    }

    private int offset(int pageNo, int pageSize) {
        return (pageNo - PointsConstants.DEFAULT_PAGE_NO) * pageSize;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private PointAccountVo toAccountVo(PointAccountEntity entity) {
        PointAccountVo vo = new PointAccountVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setAvailablePoints(entity.getAvailablePoints());
        vo.setFrozenPoints(entity.getFrozenPoints());
        vo.setTotalEarnedPoints(entity.getTotalEarnedPoints());
        vo.setTotalSpentPoints(entity.getTotalSpentPoints());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<PointAccountVo> toAccountVoList(List<PointAccountEntity> entities) {
        List<PointAccountVo> records = new ArrayList<PointAccountVo>();
        for (PointAccountEntity entity : entities) {
            records.add(toAccountVo(entity));
        }
        return records;
    }

    private PointLedgerVo toLedgerVo(PointLedgerEntity entity) {
        if (entity == null) {
            return null;
        }
        PointLedgerVo vo = new PointLedgerVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setLedgerNo(entity.getLedgerNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setAccountId(String.valueOf(entity.getAccountId()));
        vo.setDirection(entity.getDirection());
        vo.setAmount(entity.getAmount());
        vo.setBalanceBefore(entity.getBalanceBefore());
        vo.setBalanceAfter(entity.getBalanceAfter());
        vo.setFrozenBefore(entity.getFrozenBefore());
        vo.setFrozenAfter(entity.getFrozenAfter());
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setOperatorType(entity.getOperatorType());
        vo.setOperatorId(entity.getOperatorId());
        vo.setTraceId(entity.getTraceId());
        vo.setEntryHash(entity.getEntryHash());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private List<PointLedgerVo> toLedgerVoList(List<PointLedgerEntity> entities) {
        List<PointLedgerVo> records = new ArrayList<PointLedgerVo>();
        for (PointLedgerEntity entity : entities) {
            records.add(toLedgerVo(entity));
        }
        return records;
    }

    private PointRechargeOrderVo toRechargeOrderVo(PointRechargeOrderEntity entity) {
        PointRechargeOrderVo vo = new PointRechargeOrderVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setRechargeNo(entity.getRechargeNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setChannel(entity.getChannel());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setExternalNo(entity.getExternalNo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<PointRechargeOrderVo> toRechargeOrderVoList(List<PointRechargeOrderEntity> entities) {
        List<PointRechargeOrderVo> records = new ArrayList<PointRechargeOrderVo>();
        for (PointRechargeOrderEntity entity : entities) {
            records.add(toRechargeOrderVo(entity));
        }
        return records;
    }

    private PointFreezeOrderVo toFreezeOrderVo(PointFreezeOrderEntity entity) {
        PointFreezeOrderVo vo = new PointFreezeOrderVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setFreezeNo(entity.getFreezeNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setAmount(entity.getAmount());
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setStatus(entity.getStatus());
        vo.setExpireAt(entity.getExpireAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<PointFreezeOrderVo> toFreezeOrderVoList(List<PointFreezeOrderEntity> entities) {
        List<PointFreezeOrderVo> records = new ArrayList<PointFreezeOrderVo>();
        for (PointFreezeOrderEntity entity : entities) {
            records.add(toFreezeOrderVo(entity));
        }
        return records;
    }

    private PointAdjustmentOrderVo toAdjustmentOrderVo(PointAdjustmentOrderEntity entity) {
        PointAdjustmentOrderVo vo = new PointAdjustmentOrderVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setAdjustNo(entity.getAdjustNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setDirection(entity.getDirection());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setReason(entity.getReason());
        vo.setTicketNo(entity.getTicketNo());
        vo.setApplicant(entity.getApplicant());
        vo.setApprover(entity.getApprover());
        vo.setApprovedAt(entity.getApprovedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
