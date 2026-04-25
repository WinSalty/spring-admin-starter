package com.winsalty.quickstart.param.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.param.dto.ParamListRequest;
import com.winsalty.quickstart.param.dto.ParamSaveRequest;
import com.winsalty.quickstart.param.dto.ParamStatusRequest;
import com.winsalty.quickstart.param.vo.ParamConfigVo;

/**
 * 参数配置服务。
 * 提供参数分页、详情维护、状态切换和缓存刷新能力。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
public interface ParamConfigService {

    /**
     * 查询参数配置分页列表。
     */
    PageResponse<ParamConfigVo> getPage(ParamListRequest request);

    /**
     * 查询参数配置详情。
     */
    ParamConfigVo getDetail(String id);

    /**
     * 新增或编辑参数配置。
     */
    ParamConfigVo save(ParamSaveRequest request);

    /**
     * 更新参数启停状态。
     */
    ParamConfigVo updateStatus(ParamStatusRequest request);

    /**
     * 刷新启用参数缓存。
     */
    Boolean refreshCache();
}
