package com.winsalty.quickstart.department.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.department.dto.DepartmentSaveRequest;
import com.winsalty.quickstart.department.dto.DepartmentStatusRequest;
import com.winsalty.quickstart.department.entity.DepartmentEntity;
import com.winsalty.quickstart.department.mapper.DepartmentMapper;
import com.winsalty.quickstart.department.service.DepartmentService;
import com.winsalty.quickstart.department.vo.DepartmentVo;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务实现。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);

    private final DepartmentMapper departmentMapper;
    private final LogService logService;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper, LogService logService) {
        this.departmentMapper = departmentMapper;
        this.logService = logService;
    }

    @Override
    public List<DepartmentVo> getTree(String keyword, String status) {
        List<DepartmentEntity> entities = departmentMapper.findAll(keyword, status);
        log.info("department tree loaded, keyword={}, status={}, size={}", keyword, status, entities.size());
        return buildTree(entities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVo save(DepartmentSaveRequest request) {
        Long parentId = parseOptionalId(request.getParentId());
        validateParent(parentId, request.getId());
        DepartmentEntity duplicated = departmentMapper.findByCode(request.getCode());
        DepartmentEntity entity = StringUtils.hasText(request.getId()) ? load(parseId(request.getId())) : new DepartmentEntity();
        if (duplicated != null && (entity.getId() == null || !duplicated.getId().equals(entity.getId()))) {
            throw new BusinessException(4047, "部门编码已存在");
        }
        applyFields(entity, request, parentId);
        if (entity.getId() == null) {
            departmentMapper.insert(entity);
            recordLog("department_create", "部门创建成功", entity.getName());
            log.info("department created, id={}, code={}", entity.getId(), entity.getCode());
        } else {
            departmentMapper.update(entity);
            recordLog("department_update", "部门更新成功", entity.getName());
            log.info("department updated, id={}, code={}", entity.getId(), entity.getCode());
        }
        return toVo(load(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVo updateStatus(DepartmentStatusRequest request) {
        Long id = parseId(request.getId());
        DepartmentEntity existed = load(id);
        departmentMapper.updateStatus(id, request.getStatus());
        recordLog("department_status_update", "部门状态更新成功", existed.getName());
        log.info("department status updated, id={}, status={}", id, request.getStatus());
        return toVo(load(id));
    }

    private void validateParent(Long parentId, String currentId) {
        if (parentId == null) {
            return;
        }
        DepartmentEntity parent = departmentMapper.findById(parentId);
        if (parent == null) {
            throw new BusinessException(4046, "父级部门不存在");
        }
        if (StringUtils.hasText(currentId) && parentId.equals(parseId(currentId))) {
            throw new BusinessException(4048, "父级部门不能选择自身");
        }
    }

    private void applyFields(DepartmentEntity entity, DepartmentSaveRequest request, Long parentId) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setParentId(parentId);
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setLeader(defaultText(request.getLeader()));
        entity.setPhone(defaultText(request.getPhone()));
        entity.setEmail(defaultText(request.getEmail()));
        entity.setStatus(request.getStatus());
    }

    private List<DepartmentVo> buildTree(List<DepartmentEntity> entities) {
        Map<String, DepartmentVo> map = new LinkedHashMap<String, DepartmentVo>();
        List<DepartmentVo> roots = new ArrayList<DepartmentVo>();
        for (DepartmentEntity entity : entities) {
            DepartmentVo vo = toVo(entity);
            map.put(vo.getId(), vo);
        }
        for (DepartmentVo item : map.values()) {
            if (!StringUtils.hasText(item.getParentId())) {
                roots.add(item);
                continue;
            }
            DepartmentVo parent = map.get(item.getParentId());
            if (parent == null) {
                roots.add(item);
                continue;
            }
            parent.getChildren().add(item);
        }
        return roots;
    }

    private DepartmentEntity load(Long id) {
        DepartmentEntity entity = departmentMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(4045, "部门不存在");
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(4001, "id 不合法");
        }
    }

    private Long parseOptionalId(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        return parseId(id);
    }

    private DepartmentVo toVo(DepartmentEntity entity) {
        DepartmentVo vo = new DepartmentVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setParentId(entity.getParentId() == null ? "" : String.valueOf(entity.getParentId()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setLeader(entity.getLeader());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private void recordLog(String code, String description, String target) {
        OperationLogRequest request = new OperationLogRequest();
        request.setLogType("operation");
        request.setOwner("system");
        request.setName(description);
        request.setCode(code);
        request.setDescription(description);
        request.setTarget(target);
        request.setIpAddress("127.0.0.1");
        request.setResult("成功");
        request.setDurationMs(0L);
        logService.record(request);
    }
}
