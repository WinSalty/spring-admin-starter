package com.winsalty.quickstart.benefit.service.impl;

import com.winsalty.quickstart.benefit.constant.BenefitConstants;
import com.winsalty.quickstart.benefit.dto.BenefitExchangeRequest;
import com.winsalty.quickstart.benefit.dto.BenefitOrderListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductSaveRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductStatusRequest;
import com.winsalty.quickstart.benefit.entity.BenefitExchangeOrderEntity;
import com.winsalty.quickstart.benefit.entity.BenefitProductEntity;
import com.winsalty.quickstart.benefit.entity.UserBenefitEntity;
import com.winsalty.quickstart.benefit.mapper.BenefitExchangeOrderMapper;
import com.winsalty.quickstart.benefit.mapper.BenefitProductMapper;
import com.winsalty.quickstart.benefit.mapper.UserBenefitMapper;
import com.winsalty.quickstart.benefit.service.BenefitExchangeService;
import com.winsalty.quickstart.benefit.vo.BenefitExchangeOrderVo;
import com.winsalty.quickstart.benefit.vo.BenefitProductVo;
import com.winsalty.quickstart.benefit.vo.UserBenefitVo;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxService;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointFreezeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 权益兑换服务实现。
 * 通过“冻结积分-发放权益-确认扣减”流程保证权益兑换可追溯、可补偿和幂等。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class BenefitExchangeServiceImpl extends BaseService implements BenefitExchangeService {

    private static final Logger log = LoggerFactory.getLogger(BenefitExchangeServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ORDER_NO_PREFIX = "BE";
    private static final String PRODUCT_NO_PREFIX = "BP";
    private static final String EMPTY_JSON = "{}";
    private static final String EMPTY_FAILURE_MESSAGE = "";
    private static final String FREEZE_IDEMPOTENCY_PREFIX = "benefit:freeze:";
    private static final String CONFIRM_IDEMPOTENCY_PREFIX = "benefit:confirm:";
    private static final String CANCEL_IDEMPOTENCY_PREFIX = "benefit:cancel:";
    private static final String OUTBOX_AGGREGATE_TYPE = "benefit_exchange";
    private static final String OUTBOX_EVENT_SUCCESS = "benefit.exchange.success";
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final BenefitProductMapper benefitProductMapper;
    private final BenefitExchangeOrderMapper benefitExchangeOrderMapper;
    private final UserBenefitMapper userBenefitMapper;
    private final PointFreezeOrderMapper pointFreezeOrderMapper;
    private final PointAccountService pointAccountService;
    private final TransactionOutboxService transactionOutboxService;

    public BenefitExchangeServiceImpl(BenefitProductMapper benefitProductMapper,
                                      BenefitExchangeOrderMapper benefitExchangeOrderMapper,
                                      UserBenefitMapper userBenefitMapper,
                                      PointFreezeOrderMapper pointFreezeOrderMapper,
                                      PointAccountService pointAccountService,
                                      TransactionOutboxService transactionOutboxService) {
        this.benefitProductMapper = benefitProductMapper;
        this.benefitExchangeOrderMapper = benefitExchangeOrderMapper;
        this.userBenefitMapper = userBenefitMapper;
        this.pointFreezeOrderMapper = pointFreezeOrderMapper;
        this.pointAccountService = pointAccountService;
        this.transactionOutboxService = transactionOutboxService;
    }

    @Override
    public PageResponse<BenefitProductVo> listAvailableProducts(BenefitProductListRequest request) {
        BenefitProductListRequest normalized = request == null ? new BenefitProductListRequest() : request;
        // 前台可兑换列表只允许展示 active 商品，避免客户端传入状态绕过上架控制。
        normalized.setStatus(BenefitConstants.PRODUCT_STATUS_ACTIVE);
        PageResponse<BenefitProductVo> page = listProductPage(normalized);
        log.info("available benefit products loaded, total={}", page.getTotal());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitExchangeOrderVo exchange(Long productId, Long userId, BenefitExchangeRequest request) {
        validateExchangeRequest(request);
        // 先按业务幂等键查询兑换单，客户端重试时直接返回原订单，避免重复冻结积分。
        BenefitExchangeOrderEntity existed = benefitExchangeOrderMapper.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey());
        if (existed != null) {
            log.info("benefit exchange idempotency hit, userId={}, orderNo={}", userId, existed.getOrderNo());
            return toOrderVo(existed);
        }
        // 商品行加锁后再扣减库存，保证库存校验、冻结积分和订单落库位于同一串行化窗口。
        BenefitProductEntity product = benefitProductMapper.findByIdForUpdate(productId);
        validateProduct(product);
        if (benefitProductMapper.incrementStockUsed(productId) == 0) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_STOCK_NOT_ENOUGH);
        }
        String orderNo = createNo(ORDER_NO_PREFIX);
        // 冻结、确认、取消分别使用不同幂等键，补偿重试时不会互相误判为同一笔账务动作。
        String freezeIdempotencyKey = FREEZE_IDEMPOTENCY_PREFIX + orderNo;
        PointChangeCommand freezeCommand = pointCommand(userId, product.getCostPoints(), orderNo, freezeIdempotencyKey);
        pointAccountService.freeze(freezeCommand);
        PointFreezeOrderEntity freezeOrder = pointFreezeOrderMapper.findByUserIdAndIdempotencyKey(userId, freezeIdempotencyKey);
        BenefitExchangeOrderEntity order = buildOrder(orderNo, userId, product, freezeOrder.getFreezeNo(), request.getIdempotencyKey());
        benefitExchangeOrderMapper.insert(order);
        try {
            // 权益先写入用户资产，再确认冻结积分扣减；任一环节异常都会进入取消冻结补偿。
            grantUserBenefit(userId, product, orderNo);
            PointChangeCommand confirmCommand = pointCommand(userId, product.getCostPoints(), orderNo, CONFIRM_IDEMPOTENCY_PREFIX + orderNo);
            pointAccountService.confirmFreeze(freezeOrder.getFreezeNo(), confirmCommand);
            benefitExchangeOrderMapper.updateStatus(orderNo, BenefitConstants.ORDER_STATUS_SUCCESS, EMPTY_FAILURE_MESSAGE);
            // 成功事件写入 outbox，由异步任务投递后续通知或审计集成，避免主流程直接依赖外部系统。
            transactionOutboxService.createEvent(OUTBOX_AGGREGATE_TYPE, orderNo, OUTBOX_EVENT_SUCCESS, buildOutboxPayload(userId, product, orderNo));
            log.info("benefit exchanged, userId={}, productNo={}, orderNo={}, costPoints={}",
                    userId, product.getProductNo(), orderNo, product.getCostPoints());
        } catch (RuntimeException exception) {
            log.info("benefit exchange failed, cancel frozen points, userId={}, orderNo={}, freezeNo={}",
                    userId, orderNo, freezeOrder.getFreezeNo());
            PointChangeCommand cancelCommand = pointCommand(userId, product.getCostPoints(), orderNo, CANCEL_IDEMPOTENCY_PREFIX + orderNo);
            pointAccountService.cancelFreeze(freezeOrder.getFreezeNo(), cancelCommand);
            benefitExchangeOrderMapper.updateStatus(orderNo, BenefitConstants.ORDER_STATUS_FAILED, exception.getMessage());
            throw exception;
        }
        return toOrderVo(benefitExchangeOrderMapper.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey()));
    }

    @Override
    public PageResponse<BenefitExchangeOrderVo> listCurrentUserOrders(Long userId, BenefitOrderListRequest request) {
        BenefitOrderListRequest normalized = request == null ? new BenefitOrderListRequest() : request;
        normalized.setUserId(userId);
        return listOrderPage(normalized);
    }

    @Override
    public PageResponse<UserBenefitVo> listCurrentUserBenefits(Long userId, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = pageNo(pageNo);
        int normalizedPageSize = pageSize(pageSize);
        List<UserBenefitEntity> entities = userBenefitMapper.findUserPage(userId, offset(normalizedPageNo, normalizedPageSize), normalizedPageSize);
        long total = userBenefitMapper.countUserPage(userId);
        return new PageResponse<UserBenefitVo>(toUserBenefitVoList(entities), normalizedPageNo, normalizedPageSize, total);
    }

    @Override
    public PageResponse<BenefitProductVo> listAdminProducts(BenefitProductListRequest request) {
        return listProductPage(request == null ? new BenefitProductListRequest() : request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitProductVo createProduct(BenefitProductSaveRequest request) {
        BenefitProductEntity entity = buildProduct(new BenefitProductEntity(), request);
        entity.setProductNo(createNo(PRODUCT_NO_PREFIX));
        entity.setCreatedBy(currentUsername());
        entity.setStockUsed(0);
        benefitProductMapper.insert(entity);
        log.info("benefit product created, productNo={}, benefitType={}, costPoints={}",
                entity.getProductNo(), entity.getBenefitType(), entity.getCostPoints());
        return toProductVo(benefitProductMapper.findById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitProductVo updateProduct(Long id, BenefitProductSaveRequest request) {
        BenefitProductEntity existed = benefitProductMapper.findById(id);
        if (existed == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        BenefitProductEntity entity = buildProduct(existed, request);
        benefitProductMapper.update(entity);
        log.info("benefit product updated, productNo={}", entity.getProductNo());
        return toProductVo(benefitProductMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BenefitProductVo updateProductStatus(Long id, BenefitProductStatusRequest request) {
        validateProductStatus(request.getStatus());
        if (benefitProductMapper.updateStatus(id, request.getStatus()) == 0) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        log.info("benefit product status updated, id={}, status={}", id, request.getStatus());
        return toProductVo(benefitProductMapper.findById(id));
    }

    @Override
    public PageResponse<BenefitExchangeOrderVo> listAdminOrders(BenefitOrderListRequest request) {
        return listOrderPage(request == null ? new BenefitOrderListRequest() : request);
    }

    private PageResponse<BenefitProductVo> listProductPage(BenefitProductListRequest request) {
        int normalizedPageNo = pageNo(request.getPageNo());
        int normalizedPageSize = pageSize(request.getPageSize());
        List<BenefitProductEntity> entities = benefitProductMapper.findPage(request.getKeyword(), request.getBenefitType(),
                request.getStatus(), offset(normalizedPageNo, normalizedPageSize), normalizedPageSize);
        long total = benefitProductMapper.countPage(request.getKeyword(), request.getBenefitType(), request.getStatus());
        return new PageResponse<BenefitProductVo>(toProductVoList(entities), normalizedPageNo, normalizedPageSize, total);
    }

    private PageResponse<BenefitExchangeOrderVo> listOrderPage(BenefitOrderListRequest request) {
        int normalizedPageNo = pageNo(request.getPageNo());
        int normalizedPageSize = pageSize(request.getPageSize());
        List<BenefitExchangeOrderEntity> entities = benefitExchangeOrderMapper.findPage(request.getUserId(), request.getBenefitType(),
                request.getStatus(), offset(normalizedPageNo, normalizedPageSize), normalizedPageSize);
        long total = benefitExchangeOrderMapper.countPage(request.getUserId(), request.getBenefitType(), request.getStatus());
        return new PageResponse<BenefitExchangeOrderVo>(toOrderVoList(entities), normalizedPageNo, normalizedPageSize, total);
    }

    private BenefitProductEntity buildProduct(BenefitProductEntity entity, BenefitProductSaveRequest request) {
        validateBenefitType(request.getBenefitType());
        // 未显式传状态时默认上架，管理端保存逻辑仍统一走白名单校验。
        validateProductStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : BenefitConstants.PRODUCT_STATUS_ACTIVE);
        if (request.getStockTotal() == null || request.getStockTotal() < BenefitConstants.UNLIMITED_STOCK) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "库存不能小于 -1");
        }
        entity.setProductName(request.getProductName());
        entity.setBenefitType(request.getBenefitType());
        entity.setBenefitCode(request.getBenefitCode());
        entity.setBenefitName(request.getBenefitName());
        // 权益配置允许为空但落库保持 JSON 对象格式，便于发放端统一解析。
        entity.setBenefitConfig(StringUtils.hasText(request.getBenefitConfig()) ? request.getBenefitConfig() : EMPTY_JSON);
        entity.setCostPoints(request.getCostPoints());
        entity.setStockTotal(request.getStockTotal());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : BenefitConstants.PRODUCT_STATUS_ACTIVE);
        return entity;
    }

    private BenefitExchangeOrderEntity buildOrder(String orderNo,
                                                  Long userId,
                                                  BenefitProductEntity product,
                                                  String freezeNo,
                                                  String idempotencyKey) {
        BenefitExchangeOrderEntity order = new BenefitExchangeOrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductNo(product.getProductNo());
        order.setBenefitType(product.getBenefitType());
        order.setBenefitCode(product.getBenefitCode());
        order.setCostPoints(product.getCostPoints());
        order.setFreezeNo(freezeNo);
        order.setStatus(BenefitConstants.ORDER_STATUS_PROCESSING);
        order.setFailureMessage(EMPTY_FAILURE_MESSAGE);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }

    private void grantUserBenefit(Long userId, BenefitProductEntity product, String orderNo) {
        UserBenefitEntity entity = new UserBenefitEntity();
        entity.setUserId(userId);
        entity.setBenefitType(product.getBenefitType());
        entity.setBenefitCode(product.getBenefitCode());
        entity.setBenefitName(product.getBenefitName());
        entity.setSourceType(BenefitConstants.SOURCE_TYPE_POINT_EXCHANGE);
        entity.setSourceNo(orderNo);
        entity.setStatus(BenefitConstants.USER_BENEFIT_STATUS_ACTIVE);
        // 生效时间使用服务端当前时间，过期时间沿用商品有效期，避免客户端影响权益窗口。
        entity.setEffectiveAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        entity.setExpireAt(product.getValidTo());
        entity.setConfigSnapshot(product.getBenefitConfig());
        userBenefitMapper.insert(entity);
    }

    private String buildOutboxPayload(Long userId, BenefitProductEntity product, String orderNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("orderNo", orderNo);
        payload.put("benefitType", product.getBenefitType());
        payload.put("benefitCode", product.getBenefitCode());
        return FastJsonUtils.toJsonString(payload);
    }

    private PointChangeCommand pointCommand(Long userId, Long amount, String orderNo, String idempotencyKey) {
        PointChangeCommand command = new PointChangeCommand();
        command.setUserId(userId);
        command.setAmount(amount);
        command.setBizType(PointsConstants.BIZ_TYPE_BENEFIT_EXCHANGE);
        command.setBizNo(orderNo);
        command.setIdempotencyKey(idempotencyKey);
        command.setOperatorType(PointsConstants.OPERATOR_TYPE_USER);
        command.setOperatorId(String.valueOf(userId));
        command.setRemark("积分兑换权益");
        return command;
    }

    private void validateExchangeRequest(BenefitExchangeRequest request) {
        if (request == null || !StringUtils.hasText(request.getIdempotencyKey())) {
            throw new BusinessException(ErrorCode.POINT_IDEMPOTENCY_REQUIRED);
        }
    }

    private void validateProduct(BenefitProductEntity product) {
        if (product == null) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_NOT_FOUND);
        }
        if (!BenefitConstants.PRODUCT_STATUS_ACTIVE.equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        // 有效期判断使用服务端时间，防止客户端时间差导致提前兑换或过期兑换。
        LocalDateTime validFrom = LocalDateTime.parse(product.getValidFrom(), DATE_TIME_FORMATTER);
        LocalDateTime validTo = LocalDateTime.parse(product.getValidTo(), DATE_TIME_FORMATTER);
        if (now.isBefore(validFrom) || now.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.BENEFIT_PRODUCT_UNAVAILABLE);
        }
    }

    private void validateBenefitType(String benefitType) {
        if (!BenefitConstants.BENEFIT_TYPE_PERMISSION.equals(benefitType)
                && !BenefitConstants.BENEFIT_TYPE_SERVICE_PACKAGE.equals(benefitType)) {
            throw new BusinessException(ErrorCode.BENEFIT_TYPE_UNSUPPORTED);
        }
    }

    private void validateProductStatus(String status) {
        if (!BenefitConstants.PRODUCT_STATUS_ACTIVE.equals(status) && !BenefitConstants.PRODUCT_STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "权益商品状态只允许 active 或 disabled");
        }
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    private int pageNo(Integer value) {
        return value == null || value < BenefitConstants.DEFAULT_PAGE_NO ? BenefitConstants.DEFAULT_PAGE_NO : value;
    }

    private int pageSize(Integer value) {
        if (value == null || value < BenefitConstants.DEFAULT_PAGE_NO) {
            return BenefitConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, BenefitConstants.MAX_PAGE_SIZE);
    }

    private int offset(int pageNo, int pageSize) {
        return (pageNo - BenefitConstants.DEFAULT_PAGE_NO) * pageSize;
    }

    private BenefitProductVo toProductVo(BenefitProductEntity entity) {
        BenefitProductVo vo = new BenefitProductVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setProductNo(entity.getProductNo());
        vo.setProductName(entity.getProductName());
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitCode(entity.getBenefitCode());
        vo.setBenefitName(entity.getBenefitName());
        vo.setBenefitConfig(entity.getBenefitConfig());
        vo.setCostPoints(entity.getCostPoints());
        vo.setStockTotal(entity.getStockTotal());
        vo.setStockUsed(entity.getStockUsed());
        vo.setValidFrom(entity.getValidFrom());
        vo.setValidTo(entity.getValidTo());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<BenefitProductVo> toProductVoList(List<BenefitProductEntity> entities) {
        List<BenefitProductVo> records = new ArrayList<BenefitProductVo>();
        for (BenefitProductEntity entity : entities) {
            records.add(toProductVo(entity));
        }
        return records;
    }

    private BenefitExchangeOrderVo toOrderVo(BenefitExchangeOrderEntity entity) {
        BenefitExchangeOrderVo vo = new BenefitExchangeOrderVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setOrderNo(entity.getOrderNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setProductId(String.valueOf(entity.getProductId()));
        vo.setProductNo(entity.getProductNo());
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitCode(entity.getBenefitCode());
        vo.setCostPoints(entity.getCostPoints());
        vo.setFreezeNo(entity.getFreezeNo());
        vo.setStatus(entity.getStatus());
        vo.setFailureMessage(entity.getFailureMessage());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<BenefitExchangeOrderVo> toOrderVoList(List<BenefitExchangeOrderEntity> entities) {
        List<BenefitExchangeOrderVo> records = new ArrayList<BenefitExchangeOrderVo>();
        for (BenefitExchangeOrderEntity entity : entities) {
            records.add(toOrderVo(entity));
        }
        return records;
    }

    private UserBenefitVo toUserBenefitVo(UserBenefitEntity entity) {
        UserBenefitVo vo = new UserBenefitVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitCode(entity.getBenefitCode());
        vo.setBenefitName(entity.getBenefitName());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceNo(entity.getSourceNo());
        vo.setStatus(entity.getStatus());
        vo.setEffectiveAt(entity.getEffectiveAt());
        vo.setExpireAt(entity.getExpireAt());
        vo.setConfigSnapshot(entity.getConfigSnapshot());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<UserBenefitVo> toUserBenefitVoList(List<UserBenefitEntity> entities) {
        List<UserBenefitVo> records = new ArrayList<UserBenefitVo>();
        for (UserBenefitEntity entity : entities) {
            records.add(toUserBenefitVo(entity));
        }
        return records;
    }
}
