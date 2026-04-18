package com.winsalty.quickstart.department.service;

import com.winsalty.quickstart.department.dto.DepartmentSaveRequest;
import com.winsalty.quickstart.department.dto.DepartmentStatusRequest;
import com.winsalty.quickstart.department.vo.DepartmentVo;

import java.util.List;

/**
 * 部门服务。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public interface DepartmentService {

    List<DepartmentVo> getTree(String keyword, String status);

    DepartmentVo save(DepartmentSaveRequest request);

    DepartmentVo updateStatus(DepartmentStatusRequest request);
}
