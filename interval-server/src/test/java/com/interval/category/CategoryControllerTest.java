package com.interval.category;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interval.auth.config.SecurityConfig;
import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.util.JwtUtil;
import com.interval.category.dto.DeleteCategoryResponseDto;
import com.interval.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

@WebMvcTest(com.interval.category.controller.CategoryController.class)
@Import(SecurityConfig.class)
@DisplayName("Category Controller 测试")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("删除分类时应返回智能删除结果")
    void should_return_smart_delete_result_when_deleting_category() throws Exception {
        Long userId = 1L;
        Long categoryId = 10L;
        when(categoryService.deleteCategory(userId, categoryId))
            .thenReturn(new DeleteCategoryResponseDto("ARCHIVED", 3L));

        String token = "test-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("alex");
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, "alex")));

        String responseBody = mockMvc.perform(delete("/api/categories/{categoryId}", categoryId)
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("Category deleted successfully"))
            .andExpect(jsonPath("$.data.action").value("ARCHIVED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.get("data").isNull()).isFalse();
        assertThat(response.get("data").get("affectedRecords").asLong()).isEqualTo(3L);
        verify(categoryService).deleteCategory(userId, categoryId);
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
