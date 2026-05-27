package com.interval.auth;

import com.interval.auth.entity.User;
import com.interval.auth.config.SeedDataInitializer;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.util.JwtUtil;
import com.interval.category.repository.CategoryRepository;
import com.interval.timeslot.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("JWT 安全集成测试")
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private SeedDataInitializer seedDataInitializer;

    private User user;

    @BeforeEach
    void setUp() {
        timeSlotRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setUsername("jwtuser");
        user.setPasswordHash("hashed-password");
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("受保护接口缺少 token 时应返回 401")
    void should_return_unauthorized_when_token_missing() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("受保护接口 token 非法时应返回 401")
    void should_return_unauthorized_when_token_invalid() throws Exception {
        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    @DisplayName("受保护接口 token 有效时应使用当前用户上下文")
    void should_use_authenticated_user_context_when_token_valid() throws Exception {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("token 有效但用户不存在时应返回 401")
    void should_return_unauthorized_when_authenticated_user_not_found() throws Exception {
        String token = jwtUtil.generateToken(9999L, "missing-user");

        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.message").value("Authenticated user not found"));
    }

    @Test
    @DisplayName("注册接口不需要 token")
    void should_allow_register_without_token() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newjwtuser\",\"password\":\"SecurePass123!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("启动后应提供 admin 种子账号")
    void should_login_with_admin_seed_account() throws Exception {
        seedDataInitializer.seedAdminAccount();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123ABCdef*\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
