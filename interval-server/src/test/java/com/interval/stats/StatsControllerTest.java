package com.interval.stats;

import com.interval.auth.config.SecurityConfig;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.util.JwtUtil;
import com.interval.stats.dto.CategoryDurationSummaryDto;
import com.interval.stats.dto.CategoryDurationStatDto;
import com.interval.stats.service.StatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.interval.stats.controller.StatsController.class)
@Import(SecurityConfig.class)
@DisplayName("Stats Controller 测试")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("查询分类耗时统计时应返回标准 ApiResponse")
    void should_return_category_duration_stats() throws Exception {
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);
        String token = "stats-token";
        CategoryDurationSummaryDto summary = new CategoryDurationSummaryDto(
            startDate,
            endDate,
            4L,
            60L,
            44640L,
            44580L,
            List.of(new CategoryDurationStatDto(10L, "工作", "#6366f1", "ACTIVE", 4L, 60L, 100.0))
        );

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("alex");
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "alex")));
        when(statsService.getCategoryDurations(userId, startDate, endDate)).thenReturn(summary);

        mockMvc.perform(get("/api/stats/category-durations")
                .header("Authorization", "Bearer " + token)
                .queryParam("startDate", "2026-05-01")
                .queryParam("endDate", "2026-05-31")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("Category duration statistics retrieved successfully"))
            .andExpect(jsonPath("$.data.totalRecordedMinutes").value(60))
            .andExpect(jsonPath("$.data.categories[0].categoryName").value("工作"));

        verify(statsService).getCategoryDurations(userId, startDate, endDate);
    }

    @Test
    @DisplayName("日期范围非法时应返回 400")
    void should_return_bad_request_for_invalid_range() throws Exception {
        Long userId = 1L;
        LocalDate startDate = LocalDate.of(2026, 5, 31);
        LocalDate endDate = LocalDate.of(2026, 5, 1);
        String token = "stats-token";

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("alex");
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "alex")));
        when(statsService.getCategoryDurations(userId, startDate, endDate))
            .thenThrow(new IllegalArgumentException("startDate must be before or equal to endDate"));

        mockMvc.perform(get("/api/stats/category-durations")
                .header("Authorization", "Bearer " + token)
                .queryParam("startDate", "2026-05-31")
                .queryParam("endDate", "2026-05-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.message").value("startDate must be before or equal to endDate"));
    }

    private User activeUser(Long userId, String username) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setPasswordHash("hashed-password");
        user.setAccountType(AccountType.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
