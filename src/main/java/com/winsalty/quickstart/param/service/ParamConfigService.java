package com.winsalty.quickstart.param.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.param.dto.ParamListRequest;
import com.winsalty.quickstart.param.dto.ParamSaveRequest;
import com.winsalty.quickstart.param.dto.ParamStatusRequest;
import com.winsalty.quickstart.param.vo.ParamConfigVo;

public interface ParamConfigService {
    PageResponse<ParamConfigVo> getPage(ParamListRequest request);

    ParamConfigVo getDetail(String id);

    ParamConfigVo save(ParamSaveRequest request);

    ParamConfigVo updateStatus(ParamStatusRequest request);

    Boolean refreshCache();
}
