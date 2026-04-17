package com.winsalty.quickstart.system.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.system.dto.SystemListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuSaveRequest;
import com.winsalty.quickstart.system.dto.SystemSaveRequest;
import com.winsalty.quickstart.system.dto.SystemStatusRequest;
import com.winsalty.quickstart.system.entity.SystemMenuEntity;
import com.winsalty.quickstart.system.entity.SystemRecordEntity;
import com.winsalty.quickstart.system.mapper.SystemMapper;
import com.winsalty.quickstart.system.service.SystemService;
import com.winsalty.quickstart.system.vo.SystemMenuVo;
import com.winsalty.quickstart.system.vo.SystemRecordVo;
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
 * 系统管理服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class SystemServiceImpl implements SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemServiceImpl.class);

    private final SystemMapper systemMapper;

    public SystemServiceImpl(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    @Override
    public PageResponse<SystemRecordVo> getPage(SystemListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<SystemRecordEntity> entities = systemMapper.findPage(request.getModuleKey(), request.getKeyword(), request.getStatus(), request.getLogType(), offset, pageSize);
        long total = systemMapper.countPage(request.getModuleKey(), request.getKeyword(), request.getStatus(), request.getLogType());
        log.info("system page loaded, moduleKey={}, pageNo={}, pageSize={}, total={}", request.getModuleKey(), pageNo, pageSize, total);
        return new PageResponse<SystemRecordVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public SystemRecordVo getDetail(String id) {
        SystemRecordEntity entity = systemMapper.findByRecordCode(id);
        if (entity == null) {
            throw new BusinessException(4042, "系统记录不存在");
        }
        log.info("system detail loaded, moduleKey={}, id={}", entity.getModuleKey(), id);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemRecordVo save(SystemSaveRequest request) {
        SystemRecordEntity duplicated = systemMapper.findWritableByModuleAndCode(request.getModuleKey(), request.getCode());
        if (StringUtils.hasText(request.getId())) {
            SystemRecordEntity existed = systemMapper.findByRecordCode(request.getId());
            if (existed == null) {
                throw new BusinessException(4042, "系统记录不存在");
            }
            if (!request.getModuleKey().equals(existed.getModuleKey())) {
                throw new BusinessException(4009, "模块类型不匹配");
            }
            if (duplicated != null && !duplicated.getRecordCode().equals(existed.getRecordCode())) {
                throw new BusinessException(4010, "记录编码已存在");
            }
            applyCommonFields(existed, request);
            applyModuleFields(existed, request);
            updateWritable(existed);
            log.info("system record updated, moduleKey={}, id={}, code={}", existed.getModuleKey(), existed.getRecordCode(), existed.getCode());
            return toVo(loadWritableRecord(existed.getRecordCode()));
        }

        if (duplicated != null) {
            throw new BusinessException(4010, "记录编码已存在");
        }
        SystemRecordEntity entity = new SystemRecordEntity();
        entity.setRecordCode(nextRecordCode(request.getModuleKey()));
        entity.setModuleKey(request.getModuleKey());
        applyCommonFields(entity, request);
        applyModuleFields(entity, request);
        insertWritable(entity);
        log.info("system record created, moduleKey={}, id={}, code={}", entity.getModuleKey(), entity.getRecordCode(), entity.getCode());
        return toVo(loadWritableRecord(entity.getRecordCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemRecordVo updateStatus(SystemStatusRequest request) {
        SystemRecordEntity existed = systemMapper.findByRecordCode(request.getId());
        if (existed == null) {
            throw new BusinessException(4042, "系统记录不存在");
        }
        if ("logs".equals(existed.getModuleKey())) {
            throw new BusinessException(4011, "日志模块不支持状态变更");
        }
        updateWritableStatus(existed, request.getStatus());
        log.info("system status updated, moduleKey={}, id={}, status={}", existed.getModuleKey(), existed.getRecordCode(), request.getStatus());
        return toVo(loadWritableRecord(existed.getRecordCode()));
    }

    @Override
    public List<SystemMenuVo> getMenuTree(SystemMenuListRequest request) {
        List<SystemMenuEntity> menus = systemMapper.findMenus(request.getKeyword(), request.getStatus());
        log.info("system menu tree loaded, keyword={}, status={}, size={}", request.getKeyword(), request.getStatus(), menus.size());
        return buildMenuTree(menus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemMenuVo saveMenu(SystemMenuSaveRequest request) {
        SystemMenuEntity duplicated = systemMapper.findMenuByCode(request.getCode());
        Long parentId = parseParentId(request.getParentId());
        validateMenuFields(request, parentId);
        if (StringUtils.hasText(request.getId())) {
            SystemMenuEntity existed = systemMapper.findMenuById(parseRequiredId(request.getId()));
            if (existed == null) {
                throw new BusinessException(4042, "菜单记录不存在");
            }
            if (duplicated != null && !duplicated.getRecordCode().equals(existed.getRecordCode())) {
                throw new BusinessException(4010, "菜单编码已存在");
            }
            if (parentId != null && parentId.equals(existed.getId())) {
                throw new BusinessException(4014, "父级菜单不能选择自身");
            }
            existed.setParentId(parentId);
            applyMenuFields(existed, request);
            existed.setRouteCode(resolveRouteCode(request.getRoutePath()));
            systemMapper.updateMenu(existed);
            log.info("system menu updated, id={}, code={}", existed.getId(), existed.getCode());
            return toMenuVo(loadMenuById(existed.getId()));
        }

        if (duplicated != null) {
            throw new BusinessException(4010, "菜单编码已存在");
        }
        SystemMenuEntity entity = new SystemMenuEntity();
        entity.setRecordCode(nextRecordCode("menus"));
        entity.setParentId(parentId);
        applyMenuFields(entity, request);
        entity.setRouteCode(resolveRouteCode(request.getRoutePath()));
        systemMapper.insertMenu(entity);
        log.info("system menu created, id={}, code={}", entity.getId(), entity.getCode());
        return toMenuVo(loadMenuById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemMenuVo updateMenuStatus(SystemStatusRequest request) {
        SystemMenuEntity existed = systemMapper.findMenuById(parseRequiredId(request.getId()));
        if (existed == null) {
            throw new BusinessException(4042, "菜单记录不存在");
        }
        systemMapper.updateMenuStatus(existed.getId(), request.getStatus());
        log.info("system menu status updated, id={}, status={}", existed.getId(), request.getStatus());
        return toMenuVo(loadMenuById(existed.getId()));
    }

    private void validateMenuFields(SystemMenuSaveRequest request, Long parentId) {
        if (parentId != null) {
            SystemMenuEntity parent = systemMapper.findMenuById(parentId);
            if (parent == null) {
                throw new BusinessException(4015, "父级菜单不存在");
            }
        }
        if ("external".equals(request.getMenuType()) && !StringUtils.hasText(request.getExternalLink())) {
            throw new BusinessException(4016, "外链菜单必须填写 externalLink");
        }
    }

    private void applyMenuFields(SystemMenuEntity entity, SystemMenuSaveRequest request) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setStatus(request.getStatus());
        entity.setOwner("平台技术部");
        entity.setDescription(request.getDescription());
        entity.setMenuType(request.getMenuType());
        entity.setIcon(defaultText(request.getIcon()));
        entity.setRoutePath(defaultText(request.getRoutePath()));
        entity.setPermissionCode(defaultText(request.getPermissionCode()));
        entity.setExternalLink(defaultText(request.getExternalLink()));
        entity.setOrderNo(request.getOrderNo());
    }

    private Long parseParentId(String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        try {
            return Long.valueOf(parentId.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(4015, "父级菜单不存在");
        }
    }

    private String resolveRouteCode(String routePath) {
        if (!StringUtils.hasText(routePath)) {
            return null;
        }
        String path = routePath.trim();
        String[] segments = path.split("/");
        for (int index = segments.length - 1; index >= 0; index--) {
            if (StringUtils.hasText(segments[index])) {
                return segments[index];
            }
        }
        return null;
    }

    private SystemMenuEntity loadMenuById(Long id) {
        SystemMenuEntity entity = systemMapper.findMenuById(id);
        if (entity == null) {
            throw new BusinessException(4042, "菜单记录不存在");
        }
        return entity;
    }

    private Long parseRequiredId(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(4042, "菜单记录不存在");
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(4042, "菜单记录不存在");
        }
    }

    private SystemRecordEntity loadWritableRecord(String recordCode) {
        SystemRecordEntity entity = systemMapper.findByRecordCode(recordCode);
        if (entity == null) {
            throw new BusinessException(4042, "系统记录不存在");
        }
        return entity;
    }

    private void insertWritable(SystemRecordEntity entity) {
        if ("users".equals(entity.getModuleKey())) {
            systemMapper.insertUser(entity);
            return;
        }
        if ("roles".equals(entity.getModuleKey())) {
            systemMapper.insertRole(entity);
            return;
        }
        if ("dicts".equals(entity.getModuleKey())) {
            systemMapper.insertDict(entity);
            return;
        }
        throw new BusinessException(4009, "模块类型不支持保存");
    }

    private void updateWritable(SystemRecordEntity entity) {
        if ("users".equals(entity.getModuleKey())) {
            systemMapper.updateUser(entity);
            return;
        }
        if ("roles".equals(entity.getModuleKey())) {
            systemMapper.updateRole(entity);
            return;
        }
        if ("dicts".equals(entity.getModuleKey())) {
            systemMapper.updateDict(entity);
            return;
        }
        throw new BusinessException(4009, "模块类型不支持保存");
    }

    private void updateWritableStatus(SystemRecordEntity entity, String status) {
        if ("users".equals(entity.getModuleKey())) {
            systemMapper.updateUserStatus(entity.getId(), status);
            return;
        }
        if ("roles".equals(entity.getModuleKey())) {
            systemMapper.updateRoleStatus(entity.getId(), status);
            return;
        }
        if ("dicts".equals(entity.getModuleKey())) {
            systemMapper.updateDictStatus(entity.getId(), status);
            return;
        }
        throw new BusinessException(4009, "模块类型不支持状态变更");
    }

    private void applyCommonFields(SystemRecordEntity entity, SystemSaveRequest request) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setStatus(request.getStatus());
        entity.setOwner(request.getOwner());
        entity.setDescription(request.getDescription());
    }

    private void applyModuleFields(SystemRecordEntity entity, SystemSaveRequest request) {
        if ("users".equals(request.getModuleKey())) {
            if (!StringUtils.hasText(entity.getDepartment())) {
                entity.setDepartment(request.getOwner());
            }
            entity.setRoleNames(defaultText(request.getExtraValue()));
            return;
        }
        if ("roles".equals(request.getModuleKey())) {
            entity.setDataScope(defaultText(request.getExtraValue()));
            if (entity.getUserCount() == null) {
                entity.setUserCount(0L);
            }
            return;
        }
        if ("dicts".equals(request.getModuleKey())) {
            entity.setDictType(defaultText(request.getExtraValue()));
            if (entity.getItemCount() == null) {
                entity.setItemCount(0L);
            }
            if (!StringUtils.hasText(entity.getCacheKey())) {
                entity.setCacheKey("dict:" + request.getCode());
            }
            return;
        }
        throw new BusinessException(4009, "模块类型不支持保存");
    }

    private String nextRecordCode(String moduleKey) {
        String prefix = "S";
        if ("users".equals(moduleKey)) {
            prefix = "U";
        } else if ("roles".equals(moduleKey)) {
            prefix = "R";
        } else if ("dicts".equals(moduleKey)) {
            prefix = "D";
        } else if ("logs".equals(moduleKey)) {
            prefix = "L";
        } else if ("menus".equals(moduleKey)) {
            prefix = "M";
        }
        return prefix + System.currentTimeMillis();
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private List<SystemRecordVo> toVoList(List<SystemRecordEntity> entities) {
        List<SystemRecordVo> records = new ArrayList<SystemRecordVo>();
        for (SystemRecordEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private SystemRecordVo toVo(SystemRecordEntity entity) {
        SystemRecordVo vo = new SystemRecordVo();
        vo.setId(entity.getRecordCode());
        vo.setModuleKey(entity.getModuleKey());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setStatus(entity.getStatus());
        vo.setOwner(entity.getOwner());
        vo.setDescription(entity.getDescription());
        vo.setDepartment(entity.getDepartment());
        vo.setRoleNames(entity.getRoleNames());
        vo.setLastLoginAt(entity.getLastLoginAt());
        vo.setDataScope(entity.getDataScope());
        vo.setUserCount(entity.getUserCount());
        vo.setDictType(entity.getDictType());
        vo.setItemCount(entity.getItemCount());
        vo.setCacheKey(entity.getCacheKey());
        vo.setLogType(resolveLogTypeName(entity.getLogType()));
        vo.setTarget(entity.getTarget());
        vo.setIpAddress(entity.getIpAddress());
        vo.setResult(entity.getResult());
        vo.setDurationMs(entity.getDurationMs());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<SystemMenuVo> buildMenuTree(List<SystemMenuEntity> entities) {
        Map<String, SystemMenuVo> menuMap = new LinkedHashMap<String, SystemMenuVo>();
        List<SystemMenuVo> roots = new ArrayList<SystemMenuVo>();
        for (SystemMenuEntity entity : entities) {
            SystemMenuVo vo = toMenuVo(entity);
            menuMap.put(vo.getId(), vo);
        }
        for (SystemMenuVo menu : menuMap.values()) {
            if (!StringUtils.hasText(menu.getParentId())) {
                roots.add(menu);
                continue;
            }
            SystemMenuVo parent = menuMap.get(menu.getParentId());
            if (parent == null) {
                roots.add(menu);
                continue;
            }
            parent.getChildren().add(menu);
        }
        return roots;
    }

    private SystemMenuVo toMenuVo(SystemMenuEntity entity) {
        SystemMenuVo vo = new SystemMenuVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setModuleKey("menus");
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setStatus(entity.getStatus());
        vo.setOwner(entity.getOwner());
        vo.setDescription(entity.getDescription());
        vo.setParentId(entity.getParentId() == null ? "" : String.valueOf(entity.getParentId()));
        vo.setMenuType(entity.getMenuType());
        vo.setIcon(entity.getIcon());
        vo.setRoutePath(entity.getRoutePath());
        vo.setPermissionCode(entity.getPermissionCode());
        vo.setExternalLink(entity.getExternalLink());
        vo.setOrderNo(entity.getOrderNo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String resolveLogTypeName(String logType) {
        if ("login".equals(logType)) {
            return "登录日志";
        }
        if ("operation".equals(logType)) {
            return "操作日志";
        }
        if ("api".equals(logType)) {
            return "接口日志";
        }
        return logType;
    }
}
