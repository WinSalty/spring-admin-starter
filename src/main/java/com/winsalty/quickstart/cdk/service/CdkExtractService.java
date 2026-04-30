package com.winsalty.quickstart.cdk.service;

import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkDisableRequest;
import com.winsalty.quickstart.cdk.vo.CdkExtractAccessRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractLinkVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractViewVo;
import com.winsalty.quickstart.common.api.PageResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * CDK 提取链接服务接口。
 * 负责管理端临时 URL 生成、停用、访问记录查询和公开提取访问。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
public interface CdkExtractService {

    CdkExtractLinkVo createLink(Long codeId, CdkExtractLinkCreateRequest request);

    List<CdkExtractLinkVo> listLinks(Long codeId);

    CdkExtractLinkVo disableLink(Long linkId, CdkExtractLinkDisableRequest request);

    PageResponse<CdkExtractAccessRecordVo> listAccessRecords(Long linkId, CdkExtractAccessRecordListRequest request);

    CdkExtractViewVo extract(String token, CdkExtractAccessRequest request, HttpServletRequest servletRequest);
}
