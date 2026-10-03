package com.todolist.config;

import com.todolist.entity.AppUser;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username}")
    private String defaultUsername;

    @Value("${app.admin.default-email}")
    private String defaultEmail;

    @Value("${app.admin.default-password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        if (appUserRepository.countByRole(Role.ADMIN) == 0) {
            AppUser admin = AppUser.builder()
                    .username(defaultUsername)
                    .email(defaultEmail)
                    .password(passwordEncoder.encode(defaultPassword))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            appUserRepository.save(admin);
            System.out.println("==> Đã tạo tài khoản Admin mặc định: username='" + defaultUsername
                    + "', password='" + defaultPassword + "' (hãy đổi password sau khi đăng nhập lần đầu)");
        }
    }
}
