package com.interval.auth.service;

import com.interval.auth.entity.User;
import com.interval.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private static final long THROTTLE_SECONDS = 60L;

    private final UserRepository userRepository;
    private Map<Long, Instant> lastWrites = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void markActive(Long userId) {
        Instant now = Instant.now();
        Instant lastWrite = lastWrites.get(userId);
        if (lastWrite != null && lastWrite.plusSeconds(THROTTLE_SECONDS).isAfter(now)) {
            return;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLastActiveAt(now);
        userRepository.save(user);
        lastWrites.put(userId, now);
    }
}
