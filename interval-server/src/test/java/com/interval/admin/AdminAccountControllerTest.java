package com.interval.admin;

import com.interval.admin.dto.AdminDashboardDto;
import com.interval.admin.service.AdminAccountService;
import com.interval.auth.config.SecurityConfig;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.interval.admin.controller.AdminAccountController.class)
@Import(SecurityConfig.class)
@DisplayName("管理员账号 Controller 测试")
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAccountService adminAccountService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("管理员应能访问 dashboard API")
    void should_get_dashboard_for_admin() throws Exception {
        String token = "admin-token";
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setAccountType(AccountType.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminAccountService.getDashboard(any()))
            .thenReturn(new AdminDashboardDto(3, 2, 1, 1, 2));

        mockMvc.perform(get("/api/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.totalUsers").value(3));
    }
}
