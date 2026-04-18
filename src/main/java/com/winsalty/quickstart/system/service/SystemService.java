package com.winsalty.quickstart.system.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dto.SystemListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuSaveRequest;
import com.winsalty.quickstart.system.dto.SystemStatusRequest;
import com.winsalty.quickstart.system.dto.SystemSaveRequest;
import com.winsalty.quickstart.system.dto.UserRoleAssignRequest;
import com.winsalty.quickstart.system.vo.SystemMenuVo;
import com.winsalty.quickstart.system.vo.SystemRecordVo;

import java.util.List;

/**
 * 系统管理服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface SystemService {

    PageResponse<SystemRecordVo> getPage(SystemListRequest request);

    SystemRecordVo getDetail(String id);

    SystemRecordVo save(SystemSaveRequest request);

    SystemRecordVo updateStatus(SystemStatusRequest request);

    List<SystemMenuVo> getMenuTree(SystemMenuListRequest request);

    SystemMenuVo saveMenu(SystemMenuSaveRequest request);

    SystemMenuVo updateMenuStatus(SystemStatusRequest request);

    SystemRecordVo assignUserRoles(UserRoleAssignRequest request);
}
