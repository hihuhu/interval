package com.interval.auth.config;

import com.interval.auth.entity.User;
import com.interval.auth.repository.UserRepository;
import com.interval.category.entity.Category;
import com.interval.category.entity.CategoryStatus;
import com.interval.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "123ABCdef*";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminAccount();
    }

    @Transactional
    public void seedAdminAccount() {
        User admin = userRepository.findByUsername(ADMIN_USERNAME)
            .map(existingUser -> {
                if (!passwordEncoder.matches(ADMIN_PASSWORD, existingUser.getPasswordHash())) {
                    existingUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                    return userRepository.save(existingUser);
                }
                return existingUser;
            })
            .orElseGet(() -> {
                User user = new User();
                user.setUsername(ADMIN_USERNAME);
                user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                return userRepository.save(user);
            });

        if (categoryRepository.findActiveByUserId(admin.getId()).isEmpty()) {
            seedDefaultCategories(admin.getId());
        }
    }

    private void seedDefaultCategories(Long userId) {
        Instant now = Instant.now();
        List<SeedCategory> defaults = List.of(
            new SeedCategory("工作", "#a5b4fc"),
            new SeedCategory("睡眠", "#c4b5fd"),
            new SeedCategory("运动", "#86efac"),
            new SeedCategory("阅读", "#fde68a"),
            new SeedCategory("社交", "#fdba74"),
            new SeedCategory("通勤", "#93c5fd")
        );

        for (int index = 0; index < defaults.size(); index += 1) {
            SeedCategory seed = defaults.get(index);
            Category category = new Category();
            category.setUserId(userId);
            category.setName(seed.name());
            category.setColorCode(seed.colorCode());
            category.setStatus(CategoryStatus.ACTIVE);
            category.setDisplayOrder(index);
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            categoryRepository.save(category);
        }
    }

    private record SeedCategory(String name, String colorCode) {
    }
}
