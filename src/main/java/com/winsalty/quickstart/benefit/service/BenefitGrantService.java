package com.winsalty.quickstart.benefit.service;

import com.winsalty.quickstart.benefit.dto.BenefitGrantCommand;
import com.winsalty.quickstart.benefit.dto.BenefitGrantResult;

/**
 * 权益发放服务接口。
 * 为凭证兑换和后续积分兑换提供统一扩展点。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface BenefitGrantService {

    BenefitGrantResult grant(BenefitGrantCommand command);
}
