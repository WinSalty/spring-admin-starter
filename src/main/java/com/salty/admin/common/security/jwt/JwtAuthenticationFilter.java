package com.salty.admin.common.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salty.admin.auth.service.TokenService;
import com.salty.admin.common.api.ApiResponse;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.security.LoginUser;
import com.salty.admin.common.security.LoginUserContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/email-code/send".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/health".equals(path)
                || "/actuator/health".equals(path)
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = tokenService.resolveBearer(request.getHeader("Authorization"));
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            LoginUser loginUser = tokenService.authenticateAccessToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser, null, authorities(loginUser));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            LoginUserContext.set(loginUser);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            LoginUserContext.clear();
            writeUnauthorized(response);
        } finally {
            LoginUserContext.clear();
        }
    }

    private List<SimpleGrantedAuthority> authorities(LoginUser loginUser) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        for (String role : loginUser.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : loginUser.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED)));
    }
}
