package com.winsalty.quickstart.department.service.impl;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.department.dto.DepartmentSaveRequest;
import com.winsalty.quickstart.department.dto.DepartmentStatusRequest;
import com.winsalty.quickstart.department.entity.DepartmentEntity;
import com.winsalty.quickstart.department.mapper.DepartmentMapper;
import com.winsalty.quickstart.department.service.DepartmentService;
import com.winsalty.quickstart.department.vo.DepartmentVo;
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
 * 维护部门树结构，并为用户管理提供部门归属数据。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentServiceImpl.class);
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final String EMPTY_TEXT = "";

    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    /**
     * 查询部门树。过滤后若子节点父级不在结果集中，会提升为根节点。
     */
    @Override
    public List<DepartmentVo> getTree(String keyword, String status) {
        List<DepartmentEntity> entities = departmentMapper.findAll(keyword, status);
        log.info("department tree loaded, keyword={}, status={}, size={}", keyword, status, entities.size());
        return buildTree(entities);
    }

    /**
     * 新增或编辑部门，校验父级存在、自身不能作为父级、部门编码唯一。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVo save(DepartmentSaveRequest request) {
        Long parentId = parseOptionalId(request.getParentId());
        validateParent(parentId, request.getId());
        DepartmentEntity duplicated = departmentMapper.findByCode(request.getCode());
        DepartmentEntity entity = StringUtils.hasText(request.getId()) ? load(parseId(request.getId())) : new DepartmentEntity();
        if (duplicated != null && (entity.getId() == null || !duplicated.getId().equals(entity.getId()))) {
            // 部门编码用于系统管理页和用户归属查询，必须全局唯一。
            throw new BusinessException(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS);
        }
        applyFields(entity, request, parentId);
        if (entity.getId() == null) {
            departmentMapper.insert(entity);
            log.info("department created, id={}, code={}", entity.getId(), entity.getCode());
        } else {
            departmentMapper.update(entity);
            log.info("department updated, id={}, code={}", entity.getId(), entity.getCode());
        }
        return toVo(load(entity.getId()));
    }

    /**
     * 更新部门状态，不级联修改子部门，保留给业务侧自行控制。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVo updateStatus(DepartmentStatusRequest request) {
        Long id = parseId(request.getId());
        DepartmentEntity existed = load(id);
        departmentMapper.updateStatus(id, request.getStatus());
        log.info("department status updated, id={}, status={}", id, request.getStatus());
        return toVo(load(id));
    }

    /**
     * 校验父部门合法性，避免形成直接自引用。
     */
    private void validateParent(Long parentId, String currentId) {
        if (parentId == null) {
            return;
        }
        DepartmentEntity parent = departmentMapper.findById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_PARENT_NOT_FOUND);
        }
        if (StringUtils.hasText(currentId) && parentId.equals(parseId(currentId))) {
            // 当前只拦截直接自引用，避免最常见的树结构错误。
            throw new BusinessException(ErrorCode.DEPARTMENT_PARENT_SELF);
        }
    }

    /**
     * 保存部门实体字段，空联系方式统一落空字符串。
     */
    private void applyFields(DepartmentEntity entity, DepartmentSaveRequest request, Long parentId) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setParentId(parentId);
        // 排序为空时按 0 处理，树构建和 SQL 排序都不需要额外处理 null。
        entity.setSortOrder(request.getSortOrder() == null ? DEFAULT_SORT_ORDER : request.getSortOrder());
        entity.setLeader(defaultText(request.getLeader()));
        entity.setPhone(defaultText(request.getPhone()));
        entity.setEmail(defaultText(request.getEmail()));
        entity.setStatus(request.getStatus());
    }

    /**
     * 将扁平部门列表组装为树。
     */
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
                // 查询条件过滤掉父部门时，将子部门提升为根节点，方便前端仍然展示命中结果。
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
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_ID);
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
        vo.setParentId(entity.getParentId() == null ? EMPTY_TEXT : String.valueOf(entity.getParentId()));
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
        return StringUtils.hasText(value) ? value.trim() : EMPTY_TEXT;
    }
}
