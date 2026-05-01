package com.winsalty.quickstart.credential.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.credential.dto.CredentialExtractAccessRecordListRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkDisableRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkExtendRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkListRequest;
import com.winsalty.quickstart.credential.vo.CredentialExtractAccessRecordVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCopyVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCreateResultVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 凭证提取链接服务接口。
 * 定义管理端链接列表、详情、复制、停用、延期和访问审计能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
public interface CredentialExtractLinkService {

    /** 分页查询提取链接。 */
    PageResponse<CredentialExtractLinkVo> listLinks(CredentialExtractLinkListRequest request);

    /** 查询提取链接详情。 */
    CredentialExtractLinkVo getLink(Long id);

    /** 查询提取链接包含的凭证明细。 */
    List<CredentialItemVo> listItems(Long id);

    /** 查询提取链接访问记录。 */
    PageResponse<CredentialExtractAccessRecordVo> listAccessRecords(Long id, CredentialExtractAccessRecordListRequest request);

    /** 复制提取链接公开 URL。 */
    CredentialExtractLinkCopyVo copyUrl(Long id, HttpServletRequest servletRequest);

    /** 停用提取链接。 */
    CredentialExtractLinkVo disableLink(Long id, CredentialExtractLinkDisableRequest request, HttpServletRequest servletRequest);

    /** 延期提取链接。 */
    CredentialExtractLinkVo extendLink(Long id, CredentialExtractLinkExtendRequest request, HttpServletRequest servletRequest);

    /** 按批次批量生成提取链接。 */
    CredentialExtractLinkCreateResultVo createBatchLinks(Long batchId, CredentialExtractLinkCreateRequest request);

    /** 按单个凭证明细生成提取链接。 */
    CredentialExtractLinkCreateResultVo createItemLink(Long itemId, CredentialExtractLinkCreateRequest request);

    /** 补发提取链接并停用旧链接。 */
    CredentialExtractLinkCopyVo reissueLink(Long id, CredentialExtractLinkCreateRequest request, HttpServletRequest servletRequest);
}
