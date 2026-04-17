package com.salty.admin.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.salty.admin.auth.dto.LoginRequest;
import com.salty.admin.auth.dto.RegisterRequest;
import com.salty.admin.auth.entity.SysUser;
import com.salty.admin.auth.entity.SysUserRole;
import com.salty.admin.auth.mapper.SysUserMapper;
import com.salty.admin.auth.mapper.SysUserRoleMapper;
import com.salty.admin.auth.vo.LoginResponseVO;
import com.salty.admin.auth.vo.UserInfoVO;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import com.salty.admin.log.service.LoginLogService;
import com.salty.admin.permission.entity.SysRole;
import com.salty.admin.permission.mapper.SysRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final EmailCodeService emailCodeService;

    private final LoginAttemptService loginAttemptService;

    private final LoginLogService loginLogService;

    private final TokenService tokenService;

    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicyValidator passwordPolicyValidator;

    public AuthService(SysUserMapper userMapper,
                       SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper,
                       EmailCodeService emailCodeService,
                       LoginAttemptService loginAttemptService,
                       LoginLogService loginLogService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.emailCodeService = emailCodeService;
        this.loginAttemptService = loginAttemptService;
        this.loginLogService = loginLogService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterRequest request, String ip) {
        String email = emailCodeService.normalizeEmail(request.getEmail());
        if (!StringUtils.hasText(request.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }
        passwordPolicyValidator.validate(request.getPassword(), request.getConfirmPassword());
        ensureUnique(email, request.getUsername());
        emailCodeService.consumeRegisterCode(email, request.getCode());

        SysUser user = new SysUser();
        user.setEmail(email);
        user.setUsername(resolveUsername(request.getUsername(), email));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(StringUtils.hasText(request.getNickName()) ? request.getNickName().trim() : user.getUsername());
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        SysRole defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "USER")
                .eq(SysRole::getStatus, 1)
                .last("LIMIT 1"));
        if (defaultRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认角色 USER 未初始化");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(defaultRole.getId());
        userRoleMapper.insert(userRole);
        return tokenService.toUserInfo(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO login(LoginRequest request, String ip, String userAgent) {
        String account = request.getUsername() == null ? null : request.getUsername().trim();
        loginAttemptService.assertAllowed(account, ip);
        SysUser user = userMapper.selectByUsernameOrEmail(account);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.onFailure(account, ip);
            loginLogService.record(account, ip, userAgent, false, "用户名或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            loginLogService.record(account, ip, userAgent, false, "账号已禁用");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已禁用");
        }
        loginAttemptService.onSuccess(account, ip);
        user.setLastLoginIp(ip);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        LoginResponseVO response = tokenService.issue(user, request.getDeviceId(), request.getDeviceName());
        loginLogService.record(account, ip, userAgent, true, "登录成功");
        return response;
    }

    public UserInfoVO profile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return tokenService.toUserInfo(user);
    }

    private void ensureUnique(String email, String username) {
        Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (emailCount != null && emailCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "邮箱已注册");
        }
        if (StringUtils.hasText(username)) {
            Long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
            if (usernameCount != null && usernameCount > 0) {
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "用户名已存在");
            }
        }
    }

    private String resolveUsername(String username, String email) {
        String candidate = StringUtils.hasText(username) ? username.trim() : email.substring(0, email.indexOf('@'));
        Long usernameCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, candidate));
        if (usernameCount != null && usernameCount > 0) {
            return candidate + System.currentTimeMillis();
        }
        return candidate;
    }

}
