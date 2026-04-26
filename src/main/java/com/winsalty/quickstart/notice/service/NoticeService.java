package com.winsalty.quickstart.notice.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.notice.dto.NoticeListRequest;
import com.winsalty.quickstart.notice.dto.NoticeSaveRequest;
import com.winsalty.quickstart.notice.dto.NoticeStatusRequest;
import com.winsalty.quickstart.notice.vo.NoticeVo;

import java.util.List;

/**
 * 公告通知服务。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public interface NoticeService {

    PageResponse<NoticeVo> getPage(NoticeListRequest request);

    NoticeVo getDetail(String id);

    NoticeVo save(NoticeSaveRequest request);

    NoticeVo updateStatus(NoticeStatusRequest request);

    List<NoticeVo> getActiveNotices();

    List<NoticeVo> getUnreadRequiredNotices();

    NoticeVo markRead(String id);
}
