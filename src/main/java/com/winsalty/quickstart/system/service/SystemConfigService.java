package com.winsalty.quickstart.system.service;

import com.winsalty.quickstart.system.dto.SystemConfigSaveRequest;
import com.winsalty.quickstart.system.vo.SystemConfigVo;

import java.util.List;

/**
 * 系统配置服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface SystemConfigService {

    List<SystemConfigVo> getConfigs();

    SystemConfigVo saveConfig(SystemConfigSaveRequest request);
}
