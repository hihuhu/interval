package com.interval.auth;

import com.interval.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT 工具测试")
class JwtUtilTest {

    @Test
    @DisplayName("生成的 token 应包含用户名和用户 ID")
    void should_extract_username_and_user_id_from_generated_token() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        String token = jwtUtil.generateToken(42L, "alex");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("alex");
        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("非法 token 应验证失败")
    void should_reject_malformed_token() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        assertThat(jwtUtil.validateToken("not-a-jwt-token")).isFalse();
    }
}
