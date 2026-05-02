package com.winsalty.quickstart.system.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
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
 * 通过 moduleKey 统一承接前端系统管理页，同时把用户、角色、字典、日志落到真实业务表。
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
    private final BCryptPasswordEncoder passwordEncoder;

    public SystemServiceImpl(SystemMapper systemMapper,
                             RedisCacheService redisCacheService,
                             BCryptPasswordEncoder passwordEncoder) {
        this.systemMapper = systemMapper;
        this.redisCacheService = redisCacheService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 通用分页查询。字典模块使用 Redis 缓存全量列表后在内存中过滤分页。
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageResponse<SystemRecordVo> getPage(SystemListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        if ("dicts".equals(request.getModuleKey()) && !StringUtils.hasText(request.getLogType())) {
            // 旧版系统管理页的字典列表数据量较小，优先走版本化缓存再内存过滤，减少频繁查库。
            long version = currentVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            String cacheKey = DICT_CACHE_KEY_PREFIX + version + ":list";
            Object cached = redisCacheService.get(cacheKey);
            List<SystemRecordVo> records;
            if (cached instanceof List) {
                records = (List<SystemRecordVo>) cached;
                log.info("system dict cache hit, cacheKey={}, size={}", cacheKey, records.size());
            } else {
                // 字典缓存保存全量旧版字典记录，后续关键字和状态筛选都在内存执行。
                List<SystemRecordEntity> entities = systemMapper.findPage(request.getModuleKey(), null, null, null, 0, Integer.MAX_VALUE);
                records = toVoList(entities);
                redisCacheService.set(cacheKey, records, DICT_CACHE_TTL_SECONDS);
                log.info("system dict cache refreshed, cacheKey={}, size={}", cacheKey, records.size());
            }
            List<SystemRecordVo> filtered = filterDictPage(records, request.getKeyword(), request.getStatus());
            int fromIndex = Math.min((pageNo - 1) * pageSize, filtered.size());
            int toIndex = Math.min(fromIndex + pageSize, filtered.size());
            // subList 边界用 Math.min 防御超出最后一页的请求，保证返回空列表而不是抛异常。
            return new PageResponse<SystemRecordVo>(filtered.subList(fromIndex, toIndex), pageNo, pageSize, filtered.size());
        }
        int offset = (pageNo - 1) * pageSize;
        List<SystemRecordEntity> entities = systemMapper.findPage(request.getModuleKey(), request.getKeyword(), request.getStatus(), request.getLogType(), offset, pageSize);
        long total = systemMapper.countPage(request.getModuleKey(), request.getKeyword(), request.getStatus(), request.getLogType());
        log.info("system page loaded, moduleKey={}, pageNo={}, pageSize={}, total={}", request.getModuleKey(), pageNo, pageSize, total);
        return new PageResponse<SystemRecordVo>(toVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 按统一 record_code 读取详情，隐藏各业务表自增主键。
     */
    @Override
    public SystemRecordVo getDetail(String id) {
        SystemRecordEntity entity = systemMapper.findByRecordCode(id);
        if (entity == null) {
            throw new BusinessException(4042, "系统记录不存在");
        }
        log.info("system detail loaded, moduleKey={}, id={}", entity.getModuleKey(), id);
        return toVo(entity);
    }

    /**
     * 通用保存入口：带 id 为编辑，不带 id 为新增。不同 moduleKey 会分发到不同真实表。
     */
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
                // recordCode 是跨模块唯一展示 ID，编辑时仍要校验模块，防止前端串模块提交。
                throw new BusinessException(4009, "模块类型不匹配");
            }
            if (duplicated != null && !duplicated.getRecordCode().equals(existed.getRecordCode())) {
                throw new BusinessException(4010, "记录编码已存在");
            }
            // 公共字段和模块差异字段拆开映射，避免用户、角色、字典互相污染字段。
            applyCommonFields(existed, request);
            applyModuleFields(existed, request);
            updateWritable(existed);
            if ("users".equals(existed.getModuleKey())) {
                // 用户保存后重建角色关系，避免历史角色残留。
                assignRoles(existed.getId(), resolveRequestedRoleCodes(request));
            }
            if ("dicts".equals(existed.getModuleKey())) {
                long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
                log.info("system dict cache version bumped after save, id={}, version={}", existed.getRecordCode(), version);
            }
            log.info("system record updated, moduleKey={}, id={}, code={}", existed.getModuleKey(), existed.getRecordCode(), existed.getCode());
            return toVo(loadWritableRecord(existed.getRecordCode()));
        }

        if (duplicated != null) {
            throw new BusinessException(4010, "记录编码已存在");
        }
        SystemRecordEntity entity = new SystemRecordEntity();
        entity.setRecordCode(nextRecordCode(request.getModuleKey()));
        entity.setModuleKey(request.getModuleKey());
        // 新增路径先写公共字段，再按模块补充用户/角色/字典差异字段。
        applyCommonFields(entity, request);
        applyModuleFields(entity, request);
        insertWritable(entity);
        if ("users".equals(entity.getModuleKey())) {
            // 新增用户默认使用请求角色；未传角色时 resolveRequestedRoleCodes 会兜底 viewer。
            assignRoles(entity.getId(), resolveRequestedRoleCodes(request));
        }
        if ("dicts".equals(entity.getModuleKey())) {
            long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            log.info("system dict cache version bumped after create, id={}, version={}", entity.getRecordCode(), version);
        }
        log.info("system record created, moduleKey={}, id={}, code={}", entity.getModuleKey(), entity.getRecordCode(), entity.getCode());
        return toVo(loadWritableRecord(entity.getRecordCode()));
    }

    /**
     * 状态切换入口。会根据模块刷新对应缓存版本，例如字典缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemRecordVo updateStatus(SystemStatusRequest request) {
        SystemRecordEntity existed = systemMapper.findByRecordCode(request.getId());
        if (existed == null) {
            throw new BusinessException(4042, "系统记录不存在");
        }
        if ("logs".equals(existed.getModuleKey())) {
            // 日志是审计事实记录，不允许通过系统管理页切状态。
            throw new BusinessException(4011, "日志模块不支持状态变更");
        }
        // 状态更新按模块路由到真实表，避免通用 DTO 直接修改错误表。
        updateWritableStatus(existed, request.getStatus());
        if ("dicts".equals(existed.getModuleKey())) {
            long version = nextVersion(DICT_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
            log.info("system dict cache version bumped after status update, id={}, version={}", existed.getRecordCode(), version);
        }
        log.info("system status updated, moduleKey={}, id={}, status={}", existed.getModuleKey(), existed.getRecordCode(), request.getStatus());
        return toVo(loadWritableRecord(existed.getRecordCode()));
    }

    /**
     * 查询菜单树，支持关键字和状态过滤。
     */
    @Override
    public List<SystemMenuVo> getMenuTree(SystemMenuListRequest request) {
        List<SystemMenuEntity> menus = systemMapper.findMenus(request.getKeyword(), request.getStatus());
        log.info("system menu tree loaded, keyword={}, status={}, size={}", request.getKeyword(), request.getStatus(), menus.size());
        return buildMenuTree(menus);
    }

    /**
     * 保存菜单并刷新 bootstrap 缓存版本。菜单路由码会从 routePath 末段自动推导。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemMenuVo saveMenu(SystemMenuSaveRequest request) {
        SystemMenuEntity duplicated = systemMapper.findMenuByCode(request.getCode());
        Long parentId = parseParentId(request.getParentId());
        // 父节点和外链字段先校验，后续新增/编辑共用同一套规则。
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
                // 只禁止直接把自己设为父级；更深层环路在当前扁平菜单管理中暂不递归校验。
                throw new BusinessException(4014, "父级菜单不能选择自身");
            }
            existed.setParentId(parentId);
            // routeCode 从 routePath 自动推导，避免前端同时维护两个容易不一致的字段。
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
        // 新增菜单默认写入平台技术部 owner，与系统内置菜单数据保持一致。
        applyMenuFields(entity, request);
        entity.setRouteCode(resolveRouteCode(request.getRoutePath()));
        systemMapper.insertMenu(entity);
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("system menu created, id={}, code={}, bootstrapCacheVersion={}", entity.getId(), entity.getCode(), version);
        return toMenuVo(loadMenuById(entity.getId()));
    }

    /**
     * 更新菜单状态并刷新 bootstrap 缓存版本，使动态菜单和路由权限重新计算。
     */
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
        return toMenuVo(loadMenuById(existed.getId()));
    }

    /**
     * 保存用户角色关系，并递增 bootstrap 缓存版本。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemRecordVo assignUserRoles(UserRoleAssignRequest request) {
        SystemRecordEntity user = loadWritableRecord(request.getUserId());
        if (!"users".equals(user.getModuleKey())) {
            // 前端传的是 recordCode，这里防止误把角色/字典记录当成用户做角色分配。
            throw new BusinessException(4050, "只能为用户分配角色");
        }
        assignRoles(user.getId(), request.getRoleCodes());
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("user roles assigned, userId={}, roleCodes={}, bootstrapCacheVersion={}", user.getId(), request.getRoleCodes(), version);
        return toVo(loadWritableRecord(user.getRecordCode()));
    }

    /**
     * 获取缓存版本；首次使用时初始化，避免 key 不存在导致缓存 key 不稳定。
     */
    private long currentVersion(String versionKey, long ttlSeconds) {
        Object cached = redisCacheService.get(versionKey);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        // 初始化版本号设置较长 TTL，避免短期缓存自然过期后版本号频繁回到 1。
        redisCacheService.set(versionKey, 1L, ttlSeconds * 24);
        return 1L;
    }

    /**
     * 递增缓存版本。旧版本缓存不立即删除，依赖 TTL 自然过期。
     */
    private long nextVersion(String versionKey, long ttlSeconds) {
        Long version = redisCacheService.increment(versionKey);
        if (version == null) {
            redisCacheService.set(versionKey, 1L, ttlSeconds * 24);
            return 1L;
        }
        return version.longValue();
    }

    /**
     * 字典缓存命中后在内存中过滤分页，减少小数据量字典列表对数据库的重复访问。
     */
    private List<SystemRecordVo> filterDictPage(List<SystemRecordVo> records, String keyword, String status) {
        List<SystemRecordVo> filtered = new ArrayList<SystemRecordVo>();
        for (SystemRecordVo record : records) {
            if (StringUtils.hasText(status) && !status.equals(record.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = keyword.trim().toLowerCase();
                // 旧版字典列表支持按名称、编码、负责人和描述模糊搜索，保持与数据库查询体验一致。
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
            // 外链菜单没有内部 routePath，必须依赖 externalLink 才能被前端正确打开。
            throw new BusinessException(4016, "外链菜单必须填写 externalLink");
        }
    }

    /**
     * 将菜单保存请求映射到菜单实体，保持 owner 等脚手架默认字段一致。
     */
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
                // /system/users 这类路径取最后一段 users 作为 routeCode，供前端路由权限匹配。
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
            // 用户表 password 字段借用 roleCodes 入参承载默认密码哈希，mapper 中只写入 password。
            systemMapper.insertUser(entity);
            return;
        }
        if ("roles".equals(entity.getModuleKey())) {
            // 角色记录落到 sys_role，用户数由列表查询实时聚合，不在写入时维护。
            systemMapper.insertRole(entity);
            return;
        }
        if ("dicts".equals(entity.getModuleKey())) {
            systemMapper.insertDict(entity);
            return;
        }
        throw new BusinessException(4009, "模块类型不支持保存");
    }

    /**
     * 根据 moduleKey 更新真实业务表，logs 模块不支持写操作。
     */
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

    /**
     * 写入所有系统模块共有字段。
     */
    private void applyCommonFields(SystemRecordEntity entity, SystemSaveRequest request) {
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setStatus(request.getStatus());
        entity.setOwner(request.getOwner());
        entity.setDescription(request.getDescription());
    }

    /**
     * 写入各 moduleKey 的差异字段。用户新增时会设置默认密码哈希。
     */
    private void applyModuleFields(SystemRecordEntity entity, SystemSaveRequest request) {
        if ("users".equals(request.getModuleKey())) {
            // 系统管理页新增用户时密码统一初始化，后续可通过重置密码流程修改。
            entity.setDepartmentId(resolveDepartmentId(request.getDepartmentId(), request.getOwner()));
            entity.setAvatarUrl(defaultText(request.getAvatarUrl()));
            entity.setRoleCodes(passwordEncoder.encode("SpringAdmin@2026"));
            return;
        }
        if ("roles".equals(request.getModuleKey())) {
            // extraValue 在角色模块中表示数据权限范围，例如 all、department、self。
            entity.setDataScope(defaultText(request.getExtraValue()));
            if (entity.getUserCount() == null) {
                entity.setUserCount(0L);
            }
            return;
        }
        if ("dicts".equals(request.getModuleKey())) {
            // extraValue 在旧版字典模块中表示 dictType，code 仍作为展示编码。
            entity.setDictType(defaultText(request.getExtraValue()));
            if (entity.getItemCount() == null) {
                entity.setItemCount(0L);
            }
            if (!StringUtils.hasText(entity.getCacheKey())) {
                // cacheKey 用于前端展示旧字典缓存标识，未传时按编码生成稳定值。
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
        // recordCode 用毫秒时间生成，满足后台管理展示和跨表统一查询，不作为强业务单号。
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

    /**
     * 将通用实体转换为前端 SystemRecord，合并用户、角色、字典、日志所需字段。
     */
    private SystemRecordVo toVo(SystemRecordEntity entity) {
        SystemRecordVo vo = new SystemRecordVo();
        vo.setId(entity.getRecordCode());
        vo.setModuleKey(entity.getModuleKey());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setStatus(entity.getStatus());
        vo.setOwner(entity.getOwner());
        vo.setDescription(entity.getDescription());
        vo.setAvatarUrl(entity.getAvatarUrl());
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
        vo.setUserAgent(entity.getUserAgent());
        vo.setBrowser(entity.getBrowser());
        vo.setBrowserVersion(entity.getBrowserVersion());
        vo.setOsName(entity.getOsName());
        vo.setOsVersion(entity.getOsVersion());
        vo.setDeviceType(entity.getDeviceType());
        vo.setDeviceBrand(entity.getDeviceBrand());
        vo.setRequestInfo(entity.getRequestInfo());
        vo.setResponseInfo(entity.getResponseInfo());
        vo.setDurationMs(entity.getDurationMs());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 将扁平菜单列表组装成前端可渲染的树；父节点缺失时提升为根节点。
     */
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
                // 父节点被过滤时子节点提升为根节点，避免管理页查询条件导致整棵分支消失。
                roots.add(menu);
                continue;
            }
            // 追加 children 时保留 mapper 查询顺序，前端渲染时不需要重新排序。
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
            // 兼容旧表单只传 owner 文本的情况，尝试用部门名称/负责人等关键字反查部门。
            return systemMapper.findDepartmentIdByKeyword(owner.trim());
        }
        return null;
    }

    /**
     * 从新字段 roleCodes 或兼容字段 extraValue 中解析用户角色，默认给 viewer。
     */
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

    /**
     * 重建用户与角色关系，支持按角色 code 或角色名称分配。
     */
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
                // 兼容前端传角色名称的老数据，优先 code，找不到再按 name 匹配。
                roleId = systemMapper.findRoleIdByName(roleCode.trim());
            }
            if (roleId == null) {
                throw new BusinessException(4043, "角色不存在：" + roleCode);
            }
            if (!roleIds.contains(roleId)) {
                // 角色关系去重后写入，避免唯一索引或重复菜单聚合出现异常。
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
        // 用户角色变化会影响 bootstrap 中的菜单/路由/按钮，必须刷新版本。
        long version = nextVersion(BOOTSTRAP_VERSION_KEY, DICT_CACHE_TTL_SECONDS);
        log.info("user role relation rebuilt, userId={}, roleSize={}, bootstrapCacheVersion={}",
                userId, roleIds.size(), version);
    }
}
