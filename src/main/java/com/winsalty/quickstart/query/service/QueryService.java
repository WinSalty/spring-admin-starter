package com.winsalty.quickstart.query.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.query.dto.QueryListRequest;
import com.winsalty.quickstart.query.dto.QuerySaveRequest;
import com.winsalty.quickstart.query.vo.QueryRecordVo;

/**
 * 查询配置服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface QueryService {

    PageResponse<QueryRecordVo> getPage(QueryListRequest request);

    QueryRecordVo getDetail(String id);

    QueryRecordVo save(QuerySaveRequest request);
}
