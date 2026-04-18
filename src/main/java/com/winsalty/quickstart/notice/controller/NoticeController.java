package com.winsalty.quickstart.notice.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.notice.dto.NoticeListRequest;
import com.winsalty.quickstart.notice.dto.NoticeSaveRequest;
import com.winsalty.quickstart.notice.dto.NoticeStatusRequest;
import com.winsalty.quickstart.notice.service.NoticeService;
import com.winsalty.quickstart.notice.vo.NoticeVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 公告通知控制器。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/system/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<NoticeVo>> list(@Validated NoticeListRequest request) {
        return ApiResponse.success("获取成功", noticeService.getPage(request));
    }

    @GetMapping("/detail")
    public ApiResponse<NoticeVo> detail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", noticeService.getDetail(id));
    }

    @AuditLog(logType = "operation", code = "notice_save", name = "保存公告")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<NoticeVo> save(@Valid @RequestBody NoticeSaveRequest request) {
        return ApiResponse.success("保存成功", noticeService.save(request));
    }

    @AuditLog(logType = "operation", code = "notice_status", name = "更新公告状态")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/status")
    public ApiResponse<NoticeVo> status(@Valid @RequestBody NoticeStatusRequest request) {
        return ApiResponse.success("状态已更新", noticeService.updateStatus(request));
    }

    @GetMapping("/active")
    public ApiResponse<List<NoticeVo>> active() {
        return ApiResponse.success("获取成功", noticeService.getActiveNotices());
    }
}
