package com.ecommerce.user.config;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.Role;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL    = "admin@ecommerce.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = User.builder()
            .email(ADMIN_EMAIL)
            .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
            .role(Role.ADMIN)
            .build();

        userRepository.save(admin);
        log.info("==========================================================");
        log.info("  Default admin created");
        log.info("  Email   : {}", ADMIN_EMAIL);
        log.info("  Password: {}", ADMIN_PASSWORD);
        log.info("  Change this password before deploying to production!");
        log.info("==========================================================");
    }
}
