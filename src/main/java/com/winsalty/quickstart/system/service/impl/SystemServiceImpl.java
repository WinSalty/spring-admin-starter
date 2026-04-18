package com.winsalty.quickstart.system.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.service.LogService;
import com.winsalty.quickstart.system.dto.SystemListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuSaveRequest;
import com.winsalty.quickstart.system.dto.SystemSaveRequest;
import com.winsalty.quickstart.system.dto.SystemStatusRequest;
import com.winsalty.quickstart.system.dto.UserRoleAssignRequest;
import com.winsalty.quickstart.system.entity.SystemMenuEntity;
import com.winsalty.quickstart.system.entity.SystemRecordEntity;
import com.winsalty.quickstart.system.mapper.SystemMapper;
import com.winsalty.quickstart.system.service.SystemService;
import com.winsalty.quickstart.system.vo.SystemMenuVo;
import com.winsalty.quickstart.system.vo.SystemRecordVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private static final String BOOTSTRAP_VERSION_KEY = "sa:cache:ver:bootstrap";
    private static final String DICT_VERSION_KEY = "sa:cache:ver:dict";
    private static final String DICT_CACHE_KEY_PREFIX = "sa:dict:v";
    private static final long DICT_CACHE_TTL_SECONDS = 3600L;

    private final SystemMapper systemMapper;
    private final RedisCacheService redisCacheService;
    private final LogService logService;
    private final BCryptPasswordEncoder passwordEncoder;

    public SystemServiceImpl(SystemMapper systemMapper,
                             RedisCacheService redisCacheService,
                             LogService logService,
                             BCryptPasswordEncoder passwordEncoder) {
        this.systemMapper = systemMapper;
        this.redisCacheService = redisCacheService;
        this.logService = logService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResponse<SystemRecordVo> getPage(SystemListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        if ("dicts".equals(request.getModuleKey()) && !StringUtils.hasText(request.getLogType())) {
            long version = currentVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            String cacheKey = DICT_CACHE_KEY_PREFIX + version + ":list";
            Object cached = redisCacheService.get(cacheKey);
            List<SystemRecordVo> records;
            if (cached instanceof List) {
                records = (List<SystemRecordVo>) cached;
                log.info("system dict cache hit, cacheKey={}, size={}", cacheKey, records.size());
            } else {
                List<SystemRecordEntity> entities = systemMapper.findPage(request.getModuleKey(), null, null, null, 0, Integer.MAX_VALUE);
                records = toVoList(entities);
                redisCacheService.set(cacheKey, records, DICT_CACHE_TTL_SECONDS);
                log.info("system dict cache refreshed, cacheKey={}, size={}", cacheKey, records.size());
            }
            List<SystemRecordVo> filtered = filterDictPage(records, request.getKeyword(), request.getStatus());
            int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
            int toIndex = Math.min(fromIndex + pageSize, filtered.size());
            return new PageResponse<SystemRecordVo>(filtered.subList(fromIndex, toIndex), pageNo, pageSize, filtered.size());
        }
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
            if ("users".equals(existed.getModuleKey())) {
                assignRoles(existed.getId(), resolveRequestedRoleCodes(request));
            }
            if ("dicts".equals(existed.getModuleKey())) {
                long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
                log.info("system dict cache version bumped after save, id={}, version={}", existed.getRecordCode(), version);
            }
            log.info("system record updated, moduleKey={}, id={}, code={}", existed.getModuleKey(), existed.getRecordCode(), existed.getCode());
            recordSystemLog("operation", request.getOwner(), request.getCode(), "系统记录更新成功", request.getModuleKey(), "成功");
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
        if ("users".equals(entity.getModuleKey())) {
            assignRoles(entity.getId(), resolveRequestedRoleCodes(request));
        }
        if ("dicts".equals(entity.getModuleKey())) {
            long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            log.info("system dict cache version bumped after create, id={}, version={}", entity.getRecordCode(), version);
        }
        log.info("system record created, moduleKey={}, id={}, code={}", entity.getModuleKey(), entity.getRecordCode(), entity.getCode());
        recordSystemLog("operation", request.getOwner(), request.getCode(), "系统记录创建成功", request.getModuleKey(), "成功");
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
        if ("dicts".equals(existed.getModuleKey())) {
            long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            log.info("system dict cache version bumped after status update, id={}, version={}", existed.getRecordCode(), version);
        }
        log.info("system status updated, moduleKey={}, id={}, status={}", existed.getModuleKey(), existed.getRecordCode(), request.getStatus());
        recordSystemLog("operation", existed.getOwner(), existed.getCode(), "系统记录状态更新成功", existed.getModuleKey(), "成功");
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
            long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            log.info("system menu updated, id={}, code={}, bootstrapCacheVersion={}", existed.getId(), existed.getCode(), version);
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
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("system menu created, id={}, code={}, bootstrapCacheVersion={}", entity.getId(), entity.getCode(), version);
        recordSystemLog("operation", entity.getOwner(), entity.getCode(), "菜单创建成功", "menus", "成功");
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
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("system menu status updated, id={}, status={}, bootstrapCacheVersion={}", existed.getId(), request.getStatus(), version);
        recordSystemLog("operation", existed.getOwner(), existed.getCode(), "菜单状态更新成功", "menus", "成功");
        return toMenuVo(loadMenuById(existed.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemRecordVo assignUserRoles(UserRoleAssignRequest request) {
        SystemRecordEntity user = loadWritableRecord(request.getUserId());
        if (!"users".equals(user.getModuleKey())) {
            throw new BusinessException(4050, "只能为用户分配角色");
        }
        assignRoles(user.getId(), request.getRoleCodes());
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("user roles assigned, userId={}, roleCodes={}, bootstrapCacheVersion={}", user.getId(), request.getRoleCodes(), version);
        recordSystemLog("operation", user.getOwner(), user.getCode(), "用户角色分配成功", "users", "成功");
        return toVo(loadWritableRecord(user.getRecordCode()));
    }

    private long currentVersion(String versionKey, long ttlSeconds) {
        Object cached = redisCacheService.get(versionKey);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        redisCacheService.set(versionKey, 1L, ttlSeconds * 24);
        return 1L;
    }

    private long nextVersion(String versionKey, long ttlSeconds) {
        Long version = redisCacheService.increment(versionKey);
        if (version == null) {
            redisCacheService.set(versionKey, 1L, ttlSeconds * 24);
            return 1L;
        }
        return version.longValue();
    }

    private List<SystemRecordVo> filterDictPage(List<SystemRecordVo> records, String keyword, String status) {
        List<SystemRecordVo> filtered = new ArrayList<SystemRecordVo>();
        for (SystemRecordVo record : records) {
            if (StringUtils.hasText(status) && !status.equals(record.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = keyword.trim().toLowerCase();
                if (!containsText(record.getName(), normalizedKeyword)
                        && !containsText(record.getCode(), normalizedKeyword)
                        && !containsText(record.getOwner(), normalizedKeyword)
                        && !containsText(record.getDescription(), normalizedKeyword)) {
                    continue;
                }
            }
            filtered.add(record);
        }
        return filtered;
    }

    private boolean containsText(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
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
            entity.setDepartmentId(resolveDepartmentId(request.getDepartmentId(), request.getOwner()));
            entity.setRoleCodes(passwordEncoder.encode("SpringAdmin@2026"));
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
        vo.setDepartmentId(entity.getDepartmentId() == null ? "" : String.valueOf(entity.getDepartmentId()));
        vo.setRoleNames(entity.getRoleNames());
        vo.setRoleCodes(entity.getRoleCodes());
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
        vo.setDeviceInfo(entity.getDeviceInfo());
        vo.setRequestInfo(entity.getRequestInfo());
        vo.setResponseInfo(entity.getResponseInfo());
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

    private void recordSystemLog(String logType, String owner, String code, String description, String target, String result) {
        OperationLogRequest request = new OperationLogRequest();
        request.setLogType(logType);
        request.setOwner(owner);
        request.setName(description);
        request.setCode(code);
        request.setDescription(description);
        request.setTarget(target);
        request.setIpAddress("127.0.0.1");
        request.setResult(result);
        request.setDurationMs(0L);
        logService.record(request);
    }

    private Long resolveDepartmentId(String departmentId, String owner) {
        if (StringUtils.hasText(departmentId)) {
            try {
                Long id = Long.valueOf(departmentId.trim());
                if (systemMapper.findDepartmentIdById(id) == null) {
                    throw new BusinessException(4045, "部门不存在");
                }
                return id;
            } catch (NumberFormatException exception) {
                throw new BusinessException(4001, "部门ID不合法");
            }
        }
        if (StringUtils.hasText(owner)) {
            return systemMapper.findDepartmentIdByKeyword(owner.trim());
        }
        return null;
    }

    private List<String> resolveRequestedRoleCodes(SystemSaveRequest request) {
        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            return request.getRoleCodes();
        }
        List<String> roleCodes = new ArrayList<String>();
        if (StringUtils.hasText(request.getExtraValue())) {
            String[] values = request.getExtraValue().split(",");
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    roleCodes.add(value.trim());
                }
            }
        }
        if (roleCodes.isEmpty()) {
            roleCodes.add("viewer");
        }
        return roleCodes;
    }

    private void assignRoles(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(4049, "至少需要分配一个角色");
        }
        List<Long> roleIds = new ArrayList<Long>();
        for (String roleCode : roleCodes) {
            if (!StringUtils.hasText(roleCode)) {
                continue;
            }
            Long roleId = systemMapper.findRoleIdByCode(roleCode.trim());
            if (roleId == null) {
                roleId = systemMapper.findRoleIdByName(roleCode.trim());
            }
            if (roleId == null) {
                throw new BusinessException(4043, "角色不存在：" + roleCode);
            }
            if (!roleIds.contains(roleId)) {
                roleIds.add(roleId);
            }
        }
        if (roleIds.isEmpty()) {
            throw new BusinessException(4049, "至少需要分配一个角色");
        }
        systemMapper.deleteUserRoles(userId);
        for (Long roleId : roleIds) {
            systemMapper.insertUserRole(userId, roleId);
        }
        nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
    }
}
