package com.interval.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.security.JwtAuthenticationFilter;
import com.interval.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT 账号状态测试")
class JwtAccountStatusTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("有效旧 token 对应禁用账号时应拒绝访问")
    void should_forbid_disabled_account_with_existing_token() throws Exception {
        String token = "valid-token";
        User disabledUser = new User();
        disabledUser.setId(5L);
        disabledUser.setUsername("disabled-user");
        disabledUser.setAccountType(AccountType.USER);
        disabledUser.setStatus(UserStatus.DISABLED);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(5L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("disabled-user");
        when(userRepository.findById(5L)).thenReturn(Optional.of(disabledUser));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/categories");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(userRepository).findById(5L);
        verify(userRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("管理员账号访问普通业务接口时应拒绝")
    void should_forbid_admin_from_user_routes() throws Exception {
        String token = "admin-token";
        User admin = activeUser(1L, "admin", AccountType.ADMIN);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/categories");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("普通用户访问管理员接口时应拒绝")
    void should_forbid_user_from_admin_routes() throws Exception {
        String token = "user-token";
        User user = activeUser(2L, "alex", AccountType.USER);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(2L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("alex");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("强制改密的普通用户访问业务接口时应拒绝")
    void should_forbid_must_change_password_user_from_business_routes() throws Exception {
        String token = "must-change-user-token";
        User user = activeUser(3L, "reset-user", AccountType.USER);
        user.setMustChangePassword(true);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(3L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("reset-user");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/categories");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("强制改密的管理员访问管理接口时应拒绝")
    void should_forbid_must_change_password_admin_from_admin_routes() throws Exception {
        String token = "must-change-admin-token";
        User admin = activeUser(4L, "admin", AccountType.ADMIN);
        admin.setMustChangePassword(true);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(4L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("admin");
        when(userRepository.findById(4L)).thenReturn(Optional.of(admin));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("强制改密账号应允许访问改密接口")
    void should_allow_must_change_password_account_to_change_password_route() throws Exception {
        String token = "change-password-token";
        User user = activeUser(6L, "reset-user", AccountType.USER);
        user.setMustChangePassword(true);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(6L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("reset-user");
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtUtil,
            userRepository,
            new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/change-password");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    private User activeUser(Long userId, String username, AccountType accountType) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setAccountType(accountType);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
