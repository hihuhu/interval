package com.interval.auth;

import com.interval.auth.entity.User;
import com.interval.auth.repository.UserRepository;
import com.interval.auth.service.UserActivityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("用户活跃度服务测试")
class UserActivityServiceTest {

    @Test
    @DisplayName("60 秒内重复调用只保存一次，超过 60 秒会再次更新 lastActiveAt")
    void should_throttle_activity_updates_within_60_seconds() {
        UserRepository userRepository = mock(UserRepository.class);
        UserActivityServiceImpl service = new UserActivityServiceImpl(userRepository);
        User user = new User();
        user.setId(1L);
        user.setUsername("alex");
        user.setPasswordHash("hash");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.markActive(1L);
        service.markActive(1L);

        verify(userRepository, times(1)).save(argThat(saved -> saved.getLastActiveAt() != null));

        ReflectionTestUtils.setField(service, "lastWrites", new java.util.concurrent.ConcurrentHashMap<Long, Instant>());
        user.setLastActiveAt(Instant.now().minusSeconds(61));

        service.markActive(1L);

        verify(userRepository, times(2)).save(argThat(saved -> saved.getLastActiveAt() != null));
    }
}
