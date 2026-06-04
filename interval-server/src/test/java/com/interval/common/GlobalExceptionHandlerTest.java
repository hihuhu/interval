package com.interval.common;

import com.interval.common.dto.ApiResponse;
import com.interval.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("权限拒绝异常应返回 403")
    void should_return_forbidden_for_access_denied_exception() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<?>> response = handler.handleAccessDeniedException(
            new AccessDeniedException("Admin access required")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Admin access required");
    }
}
