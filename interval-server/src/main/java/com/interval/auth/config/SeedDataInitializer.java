package com.interval.auth.config;

import com.interval.auth.entity.AccountType;
import com.interval.auth.entity.User;
import com.interval.auth.entity.UserStatus;
import com.interval.auth.repository.UserRepository;
import com.interval.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminAccount();
    }

    @Transactional
    public void seedAdminAccount() {
        userRepository.findByUsername(ADMIN_USERNAME)
            .orElseGet(() -> {
                User user = new User();
                user.setUsername(ADMIN_USERNAME);
                user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                user.setAccountType(AccountType.ADMIN);
                user.setStatus(UserStatus.ACTIVE);
                user.setMustChangePassword(true);
                return userRepository.save(user);
            });
    }
}
