package com.winsalty.quickstart.points.service;

/**
 * 积分补偿服务接口。
 * 用于定时处理过期冻结单等需要最终一致补偿的账务事项。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface PointCompensationService {

    int cancelExpiredFreezes();
}
